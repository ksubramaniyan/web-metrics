package webmetrics.spring;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import webmetrics.filter.MetricsFilter;

@Configuration
public class MetricsSpringBootConfiguration {

	@Bean
    public FilterRegistrationBean shallowEtagHeaderFilter() {
		Map<String,String> initParam = new HashMap<String, String>();
		initParam.put("webMetricConfig", "hdwebmetricconfig.xml");
        FilterRegistrationBean registration = new FilterRegistrationBean();
       // registration.setFilter(new WebStatsFilter());
        registration.setFilter(new MetricsFilter());
       // registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
        registration.addUrlPatterns("/*");
        registration.setInitParameters(initParam);
        return registration;
    }
}
