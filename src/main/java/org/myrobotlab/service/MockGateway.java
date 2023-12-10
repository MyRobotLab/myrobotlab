package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.TimeoutException;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.generics.SlidingWindowList;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.config.MockGatewayConfig;
import org.myrobotlab.service.interfaces.Gateway;
import org.slf4j.Logger;

public class MockGateway extends Service<MockGatewayConfig> implements Gateway {

  transient public final static Logger log = LoggerFactory.getLogger(MockGateway.class);

  private static final long serialVersionUID = 1L;

  protected final transient BlockingQueue<Message> sendQueuex = new LinkedBlockingQueue<>();

  protected SlidingWindowList<Message> msgs = new SlidingWindowList<>(100);

  transient protected Map<String, BlockingQueue<Message>> sendQueues = new HashMap<>();

  /**
   * Id of a fake remote instance
   */
  protected String remoteId = "mockId";

  /**
   * list of remote services that have registered
   */
  protected Map<String, RemoteService> remoteServices = new TreeMap<>();

  public class RemoteService {
    protected transient MockGateway gateway;
    protected String name;

    public RemoteService(MockGateway gateway, String fullname) {
      this.name = fullname;
      this.gateway = gateway;
    }

    public void handle(Message msg) {
      if (msg.method.equals("getMethodMap")) {
        Map<String, MethodEntry> emptyMap = new HashMap<>();
        // emptyMap = Runtime.getMethodMap("clock");
        Message returnMsg = Message.createMessage(name, msg.sender, "onMethodMap", emptyMap);
        gateway.send(returnMsg);
      }

    }
  }

  @Override
  public void send(Message msg) {
    super.send(msg);
    invoke("publishMessageEvent", msg);
    msgs.add(msg);
  }

  public MockGateway(String reservedKey, String inId) {
    super(reservedKey, inId);
  }

  @Override
  public void connect(String uri) throws Exception {
    log.info("connecting {}", uri);
  }

  @Override
  public List<String> getClientIds() {
    return Runtime.getInstance().getConnectionUuids(getName());
  }

  @Override
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  /**
   * Connect a remote instance identified by remote id
   * 
   * @param id
   */
  public void addConnection(String id) {
    String uuid = UUID.randomUUID().toString();
    Connection connection = new Connection(uuid, id, getName());
    Runtime.getInstance().addConnection(uuid, id, connection);
  }

  /**
   * Messages are sent to remote services if they are published and routed
   */
  @Override
  public void sendRemote(Message msg) throws Exception {
    log.info("mock gateway got a sendRemote {}", msg);

    String key = String.format("%s.%s", msg.name, msg.method);

    BlockingQueue<Message> q = null;
    if (!sendQueues.containsKey(key)) {
      q = new LinkedBlockingQueue<>();
      sendQueues.put(key, q);
    } else {
      q = sendQueues.get(key);
    }

    q.add(msg);
    invoke("publishMessageEvent", msg);
    msgs.add(msg);

    // verify the msg can be serialized
    String json = CodecUtils.toJson(msg);
    log.debug("sendRemote {}", json);

    if (!remoteServices.containsKey(msg.name)) {
      error("got remote message from %s - and do not have its client !!!", msg.name);
      return;
    }
    remoteServices.get(msg.name).handle(msg);
  }

  public void clear() {
    sendQueues.clear();
  }

  public int size() {
    return msgs.size();
  }

  @Override
  public boolean isLocal(Message msg) {
    return Runtime.getInstance().isLocal(msg);
  }

  public void sendWithDelay(String name, String method, Object... data) {
    Message msg = Message.createMessage(method, name, method, data);
    addTask(UUID.randomUUID().toString(), true, 0, 0, "send", new Object[] { msg });
  }

  /**
   * Send an asynchronous message so waiting for a callback can be done easily
   * with inline code e.g.
   * 
   * <pre>
   * mock.sendWithDelay(10, "mouth", "speakBlocking");
   * mock.waitForMsg(100, "mocker", "publishSpeaking");
   * 
   * </pre>
   * 
   * @param wait
   * @param name
   * @param method
   */
  public void sendWithDelay(long wait, String name, String method) {
    sendWithDelay(wait, name, method, (Object[]) null);
  }

  public void sendWithDelay(long wait, String name, String method, Object... data) {
    Message msg = Message.createMessage(method, name, method, data);
    addTask(UUID.randomUUID().toString(), true, wait, wait, "send", new Object[] { msg });
  }

  // FIXME - must have a radix of names and block on specific publishing methods
  public Message waitForMsg(String name, String callback, long maxTimeWaitMs) throws TimeoutException {
    try {
      String fullName = getFullRemoteName(name);

      String key = String.format("%s.%s", fullName, callback);

      if (!sendQueues.containsKey(key)) {
        sendQueues.put(key, new LinkedBlockingQueue<>());
      }

      Message msg = sendQueues.get(key).poll(maxTimeWaitMs, TimeUnit.MILLISECONDS);
      if (msg == null) {
        String timeout = String.format("waited %dms for %s.%s", maxTimeWaitMs, name, callback);
        throw new TimeoutException(timeout);
      } else {
        return msg;
      }
    } catch (InterruptedException e) {
      log.info("releasing polling thread {}", Thread.currentThread().getId());
    }
    return null;
  }

  /**
   * set the current remote id
   * 
   * @param id
   */
  public void setRemoteId(String id) {
    remoteId = id;
  }

  /**
   * get the current remote id
   * 
   * @return
   */
  public String getRemoteId() {
    return remoteId;
  }

  @Override
  public void startService() {
    super.startService();
    // add the remote instance over a connection
    addConnection(remoteId);
    // add a default remote service
    ArrayList<String> interfaces = new ArrayList<>();
    interfaces.add(ServiceInterface.class.getName());
    registerRemoteService("mocker", interfaces);

  }

  /**
   * Registers a non Java service with mrl runtime, so it can be added to
   * listeners and verified in testing
   * 
   * @param remoteServiceName
   */
  public void registerRemoteService(String remoteServiceName) {
    registerRemoteService(remoteServiceName, null);
  }

  public String getFullRemoteName(String name) {
    if (!name.contains("@")) {
      return String.format("%s@%s", name, remoteId);
    } else {
      return name;
    }
  }

  /**
   * Registers a non Java service with mrl runtime, so it can be added to
   * listeners and verified in testing
   * 
   * @param remoteServiceName
   * @param interfaces
   */
  public void registerRemoteService(String remoteServiceName, ArrayList<String> interfaces) {
    String fullName = getFullRemoteName(remoteServiceName);
    remoteServices.put(fullName, new RemoteService(this, fullName));

    // Runtime.register(remoteId, remoteServiceName, "mock:mock", interfaces);
    Runtime.register(remoteId, remoteServiceName, "Unknown", interfaces);

  }

  /**
   * Sends an asynchronous message with a slight delay so that testing for a
   * callback publish can be done inline
   * 
   * @param name
   * @param method
   */
  public void sendWithDelay(String name, String method) {
    sendWithDelay(0, name, method);
  }

  //
  public String onToString() {
    return toString();
  }

  public Message publishMessageEvent(Message msg) {
    return msg;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.setLevel("WARN");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      // starts a mocking gateway with default id instance
      MockGateway gateway = (MockGateway) Runtime.start("gateway", "MockGateway");

      Clock clock = (Clock) Runtime.start("clock", "Clock");

      clock.addListener("publishTime", "mocker");

      // mocker.send("clock", "startClock");
      // first click is after 1 second

      // mocker.send("clock", "StartClock");
      // gateway.sendWithDelay("clock", "startClock");
      // gateway.sendWithDelay(0, "clock", "startClock");
      Message msg = gateway.waitForMsg("mocker@mockid", "onTime", 1100);
      log.info("message {}", msg);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Message getMsg(String name, String callback) {
    String fullName = getFullRemoteName(name);

    String key = String.format("%s.%s", fullName, callback);
    if (!sendQueues.containsKey(key)) {
      return null;
    }

    try {
      Message msg = sendQueues.get(key).poll(0, TimeUnit.MILLISECONDS);
      return msg;
    } catch (InterruptedException e) {
      log.info("interrupted");
    }
    return null;
  }

  public Integer size(String name, String callback) {
    String fullName = getFullRemoteName(name);
    
    String key = String.format("%s.%s", fullName, callback);
    if (!sendQueues.containsKey(key)) {
      return null;
    }
    return sendQueues.get(key).size();
  }
}