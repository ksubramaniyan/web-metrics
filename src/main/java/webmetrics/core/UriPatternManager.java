/**
 * This class helps to generate regex pattern for URI pattern described in UriManager and then test actual uri for match.<br>
 */
package webmetrics.core;

public interface UriPatternManager {

	/**
	 * Parses URI pattern.<br>
	 * In case of error result will contain "success==false" or exception is thrown based on exceptionOnInvalidPattern.<br>
	 * 
	 * @param uriPattern
	 *            - URI or URI pattern, required
	 * @return
	 */
	public UriPatternInfo parseUriPattern(String uriPattern);

	/**
	 * Tests given actual URI for match to provided URI Pattern.<br>
	 * Returns null if not match.<br>
	 * Returns portion of actual URI matching the first group of URI pattern (i.e. before uri terminator if one is used).<br>
	 * The returned portion of URI can be used to generate metric.<br>
	 * 
	 * @param uri
	 * @param uriRegex
	 * @return
	 */
	public String matchUri(String uri, UriPatternInfo uriPatternInfo);

	public boolean isExceptionOnInvalidPattern();

	public void setExceptionOnInvalidPattern(boolean exceptionOnInvalidPattern);

}
