package webmetrics.admin;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public class ThreadDumpManager {

	 private final ThreadMXBean threadMXBean ;
	 
	 public ThreadDumpManager() {
		 threadMXBean = ManagementFactory.getThreadMXBean();
	 }
	
	 public ThreadDump dumpThreads() {
		 final ThreadInfo[] threads = this.threadMXBean.dumpAllThreads(true, true);
		 System.out.println("inside dumpThreads... threads ="+threads.length);
		 ThreadDump threadDump = new ThreadDump();
		 for (int ti = threads.length - 1; ti >= 0; ti--) {
			 threadDump.addThread(threads[ti]);
		 }
		 
		 return threadDump;
	 }
	 
	 public class ThreadDump {
		 private List<ThreadInfo> threads;
		 
		 ThreadDump() {
			 threads = new ArrayList<ThreadInfo>();
		 }
		 
		 public void addThread(ThreadInfo argThread) {
			 threads.add(argThread);
		 }

		public List<ThreadInfo> getThreads() {
			return threads;
		}
		 
	 }
	 
	 public class ThreadDetail {
		 
		 
	 }
}
