package org.myrobotlab.service;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.CronConfig;
import org.slf4j.Logger;

import it.sauronsoftware.cron4j.Scheduler;

/**
 * Cron - This is a cron based service that can execute a "task".
 * 
 */
public class Cron extends Service {

  public static class Task implements Serializable, Runnable {

    private static final long serialVersionUID = 1L;
    /**
     * cron pattern for this task
     */
    public String cronPattern;

    /**
     * data parameters to invoke
     */
    public Object[] data;

    /**
     * unique hash the scheduler uses (only)
     */
    transient public String hash;

    /**
     * unique id for the user to use
     */
    public String id;

    /**
     * method to invoke
     */
    public String method;

    /**
     * reference to service
     */
    transient Cron cron;

    /**
     * name of the target service
     */
    public String name;

    public Task() {
    }

    public Task(Cron cron, String id, String cronPattern, String name, String method) {
      this(cron, id, cronPattern, name, method, (Object[]) null);
    }

    public Task(Cron cron, String id, String cronPattern, String name, String method, Object... data) {
      this.cron = cron;
      this.id = id;
      this.cronPattern = cronPattern;
      this.name = name;
      this.method = method;
      this.data = data;
    }

    @Override
    public void run() {
      if (cron != null) {
        log.info("{} Cron firing message {}->{}.{}", cron.getName(), name, method, data);
        cron.send(name, method, data);
      } else {
        log.error("cron service is null");
      }
    }

    @Override
    public String toString() {
      return String.format("%s, %s, %s, %s", id, cronPattern, name, method);
    }
  }

  public final static Logger log = LoggerFactory.getLogger(Cron.class);

  private static final long serialVersionUID = 1L;

  /**
   * the thing that translates all the cron pattern values and implements actual tasks
   */
  transient private Scheduler scheduler = new Scheduler();

  public Cron(String n, String id) {
    super(n, id);
  }

  /**
   * Add a named task with out parameters
   * 
   * @param id
   * @param cron
   * @param serviceName
   * @param method
   * @return
   */
  public String addNamedTask(String id, String cron, String serviceName, String method) {
    return addNamedTask(id, cron, serviceName, method, (Object[]) null);
  }

  /**
   * Add a named task with parameters
   * 
   * @param id
   * @param cron
   * @param serviceName
   * @param method
   * @param data
   * @return
   */
  public String addNamedTask(String id, String cron, String serviceName, String method, Object... data) {
    CronConfig c = (CronConfig) config;
    Task task = new Task(this, id, cron, serviceName, method, data);
    task.id = id;
    task.hash = scheduler.schedule(cron, task);
    c.tasks.put(id, task);
    broadcastState();
    return id;
  }

  /**
   * 
   * @param task
   * @return
   */
  public String addNamedTask(Task task) {
    CronConfig c = (CronConfig) config;
    task.hash = scheduler.schedule(task.cronPattern, task);
    c.tasks.put(task.id, task);
    broadcastState();
    return task.id;
  }

  /**
   * Add a task with out parameters, the name will be generated guid
   * 
   * @param cron
   * @param serviceName
   * @param method
   * @return
   */
  public String addTask(String cron, String serviceName, String method) {
    String id = UUID.randomUUID().toString();
    return addNamedTask(id, cron, serviceName, method, (Object[]) null);
  }

  /**
   * Add a task with parameters, the name will be generated guid
   * 
   * @param cron
   * @param serviceName
   * @param method
   * @param data
   * @return
   */
  public String addTask(String cron, String serviceName, String method, Object... data) {
    String id = UUID.randomUUID().toString();
    return addNamedTask(id, cron, serviceName, method, data);
  }

  public Map<String, Task> getCronTasks() {
    CronConfig c = (CronConfig) config;
    return c.tasks;
  }

  /**
   * removes task by id
   * @param id - id of the task to remove
   * @return the removed task if it exists
   */
  public Task removeTask(String id) {
    CronConfig c = (CronConfig) config;
    Task t = c.tasks.remove(id);
    if (t != null) {
      scheduler.deschedule(t.hash);
    } else {
      log.error("%s could not find task %s to remove", getName(), id);
    }
    broadcastState();
    return t;
  }

  /**
   * removes all the tasks without stopping the scheduler
   */
  public void removeAllTasks() {
    CronConfig c = (CronConfig) config;
    for (Task t : c.tasks.values()) {
      scheduler.deschedule(t.hash);
    }
    c.tasks.clear();
  }

  @Override
  public void startService() {
    super.startService();
    start();
  }

  @Override
  public void stopService() {
    super.stopService();
    stop();
  }

  /**
   * start the schedular and all associated tasks
   */
  public void start() {
    if (!scheduler.isStarted()) {
      scheduler.start();
    }
  }

  /**
   * stop the schedular ad all associated tasks
   */
  public void stop() {
    removeAllTasks();
    if (scheduler.isStarted()) {
      scheduler.stop();
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      Cron cron = (Cron) Runtime.start("cron", "Cron");
      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
      mega.connect("/dev/ttyACM2");

      Runtime.start("webgui", "WebGui");

      /*
       * 
       * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 13, 0);
       * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 12, 0);
       * cron.addScheduledEvent("59 * * * *","arduino","digitalWrite", 11, 0);
       */
      // every odd minute
      String id = cron.addNamedTask("led on", "1-59/2 * * * *", "mega", "digitalWrite", 13, 1);
      // every event minute
      String id2 = cron.addNamedTask("led off", "*/2 * * * *", "mega", "digitalWrite", 13, 0);

      // Runtime.createAndStart("webgui", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
