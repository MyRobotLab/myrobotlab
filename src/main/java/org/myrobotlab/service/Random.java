package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.RandomConfig;
import org.myrobotlab.service.config.RandomConfig.RandomMessageConfig;
import org.slf4j.Logger;

/**
 * A service for random events to add interest to bots
 * 
 * @author GroG
 *
 */
public class Random extends Service<RandomConfig> {

  private static final long serialVersionUID = 1L;

  protected final static Logger log = LoggerFactory.getLogger(Random.class);

  transient private RandomProcessor processor = null;

  transient private final Object lock = new Object();

  /**
   * 
   * RandomMessage is used to contain the ranges of values and intervals for
   * which random messages will be sent
   *
   */
  static public class RandomMessage {
    public String taskName;
    public String name;
    public String method;
    public Range[] data;
    public boolean enabled = true;
    public long minIntervalMs;
    public long maxIntervalMs;
    public boolean oneShot = false;
    public transient long nextProcessTimeTs = 0;

    public RandomMessage() {
    }
  }

  static public class Range {
    public Object min;
    public Object max;
    public List<Object> set = null;

    public Range() {
    }

    public Range(Object min, Object max) {
      this.min = min;
      this.max = max;
    }

  }

  /**
   * all random message data is located here
   */
  protected Map<String, RandomMessage> randomData = new HashMap<>();

  /**
   * Java's random value generator
   */
  transient private java.util.Random random = new java.util.Random();

  /**
   * global enable will determines if "all" random messages will be enabled or
   * disabled without changing their individual enable/disable state
   */
  protected boolean enabled = true;

  public Random(String n, String id) {
    super(n, id);
  }

  public void setSeed(long seed) {
    random = new java.util.Random(seed);
  }

  public int getRandom(int min, int max) {
    return random.nextInt((max - min) + 1) + min;
  }

  public long getRandom(long min, long max) {
    return min + (long) (Math.random() * (max - min));
  }

  public double getRandom(double min, double max) {
    return min + (Math.random() * (max - min));
  }

  public RandomMessage getTask(String taskName) {
    return randomData.get(taskName);
  }

  public void addRandom(String taskName, long minIntervalMs, long maxIntervalMs, String name, String method, Integer... values) {
    addRandom(taskName, minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) values));
  }

  public void addRandom(String taskName, long minIntervalMs, long maxIntervalMs, String name, String method, Double... values) {
    addRandom(taskName, minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) values));
  }

  // FIXME - test this
  public void addRandom(String taskName, long minIntervalMs, long maxIntervalMs, String name, String method) {
    addRandom(taskName, minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) null));
  }

  public void addRandom(String taskName, long minIntervalMs, long maxIntervalMs, String name, String method, String... params) {
    addRandom(taskName, minIntervalMs, maxIntervalMs, name, method, setRange((Object[]) params));
  }

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method, Integer... values) {
    addRandom(minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) values));
  }

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method, Double... values) {
    addRandom(minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) values));
  }

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method) {
    addRandom(minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) null));
  }
  
  /**
   * Global check for enabled
   * @return
   */
  public boolean isEnabled() {
    return enabled;
  }

  public Range intRange(int min, int max) {
    return new Range(min, max);
  }

  public Range doubleRange(double min, double max) {
    return new Range(min, max);
  }

  public Range setRange(Object... strings) {
    Range range = new Range();
    range.set = Arrays.asList(strings);
    return range;
  }

  public Range longRange(long min, long max) {
    return new Range(min, max);
  }

  public Range floatRange(float min, float max) {
    return new Range(min, max);
  }

  public Range[] toRanges(Object... values) {
    if (values == null) {
      return null;
    }
    if (values.length % 2 != 0) {
      error("invalid addRandom ranges must be sets of min/max");
      return null;
    }
    // Range[] ranges = new Range[values.length/2];
    List<Range> ranges = new ArrayList<>();

    if (values != null && values.length > 0) {
      for (int i = 0; i < values.length; i += 2) {
        Range range = new Range();
        range.min = values[i];
        range.max = values[i + 1];
        ranges.add(range);
      }
    }

    Range[] r = new Range[ranges.size()];
    return ranges.toArray(r);

  }

  /**
   * remove all random events
   */
  public void removeAll() {
    purge();
  }

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method, Range... ranges) {
    String taskName = String.format("%s.%s", name, method);
    addRandom(taskName, minIntervalMs, maxIntervalMs, name, method, ranges);
  }

  public void addRandom(String taskName, long minIntervalMs, long maxIntervalMs, String name, String method, Range... ranges) {

    RandomMessage data = new RandomMessage();
    data.name = name;
    data.method = method;
    data.minIntervalMs = minIntervalMs;
    data.maxIntervalMs = maxIntervalMs;
    data.data = ranges;
    data.enabled = true;

    synchronized (lock) {
      randomData.put(taskName, data);
    }

    log.info("add random message {} in {} to {} ms", taskName, data.minIntervalMs, data.maxIntervalMs);
    broadcastState();
  }

  private class RandomProcessor extends Thread {

    public RandomProcessor(String name) {
      super(name);
    }

    public void run() {
      while (enabled) {
        try {
          // minimal interval time for processor to check
          // and see if any random event needs processing

          sleep(config.rate);
          
          Map<String, RandomMessage> tasks = null;
          synchronized (lock) {

            // copy to avoid concurrent exceptions, avoid iterating over
            // randomData
            tasks = new HashMap<>();
            Set<String> keySet = new HashSet<String>(randomData.keySet());
            for (String k : keySet) {
              RandomMessage rm = randomData.get(k);
              if (rm != null) {
                tasks.put(k, rm);
              }
            }
          }

          for (String key : tasks.keySet()) {

            long now = System.currentTimeMillis();

            RandomMessage randomEntry = tasks.get(key);
            if (!randomEntry.enabled) {
              continue;
            }

            // first time set
            if (randomEntry.nextProcessTimeTs == 0) {
              randomEntry.nextProcessTimeTs = now + getRandom((Long) randomEntry.minIntervalMs, (Long) randomEntry.maxIntervalMs);
            }

            if (now < randomEntry.nextProcessTimeTs) {
              // this entry isn't ready
              continue;
            }

            Message m = Message.createMessage(getName(), randomEntry.name, randomEntry.method, null);
            if (randomEntry.data != null) {
              List<Object> data = new ArrayList<>();

              for (int i = 0; i < randomEntry.data.length; ++i) {
                Object o = randomEntry.data[i];
                if (o instanceof Range) {
                  Range range = (Range) o;
                  Object param = null;

                  if (range.set != null) {
                    int rand = getRandom(0, range.set.size() - 1);
                    param = range.set.get(rand);
                  } else if (range.min instanceof Double) {
                    param = getRandom((Double) range.min, (Double) range.max);
                  } else if (range.min instanceof Long) {
                    param = getRandom((Long) range.min, (Long) range.max);
                  } else if (range.min instanceof Integer) {
                    param = getRandom((Integer) range.min, (Integer) range.max);
                  }

                  data.add(param);
                }
              }
              m.data = data.toArray();
            }
            m.sendingMethod = "process";
            log.debug("random msg @ {} ms {}", now - randomEntry.nextProcessTimeTs, m);
            out(m);

            // auto-disable oneshot
            if (randomEntry.oneShot) {
              randomEntry.enabled = false;
            }

            // reset next processing time
            randomEntry.nextProcessTimeTs = now + getRandom((Long) randomEntry.minIntervalMs, (Long) randomEntry.maxIntervalMs);

          }

        } catch (Exception e) {
          error(e);
        }

      } // while (enabled) {

      log.info("Random {}-processor terminating", getName());
    }
  }

  @Override
  public RandomConfig getConfig() {
    super.getConfig();

    config.enabled = enabled;

    if (config.randomMessages == null) {
      config.randomMessages = new HashMap<>();
    }

    for (String key : randomData.keySet()) {
      RandomMessage msg = randomData.get(key);
      RandomMessageConfig m = new RandomMessageConfig();
      m.service = msg.name;
      m.method = msg.method;
      m.maxIntervalMs = msg.maxIntervalMs;
      m.minIntervalMs = msg.minIntervalMs;
      m.data = msg.data;
      m.enabled = msg.enabled;
      config.randomMessages.put(key, m);
    }

    return config;
  }

  @Override
  public RandomConfig apply(RandomConfig c) {
    super.apply(c);
    if (c.enabled) {
      enable();
    } else {
      disable();
    }

    try {
      for (String key : c.randomMessages.keySet()) {
        RandomMessageConfig msgc = c.randomMessages.get(key);
        addRandom(key, msgc.minIntervalMs, msgc.maxIntervalMs, msgc.service, msgc.method, msgc.data);
        if (!msgc.enabled) {
          disable(key);
        }
      }
    } catch (Exception e) {
      error(e);
    }

    return c;
  }

  @Deprecated /* use remove(String key) */
  public RandomMessage remove(String name, String method) {
    return remove(String.format("%s.%s", name, method));
  }

  public RandomMessage remove(String key) {
    if (!randomData.containsKey(key)) {
      error("key %s does not exist");
    }
    return randomData.remove(key);
  }

  public Set<String> getKeySet() {
    return randomData.keySet();
  }

  public void disable(String key) {

    if (!randomData.containsKey(key)) {
      error("disable cannot find key %s", key);
      return;
    }

    randomData.get(key).enabled = false;
  }

  public void enable(String key) {
    if (!randomData.containsKey(key)) {
      error("disable cannot find key %s", key);
      return;
    }
    randomData.get(key).enabled = true;
  }

  public void disable() {
    synchronized (lock) {
      enabled = false;
      processor = null;
      broadcastState();
    }
  }

  public void enable() {
    synchronized (lock) {
      enabled = true;
      if (processor == null) {
        processor = new RandomProcessor(String.format("%s-processor", getName()));
        processor.start();
        // wait until thread starts
        sleep(200);
      } else {
        info("%s already enabled");
      }
      broadcastState();
    }
  }

  public void purge() {
    randomData.clear();
    broadcastState();
  }

  public Set<String> getMethodsFromName(String serviceName) {
    ServiceInterface si = Runtime.getService(serviceName);
    if (si == null) {
      return new HashSet<String>();
    }

    // FIXME FIXME FIXME - add filtering capability at the MethodCache

    return MethodCache.getInstance().getMethodNames(si.getClass().getCanonicalName());
  }

  public List<String> getServiceList() {
    List<String> ret = new ArrayList<String>();
    for (String name : Runtime.getServiceNames()) {
      ret.add(name);
    }
    return ret;
  }

  public List<MethodEntry> methodQuery(String serviceName, String methodName) {
    ServiceInterface si = Runtime.getService(serviceName);
    if (si == null) {
      return new ArrayList<MethodEntry>();
    }
    return MethodCache.getInstance().query(si.getClass().getCanonicalName(), methodName);
  }

  public Map<String, RandomMessage> getRandomEvents() {
    return randomData;
  }

  public RandomMessage getRandomEvent(String key) {
    return randomData.get(key);
  }

  /**
   * disables all the individual tasks
   */
  public void disableAll() {
    for (RandomMessage data : randomData.values()) {
      data.enabled = false;
    }
    broadcastState();
  }

  @Override
  public void releaseService() {
    disable();
    super.releaseService();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Runtime.setConfig("dev");
      Runtime.start("c1", "Clock");
      Runtime.start("python", "Python");

      Random random = (Random) Runtime.start("random", "Random");

      List<String> ret = random.getServiceList();
      Set<String> mi = random.getMethodsFromName("c1");
      List<MethodEntry> mes = MethodCache.getInstance().query("Clock", "setInterval");
      random.disable();
      random.addRandom(200, 1000, "i01", "setHeadSpeed", 8, 20, 8, 20, 8, 20);
      random.addRandom(200, 1000, "i01", "moveHead", 65, 115, 65, 115, 65, 115);
      random.enable();

      // Python python = (Python) Runtime.start("python", "Python");

      // random.addRandom(3000, 8000, "i01", "setLeftHandSpeed", 8, 25, 8, 25,
      // 8, 25, 8, 25, 8, 25, 8, 25);
      // random.addRandom(3000, 8000, "i01", "setRightHandSpeed", 8, 25, 8, 25,
      // 8, 25, 8, 25, 8, 25, 8, 25);

      // random.addRandom(200, 1000, "i01", "moveHead", 65, 115, 65, 115, 65,
      // 115);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
