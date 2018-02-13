package webmetrics.core;

import com.codahale.metrics.Timer;

public class UriTimer {

	private Timer timer;
	private Timer consumerTimer;
	private String uri;
	private String metricName;
	private Timer.Context context;
	private Timer.Context consumerContext;

	public UriTimer(Timer timer, String uri, String metricName) {
		this.timer = timer;
		this.uri = uri;
		this.metricName = metricName;
	}

	public UriTimer(Timer timer, Timer consumerTimer, String uri, String metricName) {
		this.timer = timer;
		this.consumerTimer = consumerTimer;
		this.uri = uri;
		this.metricName = metricName;
	}

	public Timer getTimer() {
		return timer;
	}

	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public void start() {
		if (timer != null) {
			context = timer.time();
		}
		if (consumerTimer != null) {
			consumerContext = consumerTimer.time();
		}
	}

	public long stop() {
		if (consumerContext != null) {
			consumerContext.stop();
		}
		if (context != null) {
			return context.stop();
		} else {
			return 0;
		}
	}
}
