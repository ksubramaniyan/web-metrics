/**
 * Structure to simplify bucket logic.<br>
 * Contains all buckets defined for single URI.<br>
 */
package webmetrics.core;

public class HistogramBuckets {

	private HistogramBucket[] uriBucket;

	public HistogramBucket[] getUriBucket() {
		return uriBucket;
	}

	public void setUriBucket(HistogramBucket[] uriBucket) {
		this.uriBucket = uriBucket;
	}
}
