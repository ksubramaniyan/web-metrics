/**
 * Instance of this class is produced after parsing URI/URI Pattern
 */
package webmetrics.core;

import java.util.regex.Pattern;

public class UriPatternInfo {

	// overall result of parsing, default true
	private boolean success = true;
	// if URI pattern has double wild card
	private boolean doubleWildCard;
	// if URI pattern has URi terminator
	private boolean uriTerminator;
	// actual regex to test actual URI for match; null if URI does not use pattern or success==false
	private String regex;
	// pattern object generated from regex
	private Pattern pattern;
	// metric name used to report; if left null, actual URI will be used instead
	private String metricName;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isDoubleWildCard() {
		return doubleWildCard;
	}

	public void setDoubleWildCard(boolean doubleWildCard) {
		this.doubleWildCard = doubleWildCard;
	}

	public boolean isUriTerminator() {
		return uriTerminator;
	}

	public void setUriTerminator(boolean uriTerminator) {
		this.uriTerminator = uriTerminator;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	/**
	 * Overriding to make this class able to compare two instances for equality.<br>
	 * Only valid parsed patterns are of interest, anything else will be considered equal.<br>
	 * Instance needs to be fully initialized with pattern and regex assigned for this to work correctly.<br>
	 */
	@Override
	public int hashCode() {
		if (regex != null)
			return regex.hashCode();
		return 0;
	}

	/**
	 * Overriding to make this class able to compare two instances for equality.<br>
	 * Only valid parsed patterns are of interest, anything else will be considered equal.<br>
	 * Instance needs to be fully initialized with pattern and regex assigned for this to work correctly.<br>
	 */
	@Override
	public boolean equals(Object anotherInfo) {
		if (anotherInfo == null || !getClass().isAssignableFrom(anotherInfo.getClass()))
			return false;
		return (success == getClass().cast(anotherInfo).isSuccess() && (regex != null && regex.equals(getClass().cast(anotherInfo).getRegex()) || (regex == null && getClass()
						.cast(anotherInfo).getRegex() == null)));
	}

	@Override
	public String toString() {
		return "UriPatternInfo{success:" + success + ",doubleWildCard:" + doubleWildCard + ",uriTerminator:" + uriTerminator + ",regex:" + regex + "}";
	}

}
