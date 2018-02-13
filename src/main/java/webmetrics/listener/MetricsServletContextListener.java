/**
 * Please see doc for WebMetricManager for details.<br>
 */
package webmetrics.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webmetrics.config.MetricConfig;
import webmetrics.core.MetricsManager;

public class MetricsServletContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		logger.info("Servlet context initializing: " + MetricsManager.class.getSimpleName());
		MetricsManager webMetricManager = null;
		synchronized (event.getServletContext()) {
			webMetricManager = MetricsManager.class.cast(event.getServletContext().getAttribute(MetricsManager.class.getSimpleName()));
			if (webMetricManager != null) {
				logger.info("WebMetricManager already initialized");
			} else {
				logger.info("Will attempt to initialize WebMetricManager");
				String webMetricConfigName = event.getServletContext().getInitParameter(MetricConfig.WEB_METRIC_CONFIG_KEY);
				if (webMetricConfigName != null) {
					logger.info("Found custom web metric config name: " + webMetricConfigName);
				} else {
					logger.info("Did not find custom web metric config name");
				}
				//WebMetricManager = new WebMetricManager(webMetricConfigName);
				webMetricManager = MetricsManager.instance(webMetricConfigName);
				logger.info("Created instance of web metric manager: " + webMetricManager);
				event.getServletContext().setAttribute(MetricsManager.class.getSimpleName(), webMetricManager);
			}
		}
		if (webMetricManager.isEnabled()) {
			webMetricManager.getJmxReporter().start();
		} else {
			logger.warn("Web Metrics is not enabled");
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger.info("Servlet context destroying");
		MetricsManager webMetricManager = MetricsManager.class.cast(event.getServletContext().getAttribute(
						MetricsManager.class.getSimpleName()));
		if (webMetricManager.isEnabled()) {
			webMetricManager.getJmxReporter().stop();
		}
	}

	private static Logger logger = LoggerFactory.getLogger(MetricsServletContextListener.class);
}
