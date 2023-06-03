package org.myrobotlab.framework.interfaces;

import java.util.Map;
import java.util.Timer;

public interface TaskManager {

  /**
   * get all timed tasks for this service
   * 
   * @return - returns all currently defined tasks
   */
  public Map<String, Timer> getTasks();

  /**
   * purge a task
   * 
   * @param taskName
   *          - name of task to be purged
   */
  public void purgeTask(String taskName);

  /**
   * purge all tasks
   */
  public void purgeTasks();

  /**
   * add a repeating task with interval intervalMs
   * 
   * @param intervalMs
   *          - interval from "now" to invoke task
   * @param method
   *          - method to invoke
   */
  public void addTask(long intervalMs, String method);

  public void addTask(long intervalMs, String method, Object... params);

  public void addTaskOneShot(long delayMs, String method, Object... params);

  /**
   * a stronger bigger better task handler !
   * 
   * @param taskName
   *          task name
   * @param intervalMs
   *          how frequent in milliseconds
   * @param delayMs
   *          the delay
   * @param method
   *          the method
   * @param params
   *          the params to pass
   */
  public void addTask(String taskName, long intervalMs, long delayMs, String method, Object... params);

  /**
   * 
   * @param taskName
   *          - unique name of task
   * @param oneShot
   *          - this does not do a repeated task, rather it activates from the
   *          current time with the delayMs
   * @param intervalMs
   *          - time interval in ms until the next task event
   * @param delayMs
   *          - the amount of delay from current time when the first event will
   *          start
   * @param method
   *          - method to invoke
   * @param params
   *          - parameters to the method
   */
  public void addTask(String taskName, boolean oneShot, long intervalMs, long delayMs, String method, Object... params);
  
  /**
   * checks if a named tasks exist
   * 
   * @param taskName
   *          - name of task
   * @return - returns task if defined
   */
  public boolean containsTask(String taskName);


}
