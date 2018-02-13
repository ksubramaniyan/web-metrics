/**
 * Structure to simplify bucket logic.<br>
 * Holds value for single bucket for given URI.<br>
 */
package webmetrics.core;

public class HistogramBucket {

	private int low; // low boundary for bucket, inclusive
	private int high; // high boundary for bucket, exclusive
	private String metricName; // fully generated metric name

	public int getLow() {
		return low;
	}

	public void setLow(int low) {
		this.low = low;
	}

	public int getHigh() {
		return high;
	}

	public void setHigh(int high) {
		this.high = high;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

}
