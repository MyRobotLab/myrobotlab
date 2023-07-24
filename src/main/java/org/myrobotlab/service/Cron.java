package org.myrobotlab.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.CronConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

import it.sauronsoftware.cron4j.Scheduler;

/**
 * Cron - This is a cron based service that can execute a "task".
 * It does not need an operating system in order to run.  It is a
 * pure java implementation of a cron service.  It accepts cron 
 * patterns and will execute a task based on the pattern.  The
 * task is a message that is sent to a service.  The message
 * can be any message that the service accepts. 
 * 
 */
public class Cron extends Service {
  
  public static class Task implements Serializable, Runnable {

    private static final long serialVersionUID = 1L;
    /**
     * reference to service
     */
    transient Cron cron;

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
     * id for the user to use
     */
    public String id;

    /**
     * method to invoke
     */
    public String method;

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
        log.info("{} Cron firing message {}->{}.{}", cron.getName(), name, method, data);
        cron.send(name, method, data);
        cron.history.add(new TaskHistory(id, new Date()));
        if (cron.history.size() > cron.HISTORY_SIZE) {
          cron.history.remove(0);
        }
        cron.broadcastState();
    }

    @Override
    public String toString() {
      return String.format("%s, %s, %s, %s", id, cronPattern, name, method);
    }
  }
  
  public static class TaskHistory {
    public String id;
    public Date processedTime;
    
    public TaskHistory(String id, Date now) {
      this.id = id;
      this.processedTime = now;
    }    
  }

  public final static Logger log = LoggerFactory.getLogger(Cron.class);

  private static final long serialVersionUID = 1L;

  /**
   * history buffer of tasks that have been executed
   */
  final protected List<TaskHistory> history = new ArrayList<>();

  /**
   * max size of history buffer
   */
  final int HISTORY_SIZE = 30;
  
  /**
   * the thing that translates all the cron pattern values and implements actual tasks
   */
  transient private Scheduler scheduler = new Scheduler();

  /**
   * map of tasks organized by id
   */
  protected Map<String, Task> tasks = new LinkedHashMap<>();

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
  public String addTask(String id, String cron, String serviceName, String method) {
    return addTask(id, cron, serviceName, method, (Object[]) null);
  }

  /**
   * Add a named task with parameters
   * 
   * @param id
   * @param cronPattern
   * @param serviceName
   * @param method
   * @param data
   * @return
   */
  public String addTask(String id, String cronPattern, String serviceName, String method, Object... data) {    
    Task task = new Task(this, id, cronPattern, serviceName, method, data);
    addTask(task);
    return id;
  }


  /**
   * 
   * @param task
   * @return
   */
  public String addTask(Task task) {
    if (tasks.containsKey(task.id)) {
      log.info("descheduling prexisting task {} hash {}", task.id, task.hash);
      scheduler.deschedule(task.id);
    }
    log.info("scheduling task {}", task.id);
    task.hash = scheduler.schedule(task.cronPattern, task);
    task.cron = this;
    tasks.put(task.id, task);
    broadcastState();
    return task.id;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    // deschedule current tasks
    removeAllTasks();
    
    // add new tasks
    CronConfig config = (CronConfig)c;
    for (Task task : config.tasks) {
      addTask(task);
    }
    return c;
  }

  @Override
  public ServiceConfig getConfig() {
    CronConfig c = (CronConfig)config;
    c.tasks = new ArrayList<>();
    for (Task task: tasks.values()) {
      c.tasks.add(task);
    }
    return c;
  }

  public Map<String, Task> getCronTasks() {
    return tasks;
  }

  /**
   * get a task from id
   * @param id
   * @return
   */
  public Task getTask(String id) {
    return tasks.get(id);
  }

  /**
   * removes all the tasks without stopping the scheduler
   */
  public void removeAllTasks() {
    for (Task t : tasks.values()) {
      scheduler.deschedule(t.hash);
    }
    tasks.clear();
  }

  /**
   * removes task by id
   * @param id - id of the task to remove
   * @return the removed task if it exists
   */
  public Task removeTask(String id) {
    Task t = tasks.remove(id);
    if (t != null) {
      scheduler.deschedule(t.hash);
    } else {
      log.error("%s could not find task %s to remove", getName(), id);
    }
    broadcastState();
    return t;
  }
  
  /**
   * start the schedular and all associated tasks
   */
  public void start() {
    if (!scheduler.isStarted()) {
      scheduler.start();
    }
  }
  
  @Override
  public void startService() {
    super.startService();
    start();
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
  

  @Override
  public void stopService() {
    super.stopService();
    stop();
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
      String id = cron.addTask("led on", "1-59/2 * * * *", "mega", "digitalWrite", 13, 1);
      // every event minute
      String id2 = cron.addTask("led off", "*/2 * * * *", "mega", "digitalWrite", 13, 0);

      // Runtime.createAndStart("webgui", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }
  
}
