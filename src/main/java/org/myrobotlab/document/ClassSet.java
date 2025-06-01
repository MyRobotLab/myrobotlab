package org.myrobotlab.document;

import java.util.ArrayList;
import java.util.List;

public class ClassSet {

  long newestTs = 0;
  long oldestTs = 0;
  // getTimeDelta

  int countLimit = 20;
  int totalCount = 0;
  Long purgeDeltaTimeMs = null; // NOT NEEDED if done on the query side for
  // count
  float totalConfidence = 0;

  List<Classification> timeline = new ArrayList<>();

  public ClassSet() {
  }

  public void add(Classification object) {
    ++totalCount;
    newestTs = object.getTs();
    timeline.add(object);

    // trim if needed
    if (timeline.size() > countLimit) {
      timeline.remove(timeline.size() - 1);
    }

    // get "new" oldest
    oldestTs = timeline.get(timeline.size() - 1).getTs();

    // delta time can be calculated here
  }

  public long getTimeSinceMs() {
    return System.currentTimeMillis() - newestTs;
  }

}
