package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.config.WebGuiConfig;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.slf4j.Logger;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * 
 * WebGui - This service is the AngularJS based GUI TODO - messages &amp;
 * services are already APIs - perhaps a data API - same as service without the
 * message wrapper
 */
public class WebGui extends Service<WebGuiConfig>
    implements AuthorizationProvider, Gateway, Handler, ServiceLifeCycleListener {

  public static class LiveVideoStreamHandler implements Handler {

    @Override
    public void handle(AtmosphereResource r) {
      // TODO Auto-generated method stub
      try {

        AtmosphereResponse response = r.getResponse();
        // response.setContentType("video/mp4");
        // response.setContentType("video/x-flv");
        response.setContentType("video/avi");
        // FIXME - mime type of avi ??

        ServletOutputStream out = response.getOutputStream();

        byte[] data = FileIO.toByteArray(new File("test.avi.h264.mp4"));

        log.info("bytes {}", data.length);
        out.write(data);
        out.flush();
      } catch (Exception e) {
        log.error("stream handler threw", e);
      }
    }
  }

  private final transient IncomingMsgQueue inMsgQueue = new IncomingMsgQueue();

  public static class Panel {

    int height = 400;
    boolean hide = false;
    String name;
    int posX = 40;
    int posY = 20;
    int preferredHeight = 600;
    int preferredWidth = 800;
    String simpleName;
    int width = 400;
    int zIndex = 1;

    public Panel(String panelName) {
      this.name = panelName;
    }

    public Panel(String name, int x, int y, int z) {
      this.name = name;
      this.posX = x;
      this.posY = y;
      this.zIndex = z;
    }
  }

  public final static Logger log = LoggerFactory.getLogger(WebGui.class);

  private static final long serialVersionUID = 1L;

  transient protected JmDNS jmdns = null;

  /**
   * needed to get the api key to select the appropriate api processor
   * 
   * @param uri
   *            u
   * @return api key
   * 
   */
  static public String getApiKey(String uri) {
    int pos = uri.indexOf(CodecUtils.PARAMETER_API);
    if (pos > -1) {
      pos += CodecUtils.PARAMETER_API.length();
      int pos2 = uri.indexOf("/", pos);
      if (pos2 > -1) {
        return uri.substring(pos, pos2);
      } else {
        return uri.substring(pos);
      }
    }
    return null;
  }

  String address = "0.0.0.0";

  boolean autoStartBrowser = true;

  transient Broadcaster broadcaster;

  transient BroadcasterFactory broadcasterFactory;

  String currentDesktop = "default";

  transient Map<String, Map<String, Panel>> desktops;

  transient Nettosphere nettosphere;

  // SHOW INTERFACE
  // FIXME - allowAPI1(true|false)
  // FIXME - allowAPI2(true|false)
  // FIXME - allow Protobuf/Thrift/Avro

  transient AtmosphereResourceEventListenerAdapter onDisconnect;

  // FIXME might need to change to HashMap<String, HashMap<String,String>> to
  // add client session
  // TODO - probably should have getters - to publish - currently
  // just marking as transient to remove some of the data load 10240 max frame
  transient Map<String, Panel> panels = new HashMap<String, Panel>();

  // FIXME - add as a config member
  public Integer port;

  public String root = "root";

  public boolean isSsl = false;

  public String startURL = "http://localhost:%d/#/tabs";

  transient LiveVideoStreamHandler stream = new LiveVideoStreamHandler();

  boolean useLocalResources = false;

  boolean debugConnectivity = false;

  /**
   * Broadcast mode deterimines how clients are to be handled - if they have a
   * single id of webgui-client-1234-5678 or their own unique id - there are
   * pros and cons of this. Previously, the webgui would just send its own name
   * in subscriptions which was not a good design. Then unique id's were created
   * which give the advantage of each page being individually assignable, but at
   * the same time this cluttered the subscriptions. Everytime a page was
   * refresh or a new page brought up many subscriptions would need to be
   * created in order to support the same view.
   * 
   * In its current design, its in the middle where the javascript webgui has a
   * "single" id but it isn't the java's webgui name.
   * 
   * MAKE NOTE !!! - if broadcastMode = false there is a bug since only 1 key is
   * served although its a many to one relations ship with 1 id internally ==
   * webgui-client-1234-5678 sendRemote will ONLY SEND TO ONE CLIENT until this
   * is fixed I'm leaving it in broadcastMode
   * 
   */
  private boolean broadcastMode = true;

  protected int maxMsgSize = 1048576;

  public WebGui(String n, String id) {
    super(n, id);

    if (desktops == null) {
      desktops = new HashMap<String, Map<String, Panel>>();
    }
    if (!desktops.containsKey(currentDesktop)) {
      panels = new HashMap<String, Panel>();
      desktops.put(currentDesktop, panels);
    } else {
      panels = desktops.get(currentDesktop);
    }

    onDisconnect = new AtmosphereResourceEventListenerAdapter() {

      @Override
      public void onDisconnect(AtmosphereResourceEvent event) {
        String uuid = event.getResource().uuid();
        log.info("onDisconnect - {} {}", event, uuid);
        Runtime runtime = Runtime.getInstance();
        runtime.removeConnection(uuid);
        // runtime.removeRoute(uuid);
        // sessions.remove(uuid);
        if (event.isCancelled()) {
          log.info("{} is canceled", uuid);
          // Unexpected closing. The client didn't send the close message when
          // request.enableProtocol
        } else if (event.isClosedByClient()) {
          // atmosphere.js has send the close message.
          // This API is only with 1.1 and up
          log.info("{} is closed by client", uuid);
        }

        // broadcasting - closing of a connection means removal of a connection
        // and all associated services
        // TODO - int the future perhaps do not be so destructive - mark the
        // services as 'unknown' state

      }
    };
  }

  @Override // FIXME - implement
  public boolean allowExport(String serviceName) {
    // TODO Auto-generated method stub
    return false;
  }

  public void autoStartBrowser(boolean autoStartBrowser) {
    this.autoStartBrowser = autoStartBrowser;
  }

  public boolean getAutoStartBrowser() {
    return autoStartBrowser;
  }

  /**
   * String broadcast to specific client
   * 
   * @param uuid
   *             u
   * @param str
   *             s
   * 
   */
  public void broadcast(String uuid, String str) {
    Broadcaster broadcaster = getBroadcasterFactory().lookup(uuid);
    broadcaster.broadcast(str);
  }

  @Override
  public void connect(String uri) throws URISyntaxException {
    // TODO Auto-generated method stub

  }

  public Broadcaster getBroadcaster() {
    return broadcaster;
  }

  public BroadcasterFactory getBroadcasterFactory() {
    return broadcasterFactory;
  }

  @Override
  public List<String> getClientIds() {
    return Runtime.getInstance().getConnectionUuids(getName());
  }

  @Override
  public Map<String, Connection> getClients() {
    return Runtime.getInstance().getConnections(getName());
  }

  public Config.Builder getNettosphereConfig() {

    Config.Builder configBuilder = new Config.Builder();
    try {
      if (isSsl) {
        // SelfSignedCertificate cert = new SelfSignedCertificate();
        // SslContext context = SslContextBuilder.forServer(cert.certificate(),
        // cert.privateKey()).build();

        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        SslContext context = SslContextBuilder
            .forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey())
            .sslProvider(SslProvider.JDK)
            .clientAuth(ClientAuth.NONE).build();

        configBuilder.sslContext(context);
      }
    } catch (Exception e) {
      log.error("certificate creation threw", e);
    }

    configBuilder.resource("/stream", stream);

    // add all webgui resource directories
    for (String resource : config.resources) {
      configBuilder.resource(resource);
    }

    // can't seem to make this work .mappingPath("resource/")

    // TO SUPPORT LEGACY - BEGIN
    // for debugging
    /*
     * .resource("./src/main/resources/resource/WebGui/app")
     * .resource("./resource/WebGui/app")
     */

    // Support 2 APIs
    // REST - http://host/object/method/param0/param1/...
    // synchronous DO NOT SUSPEND
    configBuilder.resource("/api", this);

    configBuilder.maxWebSocketFrameAggregatorContentLength(maxMsgSize);
    configBuilder.initParam("org.atmosphere.cpr.asyncSupport", "org.atmosphere.container.NettyCometSupport");
    configBuilder.initParam(ApplicationConfig.SCAN_CLASSPATH, "false");
    configBuilder.initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true").port(port).host(address); // all
    configBuilder.maxChunkContentLength(maxMsgSize);
    configBuilder.maxWebSocketFrameSize(maxMsgSize);
    // ips

    /*
     * SSLContext sslContext = createSSLContext();
     * 
     * if (sslContext != null) { configBuilder.sslContext(sslContext); } //
     * SessionSupport ss = new SessionSupport();
     */

    configBuilder.build();
    return configBuilder;
  }

  public Map<String, String> getHeadersInfo(HttpServletRequest request) {

    Map<String, String> map = new HashMap<String, String>();

    /**
     * Atmosphere (nearly) always gives a ConcurrentModificationException its
     * supposed to be fixed in later versions - but later version have proven
     * very unstable
     */
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String key = headerNames.nextElement();
      String value = request.getHeader(key);
      map.put(key.toLowerCase(), value);
    }
    return map;
  }

  /*
   * public Map<String, HttpSession> getSessions() { return sessions; }
   */

  // FIXME - mappings for ease of use
  /*
   * public String getUuid(String simpleName) { for (HttpSession client :
   * sessions.values()) { if (String.format("%s@%s",
   * client.getAttribute("user"),
   * client.getAttribute("host")).equals(simpleName)) { return
   * client.getAttribute("uuid").toString(); } } return null; }
   */

  public Integer getPort() {
    return port;
  }

  public String getAddress() {
    return address;
  }

  protected void setBroadcaster(AtmosphereResource r) {
    // FIXME - maintain single broadcaster for each session ?
    String uuid = r.uuid();

    Broadcaster uuiBroadcaster = getBroadcasterFactory().lookup(uuid);
    // create a unique broadcaster in the framework for this uuid
    if (uuiBroadcaster == null) {
      uuiBroadcaster = getBroadcasterFactory().get(uuid);
      uuiBroadcaster.addAtmosphereResource(r);
      uuiBroadcaster.getAtmosphereResources();
      // r.addBroadcaster(uuiBroadcaster);
    }

    log.debug("resource {}", r);
    // log.debug("resource {}", StringUtil.toString(r.broadcasters()));
  }

  /**
   * This method handles all http:// and ws:// requests. Depending on apiKey
   * which is part of initial GET
   * <p>
   * </p>
   * messages api attempts to promote the connection to websocket and suspends
   * the connection for a 2 way channel
   * <p>
   * </p>
   * id and session_id authentication should be required
   * 
   */
  @Override
  public void handle(AtmosphereResource r) {

    boolean newPersistentConnection = false;

    try {

      String apiKey = getApiKey(r.getRequest().getRequestURI());

      String uuid = r.uuid();

      if (!CodecUtils.API_SERVICE.equals(apiKey) && !CodecUtils.API_MESSAGES.equals(apiKey)) {
        // NOT A VALID API - send what we support - we're done...
        OutputStream out = r.getResponse().getOutputStream();
        r.getResponse().addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);
        out.write(CodecUtils.toJson(CodecUtils.getApis()).getBytes());
        return;
      }

      if (apiKey.equals(CodecUtils.API_MESSAGES)) {
        // warning - r can change through the ws:// life-cycle
        // we upsert it to keep it fresh ;)
        newPersistentConnection = upsertConnection(r);
        if (newPersistentConnection) {
          r.suspend();
        }
        // FIXME - needed ?? - we use BroadcastFactory now !
        setBroadcaster(r);
      }

      // default return encoding
      r.getResponse().addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);

      AtmosphereRequest request = r.getRequest();

      String bodyData = null;

      if (request.body() != null && !request.body()
          .isEmpty() /* && !apiKey.equals(CodecUtils.API_MESSAGES) */) {
        // log.info("apiKey {}", apiKey);
        // body returns null after destroy
        if (CodecUtils.API_MESSAGES.equals(apiKey)) {
          bodyData = request.body().asString();
        } else {
          bodyData = new String(request.body().asBytes());
        }

      }

      request.destroy();
      String logData = null;

      if (debugConnectivity) {
        if ((bodyData != null) && log.isInfoEnabled()) {
          if (bodyData.length() > 180) {
            logData = String.format("%s ...", bodyData.substring(0, 179));
          } else {
            logData = bodyData;
          }
        } else if ((bodyData != null) && log.isDebugEnabled()) {
          logData = bodyData;
        }
        log.debug("-->{} {} {} - [{}] from connection {}", (newPersistentConnection) ? "new" : "", request.getMethod(),
            request.getRequestURI(), logData, uuid);
      }

      // important persistent connections will have associated routes ...
      // http/api/service requests (not persistent connections) will not
      // (neither will udp)
      if (newPersistentConnection && apiKey.equals(CodecUtils.API_MESSAGES)) {

        // new connection established
        // subscribe to its describe
        // send a describe
        OutputStream out = r.getResponse().getOutputStream();

        // subscribe to describe
        MRLListener listener = new MRLListener("describe", String.format("runtime@%s", getId()), "onDescribe");
        Message subscribe = Message.createMessage(getFullName(), "runtime", "addListener", listener);
        // Default serialization to json/text is to json encode the parameter
        // list
        // then json encode the message
        out.write(CodecUtils.toJsonMsg(subscribe).getBytes());

        // describe
        Message describe = getDescribeMsg(uuid); // SEND BACK describe(hello)
        // Service.sleep(1000);
        // log.info(String.format("new connection %s",
        // request.getRequestURI()));
        // out.write(CodecUtils.toJson(describe).getBytes());
        // describe.setName("runtime@" + id);
        out.write(CodecUtils.toJsonMsg(describe).getBytes());// DOUBLE-ENCODE
        // i assume that flush/close happen when out of scope - but do it
        // explicitly here
        out.flush();
        out.close();
        log.info(String.format("<-- %s", describe));
        return;

      } else if (apiKey.equals(CodecUtils.API_SERVICE)) {

        String path = URLDecoder.decode(r.getRequest().getPathInfo(), StandardCharsets.UTF_8);
        Message msg = CodecUtils.pathToMsg(getFullName(), path);

        if (bodyData != null) {
          msg.data = CodecUtils.fromJson(bodyData, Object[].class);
        } else {
          msg = CodecUtils.decodeMessageParams(msg);
        }

        if (isLocal(msg)) {
          // String serviceName = msg.getFullName();// getName();
          // Class<?> clazz = Runtime.getClass(serviceName);
          // Object[] params = cache.getDecodedJsonParameters(clazz, msg.method,
          // msg.data);
          // msg.data = params;
          Object ret = invoke(msg);
          OutputStream out = r.getResponse().getOutputStream();
          out.write(CodecUtils.toJson(ret).getBytes());
        } else {
          // TODO - send it on its way - possibly do not decode the parameters
          // this would allow it to traverse mrl instances which did not
          // have the class definition !
          send(msg);
        }
        // FIXME - remove connection ! AND/OR figure out session
        return;
      }

      if (bodyData != null) {

        // decoding 1st pass - decodes the containers
        Message msg = null;
        try {
          msg = CodecUtils.jsonToMessage(bodyData);
          if (msg == null) {
            log.error("Got null message from client, check client code for bugs");
            return;
          }

          if (msg.containsHop(getId())) {
            log.error("{} dumping duplicate hop msg to avoid cyclical from {} --to--> {}.{}", getName(), msg.sender,
                msg.name, msg.method);
            return;
          }

          // add our id - we don't want to see it again
          msg.addHop(getId());

        } catch (Exception e) {
          error(e);
          return;
        }
        msg.setProperty("uuid", uuid);

        // check if we will execute it locally
        if (isLocal(msg)) {
          String serviceName = null;
          try {
            log.debug("invoking local msg {}", msg);

            serviceName = msg.getFullName();
            Class<?> clazz = Runtime.getClass(serviceName);
            if (clazz == null) {
              // can occur if target service is no longer running
              log.warn("cannot derive local type from service {}", serviceName);
              return;
            }

            // do not decode unless needed
            // Object[] params = cache.getDecodedJsonParameters(clazz,
            // msg.method, msg.data);

            ServiceInterface si = Runtime.getService(serviceName);

            // Just pass to service to deal with, no duplicated code
            // and allows things like sendBlocking() to work correctly
            si.getInbox().add(msg);
          } catch (Exception e) {
            error("local msg threw %s.%s.%s", serviceName, msg.method, e);
          }

          // propagate return data to subscribers
          // si.out(msg.method, ret);

        } else {
          // msg came is and is NOT local - we will attempt to route it on its
          // way by sending it to send(msg)
          // RELAY !!!
          log.info("GATEWAY {} RELAY {} --to--> {}.{}", getName(), msg.sender, msg.name, msg.method);
          send(msg);
        }
      }
    } catch (Exception e) {
      error(e);
      // log.error("handle threw", e);
    }
  }

  public class InvokeData {
    public ServiceInterface si = null;
    public Method method = null;
    public Object[] params = null;

    public InvokeData(Method method, ServiceInterface si, Object[] params) {
      this.method = method;
      this.si = si;
      this.params = params;
    }

  }

  public class IncomingMsgQueue implements Runnable {

    boolean isRunning = false;

    Thread worker = null;

    Object lock = new Object();

    private transient LinkedBlockingQueue<InvokeData> inMsgQueue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
      isRunning = true;
      while (isRunning) {
        try {
          InvokeData data = inMsgQueue.poll(1, TimeUnit.SECONDS);

          if (data != null) {
            if (data.method == null) {
              log.error("method null %s", data);
              continue;
            }
            Object ret = data.method.invoke(data.si, data.params);
            data.si.out(data.method.getName(), ret);
          }
        } catch (InterruptedException interrupt) {
          log.info("shutting down");
          isRunning = false;
        } catch (Exception e) {
          log.error("IncomingMessageQueue threw", e);
        }

      } // while is running

      isRunning = false;
      worker = null;
    }

    public void add(ServiceInterface si, Method method, Object[] params) {
      // TODO Auto-generated method stub
      inMsgQueue.add(new InvokeData(method, si, params));
    }

    public void start() {
      synchronized (lock) {
        if (worker == null) {
          worker = new Thread(this, getName() + "-incoming-msg-queue");
          worker.start();
        }
      }
    }

    public void stop() {
      synchronized (lock) {
        isRunning = false;
        if (worker != null) {
          worker.interrupt();
        }
      }
    }

  }

  @Override
  public boolean isLocal(Message msg) {
    return Runtime.getInstance().isLocal(msg);
  }

  private boolean upsertConnection(AtmosphereResource r) {
    String uuid = r.uuid();
    Runtime runtime = Runtime.getInstance();
    String id = r.getRequest().getParameter("id");
    Connection connection = new Connection(r.uuid(), id, getName());

    if (!runtime.connectionExists(r.uuid())) {
      r.addEventListener(onDisconnect);
      AtmosphereRequest request = r.getRequest();
      Enumeration<String> headerNames = request.getHeaderNames();

      // required attributes - id ???/
      connection.put("uuid", r.uuid());
      // so this is an interesting one .. getRequestURI is less descriptive than
      // getRequestURL
      // yet by RFC definition URL is a subset of URI .. wtf ?
      connection.put("uri", r.getRequest().getRequestURL().toString());
      // attributes.put("url", r.getRequest().getRequestURL());
      connection.put("host", r.getRequest().getRemoteAddr());
      connection.put("gateway", getName());

      // connection specific
      connection.putTransient("c-r", r);
      connection.put("c-type", getSimpleName());

      // cli specific
      connection.put("cwd", "/");

      // addendum
      connection.put("user", "root");

      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        Enumeration<String> headers = request.getHeaders(headerName);
        while (headers.hasMoreElements()) {
          String headerValue = headers.nextElement();
          connection.put(String.format("header-%s", headerName), headerValue);
        }
      }
      Runtime.getInstance().addConnection(uuid, id, connection);
      return true;
    } else {
      // keeping it "fresh" - the resource changes every request ..
      // it switches on
      runtime.getConnection(uuid).putTransient("c-r", r);
      return false;
    }
  }

  /**
   * handleMessagesApi handles all requests sent to /api/messages It
   * suspends/upgrades the connection to a websocket It is a asynchronous
   * protocol - and all messages are wrapped in a message wrapper.
   * 
   * The ApiFactory will handle the details of de-serializing but its necessary
   * to setup some protocol details here for websockets and session management
   * which the ApiFactory should not be concerned with.
   * 
   * @param r
   *          - request and response objects from Atmosphere server
   */
  public void handleMessagesApi(AtmosphereResource r) {
    try {
      AtmosphereResponse response = r.getResponse();

      if (!r.isSuspended()) {
        r.suspend();
      }
      response.addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);
      // api.process(this, out, r.getRequest().getRequestURI(),
      // request.body().asString());

    } catch (Exception e) {
      log.error("handleMessagesApi -", e);
    }
  }

  public void handleMessagesBlockingApi(AtmosphereResource r) {
    try {
      AtmosphereResponse response = r.getResponse();

      if (!r.isSuspended()) {
        r.suspend();
      }
      response.addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);

    } catch (Exception e) {
      log.error("handleMessagesBlockingApi -", e);
    }
  }

  public void hide(String name) {
    invoke("publishHide", name);
  }

  @Override
  public boolean isAuthorized(Map<String, Object> security, String serviceName, String method) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAuthorized(Message msg) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isStarted() {
    if (nettosphere != null && nettosphere.isStarted()) {
      // is running
      info("WebGui is started");
      return true;
    }
    return false;

  }

  public Map<String, Panel> loadPanels() {
    return panels;
  }

  public void onLogEvent(Message msg) {
    try {
      if (broadcaster != null) {
        broadcaster.broadcast(CodecUtils.toJson(msg));
      }
    } catch (Exception e) {
      System.out.print(e.getMessage());
    }
  }

  public String publishHide(String name) {
    return name;
  }

  public Panel publishPanel(String panelName) {

    if (panels == null) {
      return null;
    }

    Panel panel = null;
    if (panels.containsKey(panelName)) {
      panel = panels.get(panelName);
    } else {
      panel = new Panel(panelName);
      panels.put(panelName, panel);
    }
    return panel;
  }
  // === end positioning panels plumbing ===

  /*
   * public void put(String uuid, HttpSession s) { sessions.put(uuid, s); }
   */

  public void publishPanels() {
    for (String key : panels.keySet()) {
      invoke("publishPanel", key);
    }
  }

  public String publishShow(String name) {
    return name;
  }

  public boolean publishShowAll(boolean b) {
    return b;
  }

  /*
   * redirects browser to new url
   */
  public String redirect(String url) {
    return url;
  }

  public void restart() {
    stop();
    WebGui _self = this;

    // done so a thread "from" webgui can restart itself
    // similar thread within stop
    // From a web request you cannot block on a request to stop/start self
    new Thread() {
      @Override
      public void run() {
        try {
          while (nettosphere != null) {
            Thread.sleep(500);
          }
        } catch (InterruptedException e) {
        }
        _self.start();
      }
    }.start();
    start();
  }

  /**
   * From UI events --to--&gt; MRL request to save panel data typically done
   * after user has changed or updated the UI in position, height, width, zIndex
   * etc.
   * 
   * If you need MRL changes of position or UI changes use publishPanel to
   * remotely control UI
   * 
   * @param panel
   *              - the panel which has been moved or resized
   */
  public void savePanel(Panel panel) {
    if (panel.name == null) {
      log.error("panel name is null!");
      return;
    }
    panels.put(panel.name, panel);
    save();
  }

  @Override
  public void sendRemote(Message msg) {
    try {

      // add our id - we don't want to see it again
      msg.addHop(getId());

      // Double encoding - parameters then message
      String json = CodecUtils.toJsonMsg(msg);

      if (json.length() > maxMsgSize) {
        log.info(String.format("sendRemote default msg size (%d) exceeded 65536 for msg %s", json.length(), msg));
        /*
         * debugging large msgs try {
         * FileIO.toFile(String.format("too-big-%s-%d.json", msg.method,
         * System.currentTimeMillis()), json); } catch (Exception e) { }
         */
      }

      if (broadcastMode) {
        // multi-cast mode all clients have a single id
        broadcaster.broadcast(json);
      } else {
        // uni-cast mode - all clients have their own id
        Connection c = Runtime.getInstance().getRoute(msg.getId());
        Broadcaster broadcaster = getBroadcasterFactory().lookup(c.getUuid());
        if (broadcaster == null) {
          log.warn("{} failed to lookup broadcaster {} client gone?", getName(), c.getUuid());
          return;
        }
        broadcaster.broadcast(json);
      }
    } catch (Exception e) {
      log.error("WebGui.sendRemote threw", e);
    }
  }

  // === begin positioning panels plumbing ===
  public void set(String name, int x, int y) {
    set(name, x, y, 0); // or is z -1 ?
  }

  public void set(String name, int x, int y, int z) {
    Panel panel = null;
    if (panels.containsKey(name)) {
      panel = panels.get(name);
    } else {
      panel = new Panel(name, x, y, z);
    }
    invoke("publishPanel", name);
  }

  public void setAddress(String address) {
    if (address != null) {
      this.address = address;
    }
  }

  public void setPort(Integer port) {
    this.port = port; // restart service ?
  }

  public void show(String name) {
    invoke("publishShow", name);
  }

  // TODO - refactor next 6+ methods to only us publishPanel
  public void showAll(boolean b) {
    invoke("publishShowAll", b);
  }

  public void start() {
    try {

      log.info("starting webgui service....");

      if (port == null) {
        port = 8888;
      }

      // Broadcaster b = broadcasterFactory.get();
      // a session "might" be nice - but for now we are stateless
      // SessionSupport ss = new SessionSupport();

      if (nettosphere != null && nettosphere.isStarted()) {
        // is running
        log.info("webgui already started on port {}", port);
        return;
      }

      nettosphere = new Nettosphere.Builder().config(getNettosphereConfig().build()).build();
      sleep(1000); // needed ?

      try {
        nettosphere.start();
      } catch (Exception e) {
        log.error("starting nettosphere failed", e);
      }

      broadcasterFactory = nettosphere.framework().getBroadcasterFactory();
      // get default boadcaster
      // GLOBAL - doesnt work because all come in with /api !
      // broadcaster = broadcasterFactory.get("/*");
      broadcaster = broadcasterFactory.lookup("/api"); // get("/api") throws
                                                       // because already
                                                       // created !

      log.info("WebGui {} started on port {}", getName(), port);
      // get all instances

      // 20191126 - REMOVED ... does it make a difference ? Groot ?
      // we want all onState & onStatus events from all services
      // for (ServiceInterface si : Runtime.getLocalServices().values()) {
      // onRegistered(si);
      // }

      // additionally we will want onState & onStatus events from all
      // services
      // from all new services which were created "after" the webgui
      // so susbcribe to our Runtimes methods of interest
      Runtime runtime = Runtime.getInstance();
      subscribe(runtime.getName(), "registered");
      subscribe(runtime.getName(), "released");

      if (autoStartBrowser) {
        log.info("auto starting default browser");
        BareBonesBrowserLaunch.openURL(String.format(startURL, port));
      }

    } catch (Exception e) {
      log.error("start threw", e);
    }
  }

  public void startBrowser(String URL) {
    BareBonesBrowserLaunch.openURL(String.format(URL, port));
  }

  @Override
  public void startService() {
    super.startService();
    inMsgQueue.start();
    start();
  }

  public void stop() {
    if (nettosphere != null) {
      log.info("==== nettosphere STOPPING ====");
      // done so a thread "from" webgui can stop itself :P
      // Must not be called from a I/O-Thread to prevent deadlocks!
      new Thread() {
        @Override
        public void run() {
          nettosphere.framework().removeAllAtmosphereHandler();
          nettosphere.stop();
          nettosphere = null;
          log.info("==== nettosphere STOPPED ====");
        }
      }.start();
    }
  }

  @Override
  public void releaseService() {
    super.releaseService();
    stopMdns();
    stop();
    inMsgQueue.stop();
  }

  /**
   * UseLocalResources determines if references to JQuery JavaScript library are
   * local or if the library is linked to using content delivery network.
   * Default (false) is to use the CDN
   *
   * @param useLocalResources
   *                          - true uses local resources fals uses cdn
   */
  public void useLocalResources(boolean useLocalResources) {
    this.useLocalResources = useLocalResources;
  }

  public void display(String image) {
    // FIXME
    // http/https can be proxied if necessary or even fetched,
    // but what about "local" files - should they be copied to a temp directory
    // that has webgui access ?
    // e.g. copied to /data/WebGui/temp ?
    // send(getName(), "display", image);
    Message msg = Message.createMessage(getName(), "webgui", "display", new Object[] { image });
    sendRemote(msg);
  }

  public void setSsl(boolean b) {
    isSsl = b;
  }

  public void startMdns() {
    try {
      if (jmdns == null) {
        Runtime runtime = Runtime.getInstance();
        String ip = runtime.getAddress();
        log.info("starting mdns {} on {}", runtime.getId(), ip);
        jmdns = JmDNS.create(InetAddress.getByName(ip), runtime.getId());
        ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", runtime.getId(), getPort(), "myrobotlab");
        jmdns.registerService(serviceInfo);
        serviceInfo = ServiceInfo.create("_ws._tcp.local.", runtime.getId(), getPort(), "myrobotlab");
        jmdns.registerService(serviceInfo);
      }
    } catch (Exception e) {
      log.error("mdns threw", e);
    }
  }

  public void stopMdns() {
    if (jmdns != null) {
      jmdns.unregisterAllServices();
      try {
        jmdns.close();
      } catch (IOException e) {
      }
      jmdns = null;
    }
  }

  @Override
  // FIXME port and autoStartBrowser should just be part of config
  // then this override can be removed
  public WebGuiConfig getConfig() {
    config.port = port;
    config.autoStartBrowser = autoStartBrowser;
    return config;
  }

  public WebGuiConfig apply(WebGuiConfig c) {
    super.apply(c);

    if (c.port != null) {
      setPort(c.port);
    }
    autoStartBrowser(c.autoStartBrowser);
    if (c.enableMdns) {
      startMdns();
    }
    return c;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.main(new String[] { "--log-level", "warn", "-s", "webgui", "WebGui"});
      // Runtime.main(new String[] { "--install" });

      boolean done = true;
      if (done) {
        return;
      }

      // Platform.setVirtual(true);
      // Runtime.main(new String[] { "--log-level", "info", "-s", "webgui",
      // "WebGui",
      // "intro", "Intro", "python", "Python", "-c", "dev" });
      // Runtime.startConfig("dev");

      // Runtime.start("python", "Python");
      // Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8888);
      // webgui.setSsl(true);
      webgui.startService();

      Runtime.start("python", "Python");
      // Runtime.start("intro", "Intro");
      // Runtime.start("i01", "InMoov2");

      // Runtime.start("i01", "InMoov2");
      // Runtime.start("python", "Python");
      // Runtime.start("i01", "InMoov2");

      // Runtime.start("i01", "InMoov2");
      Runtime.start("track", "Tracking");
      // Runtime.startConfig("worky");
      // Runtime.startConfig("InMoov2Head");
      // Runtime.startConfig("Tracking");

      // Runtime.start("i01", "InMoov2");
      // Runtime.start("python", "Python");
      // Runtime.start("i01", "InMoov2");

      // Runtime.start("i01", "InMoov2");
      Runtime.start("track", "Tracking");
      // Runtime.startConfig("worky");
      // Runtime.startConfig("InMoov2Head");
      // Runtime.startConfig("Tracking");

      MqttBroker broker = (MqttBroker) Runtime.start("broker", "MqttBroker");
      broker.listen();

      Mqtt mqtt01 = (Mqtt) Runtime.start("mqtt01", "Mqtt");
      /*
       * mqtt01.setCert("certs/home-client/rootCA.pem",
       * "certs/home-client/cert.pem.crt", "certs/home-client/private.key");
       * mqtt01.connect(
       * "mqtts://a22mowsnlyfeb6-ats.iot.us-west-2.amazonaws.com:8883");
       */
      mqtt01.connect("mqtt://localhost:1883");

      Runtime.start("neo", "NeoPixel");

      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("/dev/ttyACM0");

      for (int i = 0; i < 1000; ++i) {
        webgui.display(
            "https://i.kinja-img.com/gawker-media/image/upload/c_scale,f_auto,fl_progressive,q_80,w_800/pytutcxcrfjvuhz2jipa.jpg");
      }

      // Runtime.setLogLevel("ERROR");
      // Runtime.start("python", "Python");
      // Runtime.start("clock01", "Clock");
      // Runtime.start("arduino", "Arduino");
      // Runtime.start("servo01", "Servo");
      // Runtime.start("servo02", "Servo");
      // Runtime.start("gui", "SwingGui");

      // runtime.setVirtual(true);
      // Runtime.start("log", "Log");
      /*
       * Runtime.start("clock01", "Clock"); Runtime.start("clock02", "Clock");
       * Runtime.start("clock03", "Clock"); Runtime.start("clock04", "Clock");
       * Runtime.start("clock05", "Clock");
       */

      // Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      Servo pan = (Servo) Runtime.start("pan", "Servo");
      Servo tilt = (Servo) Runtime.start("tilt", "Servo");
      pan.setPin(3);
      pan.setMinMax(30.0, 70.0);
      tilt.setPin("D4");

      arduino.attach(pan);
      arduino.attach(tilt);

      // Runtime.start("jme", "JMonkeyEngine");

      // arduino.connect("/dev/ttyACM0");

      // Runtime.start("arduino", "Arduino");
      // arduino.connect("COMX");

      log.info("leaving main");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void onCreated(String name) {
  }

  @Override
  public void onRegistered(Registration registration) {
  }

  @Override
  public void onStarted(String name) {
  }

  @Override
  public void onStopped(String name) {
  }

  @Override
  public void onReleased(String name) {
  }

}
