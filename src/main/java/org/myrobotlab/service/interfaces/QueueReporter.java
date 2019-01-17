package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.QueueStats;

public interface QueueReporter {

  public QueueStats publishStats(QueueStats stats);

  public void updateStats(QueueStats stats);
}
