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
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;

/**
 * A service for random events to add interest to bots
 * 
 * @author GroG
 *
 */
public class Random extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Random.class);

  /**
   * 
   * RandomMessage is used to contain the ranges of values and intervals for
   * which random messages will be sent
   *
   */
  public class RandomMessage {
    public String name;
    public String method;
    public Range[] data;
    public boolean enabled = true;
    public long minIntervalMs;
    public long maxIntervalMs;
    public long interval;
    public boolean oneShot = false;
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
  Map<String, RandomMessage> randomData = new HashMap<>();

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

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method, Integer... values) {
    addRandom(minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) values));
  }

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method, Double... values) {
    addRandom(minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) values));
  }

  public void addRandom(long minIntervalMs, long maxIntervalMs, String name, String method) {
    addRandom(minIntervalMs, maxIntervalMs, name, method, toRanges((Object[]) null));
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

    RandomMessage msg = new RandomMessage();
    msg.name = name;
    msg.method = method;
    msg.minIntervalMs = minIntervalMs;
    msg.maxIntervalMs = maxIntervalMs;
    msg.data = ranges;

    String key = String.format("%s.%s", name, method);
    randomData.put(key, msg);

    msg.interval = getRandom(minIntervalMs, maxIntervalMs);
    log.info("add random message {} in {} ms", key, msg.interval);
    addTask(key, 0, msg.interval, "process", key);
    broadcastState();
  }

  public void process(String key) {
    if (!enabled) {
      return;
    }

    RandomMessage msg = randomData.get(key);
    if (msg == null || !msg.enabled) {
      return;
    }

    Message m = Message.createMessage(getName(), msg.name, msg.method, null);
    if (msg.data != null) {
      List<Object> data = new ArrayList<>();

      for (int i = 0; i < msg.data.length; ++i) {
        Object o = msg.data[i];
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
    log.debug("random msg @ {} ms {}", msg.interval, m);
    out(m);

    purgeTask(key);
    if (!msg.oneShot) {
      msg.interval = getRandom(msg.minIntervalMs, msg.maxIntervalMs);
      addTask(key, 0, msg.interval, "process", key);
    }
  }

  @Override
  public ServiceConfig getConfig() {

    RandomConfig config = new RandomConfig();

    config.enabled = enabled;

    for (String key : randomData.keySet()) {
      RandomMessage msg = randomData.get(key);
      RandomMessageConfig m = new RandomMessageConfig();
      m.maxIntervalMs = msg.maxIntervalMs;
      m.minIntervalMs = msg.minIntervalMs;
      m.data = msg.data;
      config.randomMessages.put(key, m);
    }

    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    RandomConfig config = (RandomConfig) c;
    enabled = config.enabled;

    try {
      for (String key : config.randomMessages.keySet()) {
        RandomMessageConfig msgc = config.randomMessages.get(key);
        addRandom(msgc.minIntervalMs, msgc.maxIntervalMs, key.substring(0, key.lastIndexOf(".")), key.substring(key.lastIndexOf(".") + 1), msgc.data);
      }
    } catch (Exception e) {
      error(e);
    }

    return c;
  }

  public RandomMessage remove(String name, String method) {
    return remove(String.format("%s.%s", name, method));
  }
  
  public RandomMessage remove(String key) {
    purgeTask(key);
    return randomData.remove(key);
  }

  public Set<String> getKeySet() {
    return randomData.keySet();
  }

  public void disable(String key) {
    // exact match
    if (key.contains(".")) {
      RandomMessage msg = randomData.get(key);
      if (msg == null) {
        log.warn("cannot disable random event with key {}", key);
        return;
      }
      randomData.get(key).enabled = false;
      purgeTask(key);
      return;
    }
    // must be name - disable "all" for this service
    for (RandomMessage msg : randomData.values()) {
      if (msg.name.equals(key)) {
        msg.enabled = false;
        purgeTask(String.format("%s.%s", msg.name, msg.method));
      }
    }
  }

  public void enable(String key) {
    // exact match
    if (key.contains(".")) {
      RandomMessage msg = randomData.get(key);
      if (msg == null) {
        log.warn("cannot enable random event with key {}", key);
        return;
      }
      randomData.get(key).enabled = true;
      addTask(key, 0, msg.interval, "process", key);
      return;
    }
    // must be name - disable "all" for this service
    String name = key;
    for (RandomMessage msg : randomData.values()) {
      if (msg.name.equals(name)) {
        msg.enabled = true;
        String fullKey = String.format("%s.%s", msg.name, msg.method);
        addTask(fullKey, 0, msg.interval, "process", fullKey);
      }
    }
  }

  public void disable() {
    // remove all timed attempts of processing random
    // events
    purgeTasks();
  }

  public void enable() {
    for (RandomMessage msg : randomData.values()) {
      // re-enable tasks which were previously enabled
      if (msg.enabled == true) {
        String fullKey = String.format("%s.%s", msg.name, msg.method);
        addTask(fullKey, 0, msg.interval, "process", fullKey);
      }
    }
  }

  public void purge() {
    randomData.clear();
    purgeTasks();
  }
  
  public Set<String> getMethodsFromName(String serviceName){
    ServiceInterface si = Runtime.getService(serviceName);    
    if (si == null) {
      return new HashSet<String>();
    }
    
    // FIXME FIXME FIXME - add filtering capability at the MethodCache

    return MethodCache.getInstance().getMethodNames(si.getClass().getCanonicalName());
  }
  
  public List<String> getServiceList(){
    List<String> ret = new ArrayList<String>();
    for (String name: Runtime.getServiceNames()) {
      ret.add(name);
    }
    return ret;
  }
  
  public List<MethodEntry> methodQuery(String serviceName, String methodName){
    ServiceInterface si = Runtime.getService(serviceName);    
    if (si == null) {
      return new ArrayList<MethodEntry>();
    }
    return MethodCache.getInstance().query(si.getClass().getCanonicalName(), methodName);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("c1", "Clock");

      Random random = (Random) Runtime.start("random", "Random");
      
      List<String> ret = random.getServiceList();
      Set<String> mi = random.getMethodsFromName("c1");
      List<MethodEntry> mes = MethodCache.getInstance().query("Clock", "setInterval");
      
      random.addRandom(200, 1000, "i01", "setHeadSpeed", 8, 20, 8, 20, 8, 20);
      random.addRandom(200, 1000, "i01", "moveHead", 65, 115, 65, 115, 65, 115);

      // Python python = (Python) Runtime.start("python", "Python");

      // random.addRandom(3000, 8000, "i01", "setLeftHandSpeed", 8, 25, 8, 25, 8, 25, 8, 25, 8, 25, 8, 25);
      // random.addRandom(3000, 8000, "i01", "setRightHandSpeed", 8, 25, 8, 25, 8, 25, 8, 25, 8, 25, 8, 25);

      // random.addRandom(200, 1000, "i01", "moveHead", 65, 115, 65, 115, 65, 115);

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
