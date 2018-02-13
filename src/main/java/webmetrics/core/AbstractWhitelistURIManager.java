/**
 * This class helps build white-listed collection of URIs and URI Patterns.<br>
 */
package webmetrics.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webmetrics.util.StringUtils;

public abstract class AbstractWhitelistURIManager implements WhitelistURIManager {

	// this map contains only registered full path URIs (no patterns)
	// key is URI, value is its display name
	protected Map<String, String> uriWhiteList;

	// this map contains only actual URIs that were already matched using patterns (performance goal),
	// key is the full uri, value is pattern,
	// this map is being filled in as request start flow, it allows to save on time spent to match
	protected Map<String, String> uriWhiteListMatched;

	// this ordered Set contains parsed URI Patterns
	protected Set<UriPatternInfo> uriWhiteListPatterns;

	// singleton as it does not use any internal fields
	protected UriPatternManager patternManager = getUriPatternManager();

	protected StringUtils textUtil = StringUtils.getInstance();

	protected abstract UriPatternManager getUriPatternManager();

	protected boolean exceptionOnInvalidPattern = true;

	@Override
	public void initialize(Map<String, String> uris) {
		uriWhiteList = null;
		uriWhiteListMatched = null;
		uriWhiteListPatterns = null;
		if (uris == null || uris.isEmpty()) {
			logger.info("No valid white-listed URIs are found");
			return;
		}
		// split into full path URIs and Patterns
		patternManager.setExceptionOnInvalidPattern(exceptionOnInvalidPattern);
		for (String uri : uris.keySet()) {
			String displayName = uris.get(uri);
			UriPatternInfo patternInfo = patternManager.parseUriPattern(uri);
			if (!patternInfo.isSuccess()) {
				logger.error("Error was found while trying to parse white-lisetd URI or URI Pattern: " + uri);
				continue;
			}
			if (patternInfo.getPattern() == null) {
				// this is full path URI, we do not need to keep that info
				if (uriWhiteList == null)
					uriWhiteList = new HashMap<String, String>();
				uri = textUtil.stripTrailingSlash(uri);
				uriWhiteList.put(uri, displayName);
			} else {
				// this is valid URI Pattern
				patternInfo.setMetricName(displayName);
				if (uriWhiteListPatterns == null)
					uriWhiteListPatterns = new LinkedHashSet<UriPatternInfo>();
				uriWhiteListPatterns.add(patternInfo);
			}
		}
	}

	@Override
	public String getDisplayMetricName(String uri) {
		if (uri == null) {
			logger.error("Passed uri is null");
			return null;
		}
		if (uriWhiteList == null && uriWhiteListPatterns == null)
			return null; // nothing to match
		uri = textUtil.stripTrailingSlash(uri);
		if (uriWhiteList != null && uriWhiteList.keySet().contains(uri))
			return uriWhiteList.get(uri);
		if (uriWhiteListMatched != null && uriWhiteListMatched.keySet().contains(uri))
			return uriWhiteListMatched.get(uri);
		if (uriWhiteListPatterns != null)
			for (UriPatternInfo uriPatternInfo : uriWhiteListPatterns) {
				String matchedUri = patternManager.matchUri(uri, uriPatternInfo);
				if (matchedUri == null)
					continue; // not match
				if (uriWhiteListMatched == null) {
					uriWhiteListMatched = new HashMap<String, String>();
				}
				uriWhiteListMatched.put(uri, uriPatternInfo.getMetricName());
				return uriPatternInfo.getMetricName();
			}
		// no match found
		return null;
	}

	public void setExceptionOnInvalidPattern(boolean exceptionOnInvalidPattern) {
		this.exceptionOnInvalidPattern = exceptionOnInvalidPattern;
	}

	protected Logger logger = LoggerFactory.getLogger(getClass().getName());
}
