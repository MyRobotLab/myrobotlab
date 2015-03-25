package org.myrobotlab.service;

import it.sauronsoftware.cron4j.Scheduler;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Cron extends Service {

	public static class Task implements Serializable, Runnable {
		private static final long serialVersionUID = 1L;
		transient Cron myService;
		public String cronPattern;
		public String name;
		public String method;
		public Object[] data;

		public Task(Cron myService, String cronPattern, String name, String method) {
			this(myService, cronPattern, name, method, (Object[]) null);
		}

		public Task(Cron myService, String cronPattern, String name, String method, Object... data) {
			this.myService = myService;
			this.cronPattern = cronPattern;
			this.name = name;
			this.method = method;
			this.data = data;
		}

		@Override
		public void run() {
			log.info(String.format("%s Cron firing message %s->%s.%s", myService.getName(), name, method, data));
			myService.send(name, method, data);
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Cron.class.getCanonicalName());

	transient private Scheduler scheduler = new Scheduler();

	// Schedule a once-a-week task at 8am on Sunday.
	// 0 8 * * 7
	// Schedule a twice a day task at 7am and 6pm on weekdays
	// 0 7 * * 1-5 |0 18 * * 1-5

	public final static String EVERY_MINUTE = "* * * * *";

	public ArrayList<Task> tasks = new ArrayList<Task>();

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			Cron cron = (Cron) Runtime.start("cron", "Cron");// new
																// Cron("cron");
			cron.startService();

			/*
			 * cron.addScheduledEvent("0 6 * * 1,3,5","arduino","digitalWrite",
			 * 13, 1);
			 * cron.addScheduledEvent("0 7 * * 1,3,5","arduino","digitalWrite",
			 * 12, 1);
			 * cron.addScheduledEvent("0 8 * * 1,3,5","arduino","digitalWrite",
			 * 11, 1);
			 * 
			 * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 13,
			 * 0); cron.addScheduledEvent("59 * * * *","arduino","digitalWrite",
			 * 12, 0);
			 * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 11,
			 * 0);
			 */
			cron.addTask("* * * * *", "cron", "test", 7);

			// cron.addScheduledEvent(EVERY_MINUTE, "log", "log");
			// west wall | back | east wall

			String json = Encoder.toJson(cron.getTasks());

			log.info("here");

			// Runtime.createAndStart("webgui", "WebGUI");

			// 1. doug - find location where checked in ----
			// 2. take out security token from DL broker's response
			// 3. Tony - status ? and generated xml responses - "update" looks
			// ok

			// Runtime.createAndStart("gui", "GUIService");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Cron(String n) {
		super(n);
	}

	public String addTask(String cron, String serviceName, String method) {
		return addTask(cron, serviceName, method, (Object[]) null);
	}

	public String addTask(String cron, String name, String method, Object... data) {
		Task task = new Task(this, cron, name, method, data);
		tasks.add(task);
		return scheduler.schedule(cron, task);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "scheduling" };
	}

	@Override
	public String getDescription() {
		return "A Cron like service capable of scheduling future actions";
	}

	public ArrayList<Task> getTasks() {
		return tasks;
	}

	@Override
	public void startService() {
		super.startService();
		if (!scheduler.isStarted()) {
			scheduler.start();
		}
	}

	@Override
	public void stopService() {
		super.stopService();
		if (scheduler.isStarted()) {
			scheduler.stop();
		}
	}

	public int test(Integer data) {
		log.info("data {}", data);
		return data;
	}

}
