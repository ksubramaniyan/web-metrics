/**
 * This class helps to generate regex pattern for URI pattern described in UriManager and then test actual uri for match.<br>
 */
//  @formatter:off
// *** Following documentation is using single line Java comments due to use of character combination */ which would terminate regular @doc style comment ***
// 
// A URI in J2EE terms is a portion of full URL that follows immediately after domain name and optional port number, starts with forward slash "/" and ends with either
// start of URL Query String (question mark character "?"), hash character "#" or by the end of full URL if no query string or hash present.
// For the purpose of this component we will also limit URI by not using any matrix parameters and some other possible structures.
// More presicely we will work only with URI that contain only following set of valid charaters: / _ - . a-z A-Z 0-9 (space not included).
// Forward slash "/" is used to split a URI into Path Elements. A Path Element cannot be empty (or in other words two adjucent forward slash are illegal).
// A URI must start with slash and can optionally end with slash, however trailing slash will not be considered in any operations (it will be cut off).
// Generally speaking a J2EE application, unless deployment is default, must always start with Context: Path Element immediately after leading slash.
// However processing logic of this component does not require Context to be explicitly specified (i.e. "/" would be a perfectly valid URI,
// likewise "/**" would be perfectly valid URI pattern).
// This component accepts both types of URIs: full path URI and URI Patterns.
// Full path is where the entire full length of URI is explicitly spelled by using only the characters above and will be matched exactly AS IS (less trailing slash).
// URI pattern is a way to define a group of URIs that have common parts but have some specifics.
// URI Patterns are made by using additional wild card characters: * (star) and ? (question mark).
// When matching actual URI for a given HTTP Request general rule is that full path URI will always be matched first.
// Only if no full path URI are matched, pattern based URI matching will be used. All USIs, full path or pattern based, are cAse-sEnsitiVe !!!
// Since this component accepts a set of URIs to whitelist, few words need to be said about ordering of Full Path URIs and URI Patterns.
// Since full path URI is unique (and will be tried before matterns), ordering of full path URIs is not important.
// However more than one URI Patters can potentially match actual URI from request, therefore the rule is that the URI Patterns should be registered by ordered collection
// (ArrayList will do, LinkedHashSet will also maintain uniqueness) and the first matched pattern will be applied.
// Due to this rule it is suggested that logner URI patterns (i.e. patterns with more characters and patterns that will me matched less frequently or less likely)
// should be first in the list while shorter patterns (ones that will be matched more likely) should be at the end of the list.
// 
// Rules to create valid URI Patterns (remember - trailing forward slash will be removed from all: Full path URIs, URI Patterns and actual URIs tested for match):
// 
// 1: A single wild card * used to define placement of any number (0 or more) valid characters for a SINGLE Path Element of a URI:
// /appctx/my*Car*
// This pattern will match at least following actual URIs:
// /appctx/myCar/
// /appctx/myLovelyCar
// /appctx/myCarGarage/
// /appctx/myLovelyBlueCarColor
// Any number of path elements in a pattern can use any number of single wild cards *:
// /appctx/my*Car*/garage*/ - is a valid pattern
// 
// 2: A double wild card as ** used to define the remaining number of any path elements. It can only be used at the end of URI pattern and has to immediately follow
// the last forward slash in a pattern:
// /appctx/my*Car*/**
// Matching URIs:
// /appctx/myCar/garage
// /appctx/myCar
// 
// Following patterns would NOT be valid:
// /appctx/my*Car*/**/something
// /appctx/my*Car*/something**
// 
// Note: the pattern /** is valid and will match ANY URI.
// 
// For cases 1 and 2 above the metric name will be generated by using full actual URI.
// 
// 3: A question mark ? is used to limit matching portion of actual URI in a pattern. This is very similar to using the double wild card ** however the difference is
// that in case of ? the metric name will be generated from a URI cut off at the placement of ?.
// For this reason we call this as URI Terminator Wild Card. URI Terminator has to immediately follow last forward slash and has to be the last character in a pattern.
// Example: 
// Pattern: /appcontext/person/?
// Matching uri: /appcontext/person/name/Nikolay/zipcode/60169/city/Hoffman
// Metric name: /appcontext/person
// 
// Following patterns would NOT be valid:
// /appctx/myCar/?/something
// /appctx/myCar/something?
// /appctx/myCar/something??
// 
// 4: A combination of single wild cards * and either double wild card ** or question mark ? at the end of pattern is allowed:
// /appctx/my*Car*/**
// /appctx/my*Car*/?
// 
//  @formatter:on
package webmetrics.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webmetrics.util.StringUtils;

public abstract class AbstractUriPatternManager implements UriPatternManager {

	protected Logger logger = LoggerFactory.getLogger(getClass().getName());

	// throw exception if found invalid pattern, true by default
	// if false, only logs error and return result
	private boolean exceptionOnInvalidPattern = true;

	// common part of error messages
	protected static final String ERROR_MSG_WILD_CARD = "Invalid URI pattern detected:";
	protected static final String ERROR_MSG_URI_PATT_REQUIRED = "uriPattern is required parameter";
	protected static final String ERROR_MSG_URI_REQUIRED = "uri is required parameter";

	protected StringUtils textUtil = StringUtils.getInstance();

	/**
	 * Testing if double wild card "**" or uri terminator "?" used properly.<br>
	 * In case of error if exceptionOnInvalidPattern == true will throw IllegalArgumentException, otherwise will only log error and return result.<br>
	 * If double wild card or terminator is not found, returns true (valid).<br>
	 * If double wild card or terminator found properly, returns true.<br>
	 * If double wild card or terminator found but is not used properly, returns false or throws exception.<br>
	 * 
	 * @param uriPattern
	 *            - URI pattern to test for validity, required
	 * @param testRegex
	 *            - specific regex to use when testing URI pattern: REGEX_DOUBLE_WILD_CARD or REGEX_URI_TERMINATOR, required
	 * @return
	 */
	protected boolean validateWildCard(String uriPattern, String testRegex) {
		if (uriPattern == null)
			throw new IllegalArgumentException(ERROR_MSG_URI_PATT_REQUIRED);
		if (testRegex == null)
			throw new IllegalArgumentException("testRegex is required parameter");
		Pattern p = Pattern.compile(testRegex);
		uriPattern = uriPattern.trim();
		Matcher m = p.matcher(uriPattern);
		if (!m.matches())
			return true; // no "**" or "?" at all
		if (!m.group(2).equals("")) {
			// some characters after "**" or "?" found
			String error = ERROR_MSG_WILD_CARD + " non-empty URI after wild card: " + uriPattern;
			if (exceptionOnInvalidPattern)
				throw new IllegalArgumentException(error);
			logger.error(error);
			return false;
		}
		if (m.group(1).equals("")) {
			// nothing before "**" or "?"
			String error = ERROR_MSG_WILD_CARD + " empty URI before wild card: " + uriPattern;
			if (exceptionOnInvalidPattern)
				throw new IllegalArgumentException(error);
			logger.error(error);
			return false;
		}
		if (!m.group(1).endsWith("/")) {
			// "**" or "?" is not right after "/"
			String error = ERROR_MSG_WILD_CARD + " wild card does not follow \"/\": " + uriPattern;
			if (exceptionOnInvalidPattern)
				throw new IllegalArgumentException(error);
			logger.error(error);
			return false;
		}
		return true;
	}

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
	public String matchUri(String uri, UriPatternInfo uriPatternInfo) {
		if (uri == null)
			throw new IllegalArgumentException(ERROR_MSG_URI_REQUIRED);
		if (uriPatternInfo == null)
			throw new IllegalArgumentException("uriPatternInfo is required parameter");
		if (!uriPatternInfo.isSuccess() || uriPatternInfo.getRegex() == null || uriPatternInfo.getPattern() == null)
			throw new IllegalArgumentException("uriPatternInfo is invalid");
		uri = textUtil.stripTrailingSlash(uri);
		Matcher m = uriPatternInfo.getPattern().matcher(uri);
		if (!m.matches())
			return null;
		// in case terminator was used in pattern, the first group of matcher contains portion of URI just before terminator
		if (uriPatternInfo.isUriTerminator())
			return textUtil.stripTrailingSlash(m.group(1));
		return uri;
	}

	public boolean isExceptionOnInvalidPattern() {
		return exceptionOnInvalidPattern;
	}

	public void setExceptionOnInvalidPattern(boolean exceptionOnInvalidPattern) {
		this.exceptionOnInvalidPattern = exceptionOnInvalidPattern;
	}

	
}