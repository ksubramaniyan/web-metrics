/**
 * This class helps to build collection of white-listed URI patterns.<br>
 * It also allows single access check whether a specific URI is white-listed (either full URI or matched Pattern) and to return metric name.<br>
 */
package webmetrics.core;

import java.util.Map;

public interface WhitelistURIManager {

	/**
	 * Clears any previous data and initializes manager with new set of uris/patterns.<br>
	 * 
	 * @param uris
	 *            - presumably ordered collection of URIs (LinkedHashMap or other suitable type)
	 */
	public void initialize(Map<String, String> uris);

	/**
	 * Tests actual passed URI for possible match and returns display metric name if matched.<br>
	 * If match is not found - passed URI us not white-listed, returns null.<br>
	 * 
	 * @param uri
	 * @return
	 */
	public String getDisplayMetricName(String uri);

	/**
	 * Initializes manager: throw exception or simply log error if match pattern is invalid.<br>
	 * By default the flag should be set as "true".<br>
	 * 
	 * @param exceptionOnInvalidPattern
	 */
	public void setExceptionOnInvalidPattern(boolean exceptionOnInvalidPattern);

}
