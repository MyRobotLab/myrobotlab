package org.myrobotlab.service;

/*
 statics
 import static org.quartz.JobBuilder.newJob;
 import static org.quartz.TriggerBuilder.newTrigger;
 import static org.quartz.SimpleScheduleBuilder.*;
 import static org.quartz.TriggerBuilder.*;
 import static org.quartz.CronScheduleBuilder.*;
 import static org.quartz.DateBuilder.*;

 * 
 */

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.SimpleJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;

public class Scheduler extends Service {

	private static final long serialVersionUID = 1L;

	org.quartz.Scheduler scheduler = null;

	public final static Logger log = LoggerFactory.getLogger(Scheduler.class.getCanonicalName());

	String group = "group1";

	public Scheduler(String n) {
		super(n);
	}

	public void scheduleSimpleJob(String jobName, String triggerName, String cronString) {
		try {
			// define the job and tie it to our HelloJob class
			ArrayList<String> params = new ArrayList<String>();
			JobDataMap jdm = new JobDataMap();
			jdm.put("params", params);
			JobDetail job = newJob(SimpleJob.class).withIdentity(jobName).usingJobData("executable", "dir").usingJobData(jdm).build(); // default
																																		// group

			Trigger trigger = newTrigger()
			// .withIdentity("trigger3", group)
					.withIdentity(triggerName) // default group
					.withSchedule(cronSchedule(cronString)).forJob(jobName) // default
																			// group
					.build();

			scheduler.scheduleJob(job, trigger);

			/*
			 * ArrayList<String> params = new ArrayList<String>(); JobDataMap
			 * jdm = new JobDataMap(); jdm.put("params", params);
			 * 
			 * JobDetail job = newJob(SimpleJob.class) .withIdentity("job1")
			 * .usingJobData("executable", "dir") .usingJobData(jdm) .build();
			 * // default group
			 * 
			 * log.info("------- Initialization Complete -----------");
			 * 
			 * // computer a time that is on the next round minute
			 * 
			 * Date runTime = evenMinuteDate(new Date());
			 * 
			 * // Trigger the job to run on the next round minute Trigger
			 * trigger = newTrigger().withIdentity("trigger1") // default group
			 * .startAt(runTime) .withSchedule(simpleSchedule()
			 * .withIntervalInSeconds(10) .withRepeatCount(10)) // note that 10
			 * repeats will give a total of 11 firings .forJob(job) // identify
			 * job with handle to its JobDetail itself .build();
			 */
			// scheduler.scheduleJob(job, trigger);

			// cron - begin ----
			// reference
			// http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-06
			// http://www.adminschoice.com/crontab-quick-reference

			// cron - end ------

			// current time
			// long ctime = System.currentTimeMillis();

			/*
			 * // Initiate JobDetail with job name, job group, and executable
			 * job // class JobDetail jobDetail = new JobDetail("jobDetail2",
			 * "jobDetailGroup2", SimpleQuartzJob.class); // Initiate
			 * CronTrigger with its name and group name CronTrigger cronTrigger
			 * = new CronTrigger("cronTrigger", "triggerGroup2"); // setup
			 * CronExpression CronExpression cexp = new
			 * CronExpression("0/5 * * * * ?"); // Assign the CronExpression to
			 * CronTrigger cronTrigger.setCronExpression(cexp); // schedule a
			 * job with JobDetail and Trigger scheduler.scheduleJob(jobDetail,
			 * cronTrigger);
			 */
			// scheduler.getJobDetail(arg0)
		} catch (SchedulerException e) {
			Logging.logException(e);
		}
	}

	public boolean scheduleCronJob(String jobName, String triggerName, String cron) {
		JobDetail j;
		try {
			j = scheduler.getJobDetail(new JobKey(jobName)); // default group
			// "0 0/2 8-17 * * ?"
			Trigger cronTrigger = newTrigger().withIdentity("trigger3") // default
																		// group
					.withSchedule(cronSchedule("0 0/2 8-17 * * ?")).forJob(j) // adding
																				// to
																				// default
																				// group
					.build();
			scheduler.scheduleJob(j, cronTrigger);
			return true;
		} catch (SchedulerException e) {
			Logging.logException(e);
		}
		return false;
	}

	public List<String> getJobGroupNames() {
		try {
			return scheduler.getJobGroupNames();
		} catch (SchedulerException e) {
			Logging.logException(e);
		}
		return null;
	}

	public List<TriggerKey> getAllTriggerKeys() {
		List<TriggerKey> allTriggerKeys = new ArrayList<TriggerKey>();
		try {
			List<String> groups;
			groups = scheduler.getTriggerGroupNames();
			for (String group : groups) {
				Set<TriggerKey> keys = getTriggerKeys(group);
				if (keys != null) {
					for (TriggerKey key : keys) {
						allTriggerKeys.add(key);
					}
				}
			}

		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return allTriggerKeys;
	}

	public Set<TriggerKey> getTriggerKeys(String group) {
		try {
			@SuppressWarnings("unchecked")
			GroupMatcher<TriggerKey> groupMatcher = GroupMatcher.groupEquals(group);
			Set<TriggerKey> keys = scheduler.getTriggerKeys(groupMatcher);
			for (TriggerKey key : keys) {
				log.info("Removing trigger: " + key);
			}
			return keys;
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public JobDetail getJobDetail(String job) {
		try {
			return scheduler.getJobDetail(new JobKey(job)); // default group
		} catch (SchedulerException e) {
			Logging.logException(e);
		}
		return null;
	}

	// JDBC connection
	// http://vageeshhoskere.wordpress.com/2011/05/09/jdbc-job-store-quartz-scheduler/
	public void startScheduler() {
		try {

			Properties props = new Properties();
			// config begin ---------------------------------------------------
			props.put("org.quartz.scheduler.instanceName", "ALARM_SCHEDULER");
			props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
			props.put("org.quartz.threadPool.threadCount", "4");
			props.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");

			// specify the jobstore used
			props.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
			props.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
			props.put("org.quartz.jobStore.useProperties", "false");

			// The datasource for the jobstore that is to be used
			props.put("org.quartz.jobStore.dataSource", "myDS");

			// quartz table prefixes in the database
			props.put("org.quartz.jobStore.tablePrefix", "qrtz_");
			props.put("org.quartz.jobStore.misfireThreshold", "60000");
			props.put("org.quartz.jobStore.isClustered", "false");

			// The details of the datasource specified previously
			props.put("org.quartz.dataSource.myDS.driver", "com.mysql.jdbc.Driver");
			props.put("org.quartz.dataSource.myDS.URL", "jdbc:mysql://localhost:3306/viva");
			props.put("org.quartz.dataSource.myDS.user", "root");
			props.put("org.quartz.dataSource.myDS.password", "");
			props.put("org.quartz.dataSource.myDS.maxConnections", "20");

			// config end ---------------------------------------------------

			// FIXME - if file "quartz.properties" exists override
			/*
			 * FIXME FOR MEMORY ONLY if (scheduler == null) { scheduler =
			 * StdSchedulerFactory.getDefaultScheduler(); }
			 */

			SchedulerFactory schedulerFactory = new StdSchedulerFactory(props);
			scheduler = schedulerFactory.getScheduler();
			scheduler.start();

		} catch (SchedulerException e) {
			Logging.logException(e);
		}
	}

	public void stopScheduler() {
		try {
			if (scheduler == null) {
				scheduler.shutdown();
			}
		} catch (SchedulerException e) {
			Logging.logException(e);
		}

	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		String jobName = "hello03";
		String triggerName = "trigger04";

		Scheduler sched = new Scheduler("sched");
		sched.startService();
		sched.startScheduler();
		sched.scheduleSimpleJob(jobName, triggerName, "0/10 * * * * ?");

		log.info(sched.getJobDetail("hello").toString());

		List<String> groups = sched.getJobGroupNames();
		for (int i = 0; i < groups.size(); ++i) {
			log.info(groups.get(i));
		}

		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");

		try {
			Context ctx = new InitialContext(env);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<TriggerKey> triggersInGroup = sched.getAllTriggerKeys();
		log.info("{}",triggersInGroup.size());

		// sched.stopScheduler();
		// sched.stopService();
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}

}
