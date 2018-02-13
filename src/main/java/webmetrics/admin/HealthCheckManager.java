package webmetrics.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

public class HealthCheckManager {

	 private static final HealthCheckRegistry HEALTH_CHECK_REGISTRY = new HealthCheckRegistry();
	 
	 public static void addHealthCheck(String argName,HealthCheck argHealth) {
		 HEALTH_CHECK_REGISTRY.register(argName, argHealth);
	 }
	 
	 public static HealthCheckRegistry getRegistry() {
		 return HEALTH_CHECK_REGISTRY;
	 }
	 
	 public static HealthCheckResponse runHealthChecks() {
		 
		 SortedMap<String, HealthCheck.Result> results =  HEALTH_CHECK_REGISTRY.runHealthChecks();
		 HealthCheckResponse res = new HealthCheckResponse();	
		 for(Entry<String, HealthCheck.Result> rEntry :results.entrySet()) {
			 HealthCheck.Result result = rEntry.getValue();
			 res.addHealthCheck(rEntry.getKey(), String.valueOf(result.isHealthy()));
			// res.addError(rEntry.getKey(), result.getError());
		 }
		 
		 return res;
	 }
	 
	 public static class HealthCheckResponse {

			private Map<String,String> healthCheck = new HashMap<String,String>();
			
			private Map<String,String> errors = new HashMap<String,String>();

			public void addHealthCheck(String name, String status) {
				healthCheck.put(name, status);
			}
			
			public void addError(String name, String status) {
				errors.put(name, status);
			}
			
			public Map<String, String> getHealthCheck() {
				return healthCheck;
			}

			public Map<String, String> getErrorCause() {
				return errors;
			}
			
			
		}

}
