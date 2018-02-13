package webmetrics.spring;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import webmetrics.admin.HealthCheckManager;
import webmetrics.admin.ThreadDumpManager;
import webmetrics.admin.HealthCheckManager.HealthCheckResponse;
import webmetrics.admin.ThreadDumpManager.ThreadDump;
import webmetrics.core.MetricsManager;

@RestController
public class MetricsAdminController {

	
	private MetricsManager wManager = MetricsManager.getInstance();
	private ThreadDumpManager tManager = new ThreadDumpManager();
	private HealthCheckRegistry hReg = HealthCheckManager.getRegistry();
	
	private transient ObjectMapper mapper;
	
	public MetricsAdminController() {
		mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS,
				TimeUnit.SECONDS,
                false));
	}
	
	
	@RequestMapping(value = "/admin/metrics")
	public String getMetrics()  {
		String metrics="";
		try {
			metrics =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wManager.getMetricsRegistry());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return metrics;
	}
	
	@RequestMapping(value = "/admin/dump")
	public ThreadDump getThreadDump()  {
		if( tManager == null) {
			tManager = new ThreadDumpManager();
		}
		return tManager.dumpThreads();
	}
	
	/*@RequestMapping(value = "/admin/health")
	public SortedMap<String, HealthCheck.Result> checkHealth()  {
		return hReg.runHealthChecks();
	}
	*/
	@RequestMapping(value = "/admin/health")
	public HealthCheckResponse checkHealth()  {
		return HealthCheckManager.runHealthChecks();
	}
}
