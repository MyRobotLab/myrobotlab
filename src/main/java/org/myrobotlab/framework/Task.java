package org.myrobotlab.framework;

import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.interfaces.ServiceInterface;

public class Task extends TimerTask {

  protected String taskName;
  protected Message msg;
  protected long interval = 0;
  protected transient ServiceInterface si;

  public Task(ServiceInterface myService, String taskName, long interval, Message msg) {
    this.si = myService;
    this.taskName = taskName;
    this.interval = interval;
    this.msg = msg;
  }

  public Task(Task s) {
    this.msg = s.msg;
    this.interval = s.interval;
    this.taskName = s.taskName;
    this.si = s.si;
  }

  @Override
  public void run() {
    if (si != null) {
      si.invoke(msg);
    }

    if (interval > 0) {
      Task t = new Task(this);
      // clear history list - becomes "new" message
      t.msg.historyList.clear();
      Timer timer = si.getTasks().get(taskName);
      if (timer != null) {
        // timer = new Timer(String.format("%s.timer", getName()));
        try {
          timer.schedule(t, interval);
        } catch (IllegalStateException e) {
        }
      }
    }
    // if one shot - remove after completion
    if (interval == 0) {
      si.purgeTask(taskName);
    }
  }

}
