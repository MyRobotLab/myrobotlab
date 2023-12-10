/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.HttpData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.HttpDataListener;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinListener;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

/**
 * test catcher is a class to be used to exercise and verify publish, subscribe
 * and other forms of message sending
 * 
 * @author GroG
 *
 */
public class TestCatcher extends Service implements SerialDataListener, HttpDataListener, PinArrayListener, PinListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TestCatcher.class);

  /**
   * data to hold the incoming messages
   */
  transient public BlockingQueue<Message> msgs = new LinkedBlockingQueue<Message>();

  transient public Map<String, Object[]> methodsCalled = new HashMap<>();

  public static class Ball {
    public String name;
    public String type;
    public Integer rating;

    public Ball() {
    }
  }

  boolean isLocal = true;

  public Set<String> onCreated = ConcurrentHashMap.newKeySet();

  public Map<String, Registration> onRegistered = new HashMap<String, Registration>();

  public Set<String> onStarted = ConcurrentHashMap.newKeySet();

  public Set<String> onReleased = ConcurrentHashMap.newKeySet();

  public Set<String> onStopped = ConcurrentHashMap.newKeySet();

  public List<Long> longs = new ArrayList<>();

  public Set<PinData[]> pinSet = ConcurrentHashMap.newKeySet();

  public String[] activePins = null;

  public PinData pinData = null;

  public String pin;

  public BlockingQueue<Integer> integers = new LinkedBlockingDeque<>();

  public BlockingQueue<String> strings = new LinkedBlockingDeque<>();

  public BlockingQueue<Object> objects = new LinkedBlockingQueue<>();

  /**
   * awesome override to simulate remote services - e.g. in
   * Serial.addByteListener
   */
  @Override
  public boolean isLocal() {
    return isLocal;
  }

  public TestCatcher(String n, String id) {
    super(n, id);
  }

  /**
   * some pub/sub interfaces do not use the Message queue to post their data -
   * but use a callback thread from the other service as an optimization onByte
   * is one of those methods
   */
  @Override
  public void onBytes(byte[] b) {
    // NoOp
  }

  /*
   * preProcessHook is used to intercept messages and process or route them
   * before being processed/invoked in the Service.
   * 
   * 
   * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.
   * framework.Message)
   */
  @Override
  public boolean preProcessHook(Message msg) {
    log.info("msg - {}.{}", msg.getName(), msg.method);
    put(msg);
    // TODO - determine if the callback method exists
    // if not warn return false - if so - return true;
    return true;
  }

  private void put(Message msg) {
    try {
      if (log.isDebugEnabled()) {
        log.debug("{} msg {}", msgs.size(), msg);
      }
      msgs.put(msg);
    } catch (Exception e) {

    }
  }

  /**
   * put all recv structures here to clear
   */
  public void clear() {
    msgs.clear();
    pinData = null;
    pinSet.clear();
    methodsCalled.clear();
    longs.clear();
    integers.clear();
    strings.clear();
    objects.clear();
  }

  public Message getMsg(long timeout) throws InterruptedException {
    Message msg = msgs.poll(timeout, TimeUnit.MILLISECONDS);
    return msg;
  }

  public BlockingQueue<Message> waitForMsgs(int count) throws InterruptedException, IOException {
    return waitForMsgs(count, 1000);
  }

  public BlockingQueue<Message> waitForMsgs(int count, int timeout) throws InterruptedException, IOException {
    long start = System.currentTimeMillis();
    int interCount = 0;

    while ((interCount = msgs.size()) < count) {
      if (timeout < System.currentTimeMillis() - start) {
        throw new IOException(String.format("timeout - %d msgs under %d ms expected - got %d in %d ms", interCount, timeout, msgs.size(), System.currentTimeMillis() - start));
      }
      sleep(10);
    }

    log.info("returned {} msgs in {} ms", interCount, System.currentTimeMillis() - start);
    return msgs;
  }

  public ArrayList<?> waitForData(int count) throws InterruptedException, IOException {
    return waitForData(count, 1000, 100);
  }

  public ArrayList<?> waitForData(int count, int timeout) throws InterruptedException, IOException {
    return waitForData(count, timeout, 100);
  }

  public ArrayList<?> waitForData(int count, int timeout, int pollInterval) throws InterruptedException, IOException {
    int msgCount = 0;
    ArrayList<Object> ret = new ArrayList<Object>();
    long start = System.currentTimeMillis();
    long now = start;

    while (msgCount < count) {
      Object msg = msgs.poll(pollInterval, TimeUnit.MILLISECONDS);
      if (msg != null) {
        ret.add(msg);
      }
      now = System.currentTimeMillis();
      if (now - start > timeout) {
        throw new IOException(String.format("waited %d ms received %d messages expecting %d in less than %d ms", now - start, ret.size(), count, timeout));
      }
    }

    log.info("returned %d data in %s ms", ret.size(), now - start);
    return ret;
  }

  public int getMsgCount() {
    return msgs.size();
  }

  @Override
  public void onConnect(String portName) {
    info("connected to %s", portName);
    methodsCalled.put(Thread.currentThread().getStackTrace()[1].getMethodName(), new Object[] { portName });
  }

  @Override
  public void onDisconnect(String portName) {
    info("disconnect to %s", portName);
    methodsCalled.put(Thread.currentThread().getStackTrace()[1].getMethodName(), new Object[] { portName });
  }

  public void checkMsg(String method) throws InterruptedException, IOException {
    checkMsg(1000, method, (Object[]) null);
  }

  public void checkMsg(String method, Object... checkParms) throws InterruptedException, IOException {
    checkMsg(1000, method, checkParms);
  }

  // FIXME - good idea
  public void checkMsg(long timeout, String method, Object... checkParms) throws InterruptedException, IOException {
    Message msg = getMsg(timeout);
    if (msg == null) {
      log.error("{}", msg);
      throw new IOException(String.format("reached timeout of %d waiting for message", timeout));
    }
    if (checkParms != null && checkParms.length != msg.data.length) {
      log.error("{}", msg);
      throw new IOException(String.format("incorrect number of expected parameters - expected %d got %d", checkParms.length, msg.data.length));
    }

    if (checkParms == null && msg.data != null) {
      log.error("{}", msg);
      throw new IOException(String.format("expected null parameters - got non-null"));
    }

    // Never reached since msg.data.length is accessed above and would throw NPE
    // Probably don't need this if we can assume that msg.data is always non-null
    // and may just be empty
    if (checkParms != null && msg.data == null) {
      log.error("{}", msg);
      throw new IOException("expected non null parameters - got null");
    }

    if (!method.equals(msg.method)) {
      log.error("{}", msg);
      throw new IOException(String.format("unlike methods - expected %s got %s", method, msg.method));
    }
    if (checkParms != null) {
      for (int i = 0; i < checkParms.length; ++i) {
        Object expected = checkParms[i];
        Object got = msg.data[i];
        if (!expected.equals(got)) {
          throw new IOException(String.format("unlike methods - expected %s got %s", method, msg.method));
        }
      }
    }

  }

  static public MetaData meta = null;

  public String testMultipleParamTypes(String a, Double b, Integer c) {
    log.info("testMultipleParamTypes {} {} {}", a, b, c);
    return a;
  }

  public double testDouble(double d) {
    return d;
  }

  public double[] testDoubleArray(double[] data) {
    return data;
  }

  public void onTime(Date d) {
    log.info("onDate {}", d);
  }

  public Integer onInteger(Integer data) {
    log.info("onInteger {}", data);
    integers.add(data);
    return data;
  }

  public String onString(String data) {
    log.info("onString {}", data);
    strings.add(data);
    return data;
  }

  public Long onLong(Long data) {
    log.info("onInteger {}", data);
    longs.add(data);
    return data;
  }

  public int onInt(int data) {
    log.info("onInteger {}", data);
    return data;
  }

  public double onDouble(double data) {
    log.info("onDouble {}", data);
    return data;
  }

  public Object onObject(Object data) {
    log.info("onObject {}", data);
    objects.add(data);
    return data;
  }

  public int waitForThis(int data, long sleep) {
    sleep(sleep);
    log.info("waitForThis {}", data);
    return data;
  }

  // @Override
  public void onReady(Integer t01, Double t02, Date d) {
    log.info("onReady {} {} {}", t01, t02, d);
  }

  public void waitFor(String... pubs) {
    for (String publish : pubs) {
      String[] pubParts = publish.split("\\.");
      if (pubParts.length != 2) {
        log.error("waitFor requirement is {publisher}.{topic} but [{}] was given", publish);
        continue;
      }
      String topicName = pubParts[0];
      String topicMethod = pubParts[1];
      // subscribe is A SERVICE METHOD - not useful for non-services however
      // other "things" could have attach or addListener
      subscribe(topicName, topicMethod);
    }
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.DEBUG);

      TestCatcher catcher01 = (TestCatcher) Runtime.start("catcher01", "TestCatcher");
      TestThrower thrower01 = (TestThrower) Runtime.start("thrower01", "TestThrower");
      TestThrower thrower02 = (TestThrower) Runtime.start("thrower02", "TestThrower");
      Clock clock01 = (Clock) Runtime.start("clock01", "Clock");
      Runtime.start("gui", "SwingGui");

      // core implementation with strings subscriptions - works over remote
      // waitFor(String...subscribers)
      // if a default publish exists ...
      // catcher01.waitForDefaults("thrower01","thrower02");
      //
      // waitForEach waitforAll waitForAny
      // catcher01.waitFor("thrower01", "publishInteger", "thrower02",
      // "publishDouble");
      catcher01.waitFor("thrower01.publishInteger", "thrower02.publishDouble", "clock01.publishTime");
      // scan for key callbacks (sources & methods) and resolve in framework -
      // to be delivered

      clock01.startClock();
      thrower01.invoke("publishInteger", 7);
      thrower02.invoke("publishInteger", 5.0);

      // optimized implementation with local reference - and possibly direct
      // callbacks (perhaps not)
      // waitFor(Subscriber...subscribers)
      // catcher01.waitFor(thrower01, thrower02);

      // catcher01.waitForAny(thrower01, thrower02);

      /*
       * TestThrower thrower = new TestThrower("thrower");
       * thrower.startService();
       * 
       * catcher01.subscribe(thrower.getName(), "throwInteger",
       * catcher01.getName(), "catchInteger");
       * 
       * for (int i = 0; i < 1000; ++i) { thrower.invoke("throwInteger", i); if
       * (i % 100 == 0) { thrower.sendBlocking(catcher01.getName(),
       * "catchInteger"); } }
       */

      // thrower.throwInteger(count);

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  public String getPin(String label, String label2) {
    return null;
  }

  // ordinal collision test
  public String getPin(String label) {
    return null;
  }

  public Integer getPin(Integer address) {
    return address;
  }

  public Integer[] getPin(Integer[] address) {
    return address;
  }

  public int[] getPin(int[] address) {
    return address;
  }

  public int getPin(int address) {
    return address;
  }

  public int primitiveOnlyMethod(int x) {
    return x;
  }

  public int invokeTest(int p0) {
    log.info("invokeTest(int)");
    return p0;
  }

  public String invokeTest(String p0) {
    log.info("invokeTest(String)");
    return p0;
  }

  public Boolean invokeTest(Boolean p0) {
    log.info("invokeTest(Boolean)");
    return p0;
  }

  public SerialDataListener invokeTest(SerialDataListener p0) {
    log.info("invokeTest(SerialDataListener)");
    return p0;
  }

  public HttpDataListener invokeTest(HttpDataListener p0) {
    log.info("invokeTest(HttpDataListener)");
    return p0;
  }

  public TestCatcher invokeTestCatcher(TestCatcher p0) {
    return p0;
  }

  @Override
  public void onHttpData(HttpData data) {
    // TODO Auto-generated method stub

  }

  public void onPitch(Integer i) {
    log.info("onPitch({})", i);
  }

  public Ball catchBall(Ball ball) {
    return ball;
  }

  public String catchBall(String ball) {
    return ball;
  }

  public int catchBall(int ball) {
    return ball;
  }

  public Integer catchBall(Integer ball) {
    return ball;
  }

  public Double catchBall(Double ball) {
    return ball;
  }

  public String onUptime(String str) {
    return str;
  }

  public void onCreated(String serviceName) {
    onCreated.add(serviceName);
  }

  public void onRegistered(Registration registration) {
    if (onRegistered != null) {
      onRegistered.put(registration.getFullName(), registration);
    }
  }

  public void onStarted(String serviceName) {
    String info = String.format("notified --> %s  %s has started", getName(), serviceName);
    log.info(info);
    onStarted.add(serviceName);
  }

  public void onStopped(String serviceName) {
    onStopped.add(serviceName);
  }

  public void onReleased(String serviceName) {
    onReleased.add(serviceName);
  }

  @Override
  public void onPinArray(PinData[] pindata) {
    log.info("onPinArray {}", pinData);
    pinSet.add(pindata);
  }

  public void setActivePins(String[] activePins) {
    this.activePins = activePins;
  }

  @Override
  public String[] getActivePins() {
    return activePins;
  }

  @Override
  public void onPin(PinData pinData) {
    log.info("onPin {}", pinData);
    this.pinData = pinData;
  }

  @Override
  public void setPin(String pin) {
    this.pin = pin;
  }

  @Override
  public String getPin() {
    return pin;
  }

  public boolean containsPinArrayFromPin(String pin) {
    for (PinData[] pa : pinSet) {
      for (PinData pd : pa) {
        if (pin.equals(pd.pin)) {
          return true;
        }
      }
    }
    return false;
  }

  public void verifyCallback(String method, Object... params) throws IOException {
    if (!methodsCalled.containsKey(method)) {
      throw new IOException(String.format("callback %s not found", method));
    }
    Object[] recvdParams = methodsCalled.get(method);
    if (recvdParams.length != params.length) {
      throw new IOException(String.format("parameter misalignment for method %s - expecting %d got %d", method, params.length, recvdParams.length));
    }
    for (int i = 0; i < params.length; ++i) {
      Object verify = params[i];
      Object recvd = params[i];
      if (verify == null && recvd != null) {
        throw new IOException(String.format("parameter invalid for method %s - expecting null got %s", method, recvd));
      }

      if (!verify.equals(recvd)) {
        throw new IOException(String.format("parameter incorrect for method %s - expecting %s got %s", method, verify, recvd));
      }
    }
  }

}
