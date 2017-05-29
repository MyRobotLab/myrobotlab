package org.myrobotlab.framework;

import java.util.Timer;
import java.util.TimerTask;

public class Task extends TimerTask {

  String taskName;
  Message msg;
  int interval = 0;
  Service myService;

  // FIXME upgrade to ScheduledExecutorService
  // http://howtodoinjava.com/2015/03/25/task-scheduling-with-executors-scheduledthreadpoolexecutor-example/
  
  public Task(Service myService, String taskName, int interval, Message msg) {
    this.myService = myService;
    this.taskName = taskName;
    this.interval = interval;
    this.msg = msg;
  }
 
  public Task(Task s) {
    this.msg = s.msg;
    this.interval = s.interval;
    this.taskName = s.taskName;
    this.myService = s.myService;
  }

  @Override
  public void run() {
    // info("task %s running - next run %s", taskName,
    // MathUtils.msToString(interval));
    myService.getInbox().add(msg);

    if (interval > 0) {
      Task t = new Task(this);
      // clear history list - becomes "new" message
      t.msg.historyList.clear();
      Timer timer = myService.tasks.get(taskName);
      if (timer != null) {
        // timer = new Timer(String.format("%s.timer", getName()));
        try {
          timer.schedule(t, interval);
        } catch (IllegalStateException e) {
        }
      }
    }
  }

}
