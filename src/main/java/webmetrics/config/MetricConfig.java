package webmetrics.config;

import java.util.Map;
import java.util.Set;

public class MetricConfig {

	public static final String WEB_METRIC_CONFIG_KEY = "webMetricConfig";

	private String customConfig;

	private ConfigReader defaultConfigReader;
	private ConfigReader customConfigReader;

	static final String CONFIGURATION_SCHEMA = "webmetrics.xsd";
	static final String DEFAULT_CONFIGURATION = "webmetrics-default.xml";

	/**
	 * 
	 * @param customConfig
	 *            - custom XML configuration file name, must exist in root of class path or be full path from root of class path. Optional, if null is provided,
	 *            no custom config is used
	 */
	public MetricConfig(String customConfig) {
		if (DEFAULT_CONFIGURATION.equals(customConfig))
			throw new IllegalArgumentException("Custom configuration name is the same as default configuration: " + customConfig);
		this.customConfig = customConfig;
		buildConfiguration();
	}

	private void buildConfiguration() {
		defaultConfigReader = new ConfigReader(CONFIGURATION_SCHEMA, DEFAULT_CONFIGURATION);
		customConfigReader = new ConfigReader(CONFIGURATION_SCHEMA, customConfig);
	}

	public Map<String, String> getWhiteListedUris() {
		// only custom uris are processed, not default
		return customConfigReader.getWhitelistedUris();
	}

	public String getNonWhiteListName() {
		String name = customConfigReader.getNonWhiteListName();
		if (name != null) {
			// custom value specified, do not use default if custom is blank
			name = name.trim();
			// return null if blank
			if (name.equals(""))
				return null;
			return name;
		}
		name = defaultConfigReader.getNonWhiteListName();
		if (name != null)
			name = name.trim();
		return name != null && !name.equals("") ? name : null;
	}

	public Integer[] getResponseBuckets() {
		Integer[] buckets = customConfigReader.getResponseBuckets();
		if (buckets != null)
			return buckets;
		return defaultConfigReader.getResponseBuckets();
	}

	public Map<String, Integer[]> getWhitelistedResponseBuckets() {
		// only custom uris are processed, not default
		return customConfigReader.getWhitelistedResponseBuckets();
	}

	public String getDomain() {
		String domain = customConfigReader.getDomain();
		if (domain != null)
			return domain;
		return defaultConfigReader.getDomain();
	}

	public Boolean isSkipContextName() {
		Boolean skip = customConfigReader.isSkipContextName();
		if (skip != null)
			return skip;
		return defaultConfigReader.isSkipContextName() != null ? defaultConfigReader.isSkipContextName() : true;
	}

	public Integer getUriPatternVersion() {
		Integer patternVersion = customConfigReader.getUriPatternVersion();
		if (patternVersion != null)
			return patternVersion;
		return defaultConfigReader.getUriPatternVersion();
	}

	public Boolean isEnableNonWhiteListedUri() {
		Boolean enable = customConfigReader.isEnableNonWhiteListedUri();
		if (enable != null)
			return enable;
		return defaultConfigReader.isEnableNonWhiteListedUri() != null ? defaultConfigReader.isEnableNonWhiteListedUri() : true;
	}

	public Boolean isEnableHistogram() {
		Boolean enable = customConfigReader.isEnableHistogram();
		if (enable != null)
			return enable;
		return defaultConfigReader.isEnableHistogram() != null ? defaultConfigReader.isEnableHistogram() : true;
	}

	public Boolean isEnableStatus() {
		Boolean enable = customConfigReader.isEnableStatus();
		if (enable != null)
			return enable;
		return defaultConfigReader.isEnableStatus() != null ? defaultConfigReader.isEnableStatus() : true;
	}

	public String getIdField() {
		// only custom consumer are processed, not default
		return customConfigReader.getIdField();
	}

	public String getIdFieldscope() {
		// only custom consumer are processed, not default
		return customConfigReader.getIdFieldscope();
	}

	public Map<String, String> getConsumers() {
		// only custom consumer are processed, not default
		return customConfigReader.getConsumers();
	}

	public Set<String> getDisabledConsumers() {
		// only custom consumer are processed, not default
		return customConfigReader.getDisabledConsumers();
	}

	public boolean isEnabled() {
		Boolean enabled = customConfigReader.isEnabled();
		if (enabled != null)
			return enabled;
		return defaultConfigReader.isEnabled() != null ? defaultConfigReader.isEnabled() : true;
	}

}
