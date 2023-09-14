package org.myrobotlab.framework;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A generic class 
 * @author GroG
 *
 */
public class Task extends TimerTask {

  /**
   * unique name of task
   */
  String taskName;
  /**
   * message to send at each interval
   */
  Message msg;
  /**
   * interval of time to pass before sending another message
   */
  long interval = 0;
  /**
   * services associated with this task
   */
  Service myService;
  
  /**
   * if its not interval but a single shot timer event after which its removed
   */
  boolean oneShot = false;

  // FIXME upgrade to ScheduledExecutorService
  // http://howtodoinjava.com/2015/03/25/task-scheduling-with-executors-scheduledthreadpoolexecutor-example/

  public Task(Service myService, boolean oneShot, String taskName, long interval, Message msg) {
    this.myService = myService;
    this.taskName = taskName;
    this.interval = interval;
    this.oneShot = oneShot;
    this.msg = msg;
  }

  public Task(Task s) {
    this.msg = s.msg;
    this.interval = s.interval;
    this.taskName = s.taskName;
    this.myService = s.myService;
    this.oneShot = s.oneShot;
  }

  @Override
  public void run() {
    // info("task %s running - next run %s", taskName,
    // MathUtils.msToString(interval));
    myService.invoke(msg);

    // GroG commented out 2019.07.14 for preferrable "blocking" task
    // myService.getInbox().add(msg);

    if (interval > 0) {
      Task t = new Task(this);
      // clear history list - becomes "new" message
      t.msg.historyList.clear();
      Timer timer = (Timer) myService.tasks.get(taskName);
      if (timer != null) {
        // timer = new Timer(String.format("%s.timer", getName()));
        try {
          timer.schedule(t, interval);
        } catch (IllegalStateException e) {
        }
      }
    }
    if (oneShot) {
      myService.purgeTask(taskName);
    }
  }

}
