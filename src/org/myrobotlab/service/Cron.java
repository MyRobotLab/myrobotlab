package org.myrobotlab.service;

import it.sauronsoftware.cron4j.Scheduler;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

@Root
public class Cron extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Cron.class.getCanonicalName());
	transient private Scheduler scheduler = new Scheduler();

	public final static String EVERY_MINUTE = "* * * * *";

	@ElementList(required = false)
	public ArrayList<Task> tasks = new ArrayList<Task>();

	@Default
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

	public Cron(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void addTask(String cron, String serviceName, String method) {
		addTask(cron, serviceName, method, (Object[]) null);
	}

	public void addTask(String cron, String name, String method, Object... data) {
		Task task = new Task(this, cron, name, method, data);
		tasks.add(task);
		scheduler.schedule(cron, task);
	}

	public void startService() {
		super.startService();
		if (!scheduler.isStarted()) {
			scheduler.start();
		}
	}

	public ArrayList<Task> getTasks() {
		return tasks;
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

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Cron cron = (Cron) Runtime.start("cron", "Cron");// new Cron("cron");
		cron.startService();

		/*
		 * cron.addScheduledEvent("0 6 * * 1,3,5","arduino","digitalWrite", 13,
		 * 1); cron.addScheduledEvent("0 7 * * 1,3,5","arduino","digitalWrite",
		 * 12, 1);
		 * cron.addScheduledEvent("0 8 * * 1,3,5","arduino","digitalWrite", 11,
		 * 1);
		 * 
		 * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 13, 0);
		 * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 12, 0);
		 * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 11, 0);
		 */
		cron.addTask("* * * * *", "cron", "test", 7);

		// cron.addScheduledEvent(EVERY_MINUTE, "log", "log");
		// west wall | back | east wall

		String json = Encoder.gson.toJson(cron.getTasks());

		log.info("here");

		// Runtime.createAndStart("webgui", "WebGUI");

		// 1. doug - find location where checked in ----
		// 2. take out security token from DL broker's response
		// 3. Tony - status ? and generated xml responses - "update" looks ok

		// Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

}
