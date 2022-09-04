package org.myrobotlab.framework.interfaces;

import java.util.Map;
import java.util.Timer;

public interface TaskManager {
  
  /**
   * get all timed tasks for this service
   * @return - returns all currently defined tasks
   */
  public Map<String, Timer> getTasks();

  /**
   * purge a task
   * @param taskName - name of task to be purged
   */
  public void purgeTask(String taskName);
  
  /**
   * purge all tasks
   */
  public void purgeTasks();
  
  /**
   * add a repeating task with interval intervalMs
   * 
   * @param intervalMs - interval from "now" to invoke task
   * @param method - method to invoke
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

}
