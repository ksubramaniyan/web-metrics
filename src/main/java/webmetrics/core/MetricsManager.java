/**
 * Manager that deals with all logic related to configuring and matching URI, creating metrics objects and other metric related tasks.<br>
 * Currently this version deals only with URI metrics, but additional logic will be added to work with other measurable actions like invocation of services, DB
 * access, etc.<br>
 * Usage:<br>
 * Typically instance of this manager will be automatically placed as attribute to Servlet Context under the key "WebMetricManager" which is short class
 * name.<br>
 * Configuring application involves creating custom web metric configuration file (use schema webmetrics.xsd), specifying the name of that file as context
 * param with name webMetricConfig in the web.xml, for example:<br>
 * 
 * <pre>
 * <context-param>
 * 		<param-name>webMetricConfig</param-name>
 * 		<param-value>customwebmetricconfig.xml</param-value>
 * </context-param>
 * </pre>
 * 
 * and finally configuring filter WebMetricsFilter and WebMetricsServletContextListener (standard web.xml configuration). No additional properties for
 * filter and listener are needed (except for standard filter mapping).<br>
 */
package webmetrics.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import webmetrics.config.MetricConfig;

public class MetricsManager {

	private static MetricsManager manager;

	/**
	 * Creates new instance of web metric manager with custom web metric
	 * configuration.<br>
	 * Only one instance is allowed per application (in current version of
	 * component).<br>
	 * Name of custom file may not be "webmetrics-default.xml" located at the
	 * root of class path as this is the name and location of default
	 * configuration.<br>
	 * Name must include full relative path assuming root is classpath. If file
	 * is located at classpath no slash is necessary.<br>
	 * Hint: typical location of the file inside POM module is
	 * src/main/resources which will resolve to the root of classpath.<br>
	 * 
	 * @param customWebMetricConfig
	 *            - optional, if null is provided, only default configuration
	 *            will be used
	 */
	private MetricsManager(String customWebMetricConfig) {
		synchronized (MetricsManager.class) {
			if (instanceCreated)
				throw new IllegalStateException("Another instance of WebMetricManager already exists");
			instanceCreated = true;
		}
		webMetricConfig = new MetricConfig(customWebMetricConfig);
		if (!webMetricConfig.isEnabled()) {
			logger.info("Metrics component is not enabled");
			return;
		}
		switch (webMetricConfig.getUriPatternVersion()) {
		case 1:
			whitelistManager = new UriWhitelistManagerImpl1();
			break;
		case 2:
			whitelistManager = new UriWhitelistManagerImpl2();
			break;
		default:
			throw new IllegalArgumentException(
					"Allowed pattern versions are only 1 or 2, but got: " + webMetricConfig.getUriPatternVersion());
		}
		whitelistManager.setExceptionOnInvalidPattern(true);
		metricsRegistry = new MetricRegistry();
		jmxReporter = JmxReporter.forRegistry(metricsRegistry).inDomain(webMetricConfig.getDomain()).build();
		buildWhitelistedUri();
		// validateHistogramBuckets();
	}

	public static synchronized MetricsManager instance(String customWebMetricConfig) {

		if (manager == null) {
			manager = new MetricsManager(customWebMetricConfig);
		}
		return manager;
	}

	public static synchronized MetricsManager getInstance() {
		return manager;
	}

	public boolean isEnabled() {
		return webMetricConfig != null && webMetricConfig.isEnabled();
	}

	/**
	 * Refactoring without the ThreadLocal
	 */

	public UriTimer startTimerForUri(String uri, String consumer) {
		if (!isEnabled()) {
			return null;
		}
		// uriTimer.remove();
		UriTimer uriTimerMetric = getTimerForURI(uri, consumer);
		if (uriTimerMetric != null) {
			uriTimerMetric.start();
		}
		return uriTimerMetric;
	}

	public void stopTimer(int responseStatus, UriTimer timer) {
		if (!isEnabled()) {
			return;
		}
		if (timer != null) {
			long duration = timer.stop();
			markMeterForStatusCode(timer.getMetricName(), responseStatus);
			addToBucket(timer.getMetricName(), duration);
		}
	}

	// -------------------------------------------

	private static final String METRIC_NAME_PREFIX = "metrics";
	private static final String HISTOGRAM_NAME_PREFIX = "histogram";
	private static final String STATUS_NAME_PREFIX = "status";
	private static final String METRIC_CONSUMERS_PREFIX = "consumers";

	private static boolean instanceCreated;
	private MetricConfig webMetricConfig;
	private MetricRegistry metricsRegistry;
	private JmxReporter jmxReporter;
	private WhitelistURIManager whitelistManager;
	private ConcurrentMap<String, Meter> metersByStatusCode = new ConcurrentHashMap<String, Meter>();
	// internally holds actual custom defined buckets
	private Map<String, HistogramBuckets> uriBuckets;

	private static final int OK = 200;
	private static final int BAD_REQUEST = 400;
	private static final int SERVER_ERROR = 500;

	// allow in the future to add custom status codes
	private final static Map<Integer, String> defaultMeterNamesByStatusCode = new HashMap<Integer, String>(6);
	static {
		defaultMeterNamesByStatusCode.put(OK, "ok");
		defaultMeterNamesByStatusCode.put(BAD_REQUEST, "badRequest");
		defaultMeterNamesByStatusCode.put(SERVER_ERROR, "serverError");
	}

	/**
	 * A helper method that takes responsibility to re-build all URI structures
	 * whenever is necessary.<br>
	 * This method can be called at any time.<br>
	 * All previously built URI structures are cleared out.<br>
	 */
	private synchronized void buildWhitelistedUri() {
		// the goal is first to split all URIs in two groups - without wildcards
		// or regex and with either wildcard or regex
		Map<String, String> uris = webMetricConfig.getWhiteListedUris();
		if (uris == null || uris.isEmpty()) {
			logger.warn("buildWhitelistedUri() returns empty collection");
		}
		whitelistManager.initialize(uris);
	}

	/**
	 * Returns base for metric name.<br>
	 * This is the same as "display" name as configured for given URI.<br>
	 * If at least one URI is white listed, then the logic is defined as:<br>
	 * If current URI us not white listed, then base will be either null (in
	 * which case no need to report) or will come from configured as
	 * "nonWhiteListName" (for example: "other").<br>
	 * If current URI is white listed, the base is derived from that
	 * configuration.<br>
	 * If no URIs are white listed, the whole site comes under same metric and
	 * base name is "site".<br>
	 * 
	 * @param uri
	 * @return
	 */
	private String getBaseMetricDisplayName(String uri) {
		if (webMetricConfig.getWhiteListedUris() == null || webMetricConfig.getWhiteListedUris().isEmpty()) {
			// no white listed URIs provided, base as "site"
			return "site";
		}
		String metricName = whitelistManager.getDisplayMetricName(uri);
		if (metricName == null) {
			// non white listed URI
			if (!webMetricConfig.isEnableNonWhiteListedUri())
				return null;
			// return as configured, can be null
			return webMetricConfig.getNonWhiteListName();
		}
		return metricName;
	}

	/**
	 * Returns timer or null if no need to report the metric.<br>
	 * 
	 * @param uri
	 * @param consumer
	 * @return
	 */
	private UriTimer getTimerForURI(String uri, String consumer) {
		String metricDisplayName = getBaseMetricDisplayName(uri);
		if (metricDisplayName == null)
			return null;
		Timer timer = metricsRegistry.timer(metricDisplayName + "." + METRIC_NAME_PREFIX);
		Timer consumerTimer = null;
		if (consumer != null) {
			String consumerAlias = null;
			if (webMetricConfig.getConsumers() != null) {
				consumerAlias = webMetricConfig.getConsumers().get(consumer);
			}
			if (consumerAlias != null) {
				consumerTimer = metricsRegistry
						.timer(metricDisplayName + "." + METRIC_CONSUMERS_PREFIX + "." + consumerAlias);
			}
		}
		UriTimer uriTimer = new UriTimer(timer, consumerTimer, uri, metricDisplayName);
		return uriTimer;
	}

	/**
	 * Adds duration to proper bucket.<br>
	 * If URI is not white listed or if buckets are not defined, simply returns
	 * without generating any metric.<br>
	 * Buckets for actual URIs are generated once at run time when actual URI
	 * comes in.<br>
	 * 
	 * @param baseMetricName
	 * @param durationNano
	 */
	private void addToBucket(String baseMetricName, long durationNano) {
		try { // a temporary try-catch block to test changes for URI specific
				// histogram buckets; need to remove when tested
			if (/* !useBuckets || */!webMetricConfig.isEnableHistogram() || baseMetricName == null)
				return; // nothing to do
			// need proper synchronization to avoid conflicts and race
			// conditions
			if (uriBuckets == null) {
				synchronized (this) {
					if (uriBuckets == null)
						uriBuckets = new HashMap<String, HistogramBuckets>();
				}
			}
			HistogramBuckets currentUriBuckets = uriBuckets.get(baseMetricName);
			Map<String, Integer[]> uriResponseBuckets = webMetricConfig.getWhitelistedResponseBuckets();
			Integer[] uriResponseBucket = null;
			if (uriResponseBuckets != null) {
				uriResponseBucket = uriResponseBuckets.get(baseMetricName);
			}
			if (uriResponseBucket == null || uriResponseBucket.length == 0) {
				uriResponseBucket = webMetricConfig.getResponseBuckets();
			}
			if (uriResponseBucket == null || uriResponseBucket.length == 0) {
				return; // no buckets defined: either global or per specific set
						// of URI/DisplayName
			}
			if (currentUriBuckets == null) {
				synchronized (this) {
					currentUriBuckets = uriBuckets.get(baseMetricName);
					if (currentUriBuckets == null) {
						// generate uri buckets object for current URI
						currentUriBuckets = new HistogramBuckets();
						uriBuckets.put(baseMetricName, currentUriBuckets);
						// total count of all buckets for a URI is count of
						// boundaries + 1
						currentUriBuckets.setUriBucket(new HistogramBucket[uriResponseBucket.length + 1]);
						for (int idx = 0; idx <= uriResponseBucket.length; idx++) {
							String fullBucketName = baseMetricName + "." + HISTOGRAM_NAME_PREFIX + ".";
							fullBucketName += idx == 0 ? "0-"
									: uriResponseBucket[idx - 1] + (idx < uriResponseBucket.length ? "-" : "");
							fullBucketName += idx < uriResponseBucket.length ? uriResponseBucket[idx] : "-UP";
							HistogramBucket uriBucket = new HistogramBucket();
							uriBucket.setMetricName(fullBucketName);
							uriBucket.setLow(idx == 0 ? 0 : uriResponseBucket[idx - 1]);
							uriBucket.setHigh(
									idx < uriResponseBucket.length ? uriResponseBucket[idx] : Integer.MAX_VALUE);
							logger.debug("Bucket Metrics: idx: " + idx + ", fullBucketName: " + fullBucketName
									+ ", low/high: " + uriBucket.getLow() + "/" + uriBucket.getHigh());
							currentUriBuckets.getUriBucket()[idx] = uriBucket;
							// we also need to generate all counters even for
							// empty buckets
							metricsRegistry.counter(fullBucketName);
						}
					}
				}
			}
			int durationMilli = (int) (durationNano / 1000 / 1000); // cast to
																	// milliseconds
																	// from
																	// nanoseconds
			// find bucket that fits conditions and increment
			for (HistogramBucket uriBucket : currentUriBuckets.getUriBucket()) {
				if (uriBucket.getLow() <= durationMilli && uriBucket.getHigh() > durationMilli) {
					logger.debug("Bucket Metrics durationMilli: " + durationMilli + ", low/high: " + uriBucket.getLow()
							+ "/" + uriBucket.getHigh());
					metricsRegistry.counter(uriBucket.getMetricName()).inc();
					break;
				}
			}
		} catch (Exception e) {
			// temporary catch; need to remove
			logger.error("An exception occurred with new changes for URI specific histogram bucket, "
					+ e.getClass().getName() + ":" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void markMeterForStatusCode(String baseMetricName, int status) {
		if (baseMetricName == null)
			return;
		if (!webMetricConfig.isEnableStatus())
			return;

		int statusBucket = (status >= 200 && status < 300) ? 200
				: ((status >= 400 && status < 300) ? 400 : (status >= 500 && status < 600) ? 500 : -1);
		String statusName = defaultMeterNamesByStatusCode.get(statusBucket);
		if (statusName == null) {
			return;
		}
		String metricName = baseMetricName + "." + STATUS_NAME_PREFIX + "." + statusName;
		Meter meter = metersByStatusCode.get(metricName);
		if (meter == null) {
			synchronized (this) {
				meter = metersByStatusCode.get(metricName);
				if (meter == null)
					createStatusCodeMetersForUri(baseMetricName);
			}
			meter = metersByStatusCode.get(metricName);
		}
		meter.mark();
	}

	/**
	 * 
	 * @param metricName
	 *            - base metric name
	 */
	private void createStatusCodeMetersForUri(String metricName) {
		for (Entry<Integer, String> entry : defaultMeterNamesByStatusCode.entrySet()) {
			String key = metricName + "." + STATUS_NAME_PREFIX + "." + entry.getValue();
			metersByStatusCode.put(key, metricsRegistry.meter(key));
		}
	}

	public MetricRegistry getMetricsRegistry() {
		return metricsRegistry;
	}

	public JmxReporter getJmxReporter() {
		return jmxReporter;
	}

	public MetricConfig getWebMetricConfig() {
		return webMetricConfig;
	}

	private static Logger logger = LoggerFactory.getLogger(MetricsManager.class);
}
