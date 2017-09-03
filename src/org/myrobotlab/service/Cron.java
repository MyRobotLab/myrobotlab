package org.myrobotlab.service;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import it.sauronsoftware.cron4j.Scheduler;

/**
 * Cron - This is a cron based service that can execute a "task" at some point
 * in the future such as "invoke this method on that service"
 * 
 * FIXME - the common cron notation is kind of nice - but this thing doesn't do
 * more than Service.addTask
 * 
 * FIXME - make a purge &amp; delete DUH !
 *
 */
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
    LoggingFactory.init(Level.INFO);

    try {
      Cron cron = (Cron) Runtime.start("cron", "Cron");// new
      // Cron("cron");
      cron.startService();

      /*
       * cron.addScheduledEvent("0 6 * * 1,3,5","arduino","digitalWrite", 13,
       * 1); cron.addScheduledEvent("0 7 * * 1,3,5","arduino","digitalWrite",
       * 12, 1); cron.addScheduledEvent("0 8 * * 1,3,5"
       * ,"arduino","digitalWrite", 11, 1);
       * 
       * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 13, 0);
       * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 12, 0);
       * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 11, 0);
       */
      cron.addTask("* * * * *", "cron", "test", 7);

      // cron.addScheduledEvent(EVERY_MINUTE, "log", "log");
      // west wall | back | east wall

      String json = CodecUtils.toJson(cron.getTasks());

      log.info("here {}", json);

      // Runtime.createAndStart("webgui", "WebGui");

      // 1. doug - find location where checked in ----
      // 2. take out security token from DL broker's response
      // 3. Tony - status ? and generated xml responses - "update" looks
      // ok

      // Runtime.createAndStart("gui", "SwingGui");
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Cron(String n) {
    super(n);
  }

  /*
   * addTask - Add a task to the cron service to invoke a method on a service on
   * some schedule.
   * 
   * @param cron
   *          - The cron string to define the schedule
   * @param serviceName
   *          - The name of the service to invoke
   * @param method
   *          - the method on the service to invoke when the task starts.
   */
  public String addTask(String cron, String serviceName, String method) {
    return addTask(cron, serviceName, method, (Object[]) null);
  }

  /*
   * addTask - Add a task to the cron service to invoke a method on a service on
   * some schedule.
   * 
   * @param cron
   *          - The cron string to define the schedule
   * @param serviceName
   *          - The name of the service to invoke
   * @param method
   *          - the method on the service to invoke when the task starts.
   * @param data
   *          - additional objects/varags to pass to the method
   */
  public String addTask(String cron, String serviceName, String method, Object... data) {
    Task task = new Task(this, cron, serviceName, method, data);
    tasks.add(task);
    return scheduler.schedule(cron, task);
  }

  public ArrayList<Task> getCronTasks() {
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

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Cron.class.getCanonicalName());
    meta.addDescription("is a cron like service capable of scheduling future actions using cron syntax");
    meta.addCategory("scheduling");
    meta.addDependency("it.sauronsoftware.cron4j", "2.2.5");
    return meta;
  }

}
