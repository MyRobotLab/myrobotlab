package org.myrobotlab.service.interfaces;

import java.util.concurrent.BlockingQueue;

public interface QueueSource {
  String getName();

  BlockingQueue<?> getQueue();
}
