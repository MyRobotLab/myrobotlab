package org.myrobotlab.framework;

import java.util.TimerTask;

public class Task extends TimerTask {

  Message msg;
  int interval = 0;
  Service myService;

  public Task(int interval, String name, String method) {
    this(interval, name, method, (Object[]) null);
  }

  public Task(int interval, String name, String method, Object... data) {
    this.msg = myService.createMessage(name, method, data);
    this.interval = interval;
  }

  public Task(Service myService, String toService, String method, Object... params) {
    this.myService = myService;
    msg = myService.createMessage(toService, method, params);
  }

  public Task(Task s) {
    this.msg = s.msg;
    this.interval = s.interval;
  }

  @Override
  public void run() {

    myService.getInbox().add(msg);

    if (interval > 0) {
      Task t = new Task(this);
      // clear history list - becomes "new" message
      t.msg.historyList.clear();
      myService.timer.schedule(t, interval);
    }
  }

}
