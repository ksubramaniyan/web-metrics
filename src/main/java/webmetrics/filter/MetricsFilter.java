/**
 * Please see doc for WebMetricManager for details.<br>
 */
package webmetrics.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webmetrics.config.MetricConfig;
import webmetrics.core.MetricsManager;
import webmetrics.core.UriTimer;

public class MetricsFilter implements Filter {

	private MetricsManager webMetricManager;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.info("Initializing WebMetricsFilter");
		System.out.println("Initializing WebMetricsFilter");
		// usually metric manager would be created by listener, but in case listener was not configured, manager will be created right now
		// code to initialize manager is similar to one used by listener
		synchronized (filterConfig.getServletContext()) {
			webMetricManager = MetricsManager.class.cast(filterConfig.getServletContext().getAttribute(MetricsManager.class.getSimpleName()));
			
			if (webMetricManager != null) { 
				logger.info("WebMetricManager already initialized");
			} else {
				logger.info("Will attempt to initialize  WebMetricManager");
				//String webMetricConfigName = filterConfig.getServletContext().getInitParameter(WebMetricConfig.WEB_METRIC_CONFIG_KEY);
				String webMetricConfigName = filterConfig.getInitParameter(MetricConfig.WEB_METRIC_CONFIG_KEY);
				if (webMetricConfigName != null) {
					logger.info("Found custom web metric config name: " + webMetricConfigName);
				} else {
					logger.info("Did not find custom web metric config name");
				}
				//webMetricManager = new WebMetricManager(webMetricConfigName);
				webMetricManager = MetricsManager.instance(webMetricConfigName);
				logger.info("Created instance of web metric manager: " + webMetricManager);
				filterConfig.getServletContext().setAttribute(MetricsManager.class.getSimpleName(), webMetricManager);
				
				//TODO:
				
				//Setting the metrics in the servlet context for the admin servlet;
				filterConfig.getServletContext().setAttribute("com.codahale.metrics.servlets.MetricsServlet.registry", webMetricManager.getMetricsRegistry());
			}
		}
		if (webMetricManager.isEnabled()) {
			webMetricManager.getJmxReporter().start();
		} else {
			logger.warn(" Web Metrics is not enabled");
		}
	}

	@Override
	public void destroy() {
		logger.info("Destroying WebMetricsFilter");
		if (webMetricManager.isEnabled()) {
			webMetricManager.getJmxReporter().stop();
		}
	}

	/**
	 * This method has been modified as below to remove the ThreadLocal
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String uri = ((HttpServletRequest) request).getRequestURI().toString();
		System.out.println("doFilter  WebMetricsFilter" +uri);
		final StatusExposingServletResponse wrappedResponse = new StatusExposingServletResponse((HttpServletResponse) response);
		UriTimer timer = webMetricManager.startTimerForUri(uri, getConsumer((HttpServletRequest) request));
		try {
			chain.doFilter(request, wrappedResponse);
		} finally {
			webMetricManager.stopTimer(wrappedResponse.getStatus(), timer);
		}
	}

	private String getConsumer(HttpServletRequest request) {
		if (request == null)
			return null;
		if (webMetricManager == null || webMetricManager.getWebMetricConfig() == null
						|| webMetricManager.getWebMetricConfig().getIdField() == null
						|| webMetricManager.getWebMetricConfig().getIdFieldscope() == null
						|| webMetricManager.getWebMetricConfig().getConsumers() == null)
			return null;
		// currently working only with header and cookie fields
		if (!"header".equals(webMetricManager.getWebMetricConfig().getIdFieldscope())
						&& !"cookie".equals(webMetricManager.getWebMetricConfig().getIdFieldscope()))
			return null;
		if ("header".equals(webMetricManager.getWebMetricConfig().getIdFieldscope())) {
			String headerValue = request.getHeader(webMetricManager.getWebMetricConfig().getIdField());
			if (headerValue == null || headerValue.trim().equals(""))
				return null;
			return headerValue.trim();
		} else if ("cookie".equals(webMetricManager.getWebMetricConfig().getIdFieldscope())) {
			Cookie[] cookies = request.getCookies();
			for (Cookie cookie : cookies) {
				if (webMetricManager.getWebMetricConfig().getIdField().equals(cookie.getName())) {
					String cookieValue = cookie.getValue();
					if (cookieValue == null || cookieValue.trim().equals(""))
						return null;
					return cookieValue.trim();
				}
			}
		}
		return null;
	}

	private static class StatusExposingServletResponse extends HttpServletResponseWrapper {
		// The Servlet spec says: calling setStatus is optional, if no status is
		// set, the default is 200.
		private int httpStatus = 200;

		public StatusExposingServletResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void sendError(int sc) throws IOException {
			httpStatus = sc;
			super.sendError(sc);
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			httpStatus = sc;
			super.sendError(sc, msg);
		}

		@Override
		public void setStatus(int sc) {
			httpStatus = sc;
			super.setStatus(sc);
		}

		public int getStatus() {
			return httpStatus;
		}
	}

	private static Logger logger = LoggerFactory.getLogger(MetricsFilter.class);

}
