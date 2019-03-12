package org.myrobotlab.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class ChaosMonkey extends Thread {

  public final static Logger log = LoggerFactory.getLogger(AbstractTest.class);

  static final List<ChaosMonkey> monkeyParty = new ArrayList<>();

  static public void giveToMonkey(Long playTime, Long sleepTime, Object service, String methodName, Object... params) throws Exception {
    ChaosMonkey monkey = new ChaosMonkey(monkeyParty.size(), playTime, sleepTime, service, methodName, params);
    monkeyParty.add(monkey);
  }
  static public void giveToMonkey(Object service, String methodName) throws Exception {
    giveToMonkey(null, null, service, methodName, (Object[]) null);
  }
  static public void giveToMonkey(Object service, String methodName, Object... params) throws Exception {
    giveToMonkey(null, null, service, methodName, params);
  }
                                  static public String monkeyReport() {
    StringBuffer sb = new StringBuffer();
    boolean hasError = false;
    for (int i = 0; i < monkeyParty.size(); ++i) {
      ChaosMonkey monkey = monkeyParty.get(i);
      Exception e = monkey.getReasonOfDeath();
      if (e != null) {
        hasError = true;
      }
      sb.append(monkey.toString() + "\n");
    }

    StringBuffer ret = new StringBuffer();
    if (hasError) {
      ret.append("\n=========== ChaosMonkey Report Begin - Murdered Monkeys ! ===========\n");
      ret.append(sb.toString());
      ret.append("=========== ChaosMonkey Report End - Murdered Monkeys ! ===========\n");
    } else {
      ret.append("\n=========== ChaosMonkey Report Begin - Monkey Party Done ! ===========\n");
      ret.append(sb.toString());
      ret.append("=========== ChaosMonkey Report End - Monkey Party Done ! ===========\n");
    }

    if (hasError) {
      log.error(ret.toString());
    } else {
      log.info(ret.toString());
    }

    return ret.toString();
  }
  static public void startMonkeys() throws InterruptedException {
    startMonkeys(null);
  }
  static public void startMonkeys(Long playTime) throws InterruptedException {
    if (monkeyParty.size() == 0) {
      log.warn("no monkeys in haus!");
      return;
    }
    for (int i = 0; i < monkeyParty.size(); ++i) {
      ChaosMonkey monkey = monkeyParty.get(i);
      monkey.setTimeToLive(playTime);
      monkey.start();
    }

    for (int i = 0; i < monkeyParty.size(); ++i) {
      ChaosMonkey monkey = monkeyParty.get(i);
      monkey.join();
    }
  }
  // seconds
  public long endTs;
  transient Exception exceptionDeath = null;
  public int id;
  public String internalState = "unborn";
  Method method = null;

  Object[] params = null;
  public int pressedButtonCount = 0;
  public Integer randomStartDelayMs = 300;

  public boolean running = false;

  public long sleepTime = 300; // default 1/3 second

  public long startTs;

  public String state = "happy";

  Object testClass = null;

  public long timeToLive = 10000; // default life of a monkey is only 10

  public ChaosMonkey(int id, Long timeToLive, Long sleepTime, Object testClass, String methodName, Object[] params) throws NoSuchMethodException, SecurityException {
    super(String.format("monkey-%d", id));
    this.id = id;
    this.testClass = testClass;
    if (timeToLive != null) {
      this.timeToLive = timeToLive;
    }
    if (sleepTime != null) {
      this.sleepTime = sleepTime;
    }

    Class<?>[] paramTypes = null;
    if (params != null) {
      paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; ++i) {
        if (params[i] != null) {
          paramTypes[i] = params[i].getClass();
        } else {
          paramTypes[i] = null;
        }
      }
    }

    this.method = testClass.getClass().getMethod(methodName, paramTypes);
    this.params = params;
  }

  public Exception getReasonOfDeath() {
    return exceptionDeath;
  }

  public List<Exception> getReasonsOfDeath() {
    List<Exception> reasonsOfDeath = new ArrayList<>();
    for (int i = 0; i < monkeyParty.size(); ++i) {
      reasonsOfDeath.add(monkeyParty.get(i).getReasonOfDeath());
    }
    return reasonsOfDeath;
  }

  public void kill() {
    running = false;
    interrupt();
  }

  public void killMonkeys() {
    for (int i = 0; i < monkeyParty.size(); ++i) {
      monkeyParty.get(i).kill();
    }
    monkeyParty.clear();
  }

  @Override
  public void run() {
    try {
      running = true;
      internalState = "alive";
      startTs = System.currentTimeMillis();
      if (randomStartDelayMs != null) {
        Random r = new Random();
        sleep(r.nextInt(randomStartDelayMs));
      }
      while (running) {
        internalState = "playing";
        // monkey sleeps before causing more problems
        ++pressedButtonCount;
        method.invoke(testClass, params);
        // nap time
        internalState = "napping";
        Thread.sleep(sleepTime);
        if (System.currentTimeMillis() - startTs > timeToLive) {
          running = false;
        }
      }
    } catch (Exception e) {
      internalState = "murdered";
      state = "sad";
      exceptionDeath = e;
      log.error("monkey got hurt !", e);
      return;
    }

    log.info("monkey has died of natural causes");
    internalState = "dead";
  }

  public void setTimeToLive(Long playTime) {
    if (playTime != null) {
      timeToLive = playTime;
    }
  }

  public String toString() {
    String paramstr = CodecUtils.getParameterSignature(params);
    String methodstr = String.format("%s.%s(%s)", testClass.getClass().getSimpleName(), method.getName(), paramstr);
    String exception = (exceptionDeath == null) ? "" : Logging.stackToString(exceptionDeath);
    return String.format("monkey-%d is %s and %s after pressing %s %d times %s", id, state, internalState, methodstr, pressedButtonCount, exception);
  }

}
