package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;
import org.myrobotlab.codec.Api;
import org.myrobotlab.codec.ApiFactory;
import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecFactory;
import org.myrobotlab.codec.CodecJson;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.Connection;
//import org.myrobotlab.service.WebGUI3.Error;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.Gateway;
//import org.myrobotlab.webgui.WebGUIServlet;
import org.slf4j.Logger;

/**
 * 
 * WebGui - This service is the AngularJS based GUI TODO - messages &amp;
 * services are already APIs - perhaps a data API - same as service without the
 * message wrapper
 */
public class WebGui extends Service implements AuthorizationProvider, Gateway, Handler {

  /**
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   */

  public static class LiveVideoStreamHandler implements Handler {

    @Override
    public void handle(AtmosphereResource r) {
      // TODO Auto-generated method stub
      try {

        /*
         * OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
         * OpenCVFilterFFMEG ffmpeg = new OpenCVFilterFFMEG("ffmpeg");
         * opencv.addFilter(ffmpeg); opencv.capture(); sleep(1000);
         * opencv.removeFilters(); ffmpeg.stopRecording();
         */

        AtmosphereResponse response = r.getResponse();
        // response.setContentType("video/mp4");
        // response.setContentType("video/x-flv");
        response.setContentType("video/avi");
        // FIXME - mime type of avi ??

        ServletOutputStream out = response.getOutputStream();
        // response.addHeader(name, value);

        // byte[] data = FileIO.fileToByteArray(new
        // File("flvTest.flv"));
        // byte[] data = FileIO.fileToByteArray(new
        // File("src/resource/WebGUI/video/ffmpeg.1443989700495.mp4"));
        // byte[] data = FileIO.fileToByteArray(new
        // File("mp4Test.mp4"));
        byte[] data = FileIO.toByteArray(new File("test.avi.h264.mp4"));

        log.info("bytes {}", data.length);
        out.write(data);
        out.flush();

        // out.close();
        // r.write(data);
        // r.writeOnTimeout(arg0)
        // r.forceBinaryWrite();
        // r.close();

      } catch (Exception e) {
        Logging.logError(e);
      }

    }

  }

  public static class Panel {

    String name;
    String simpleName;
    int posX = 40;
    int posY = 20;
    int zIndex = 1;
    int width = 400;
    int height = 400;
    int preferredWidth = 800;
    int preferredHeight = 600;
    boolean hide = false;

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

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebGui.class);

  private static final AtomicBoolean TRUST_SERVER_CERT = new AtomicBoolean(true);
  private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      if (!TRUST_SERVER_CERT.get()) {
        throw new CertificateException("Server certificate not trusted.");
      }
    }

    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  };
  private static SSLContext createSSLContext2() {
    try {
      InputStream keyStoreStream = Security.class.getResourceAsStream("resource/keys/selfsigned.jks");
      char[] keyStorePassword = "changeit".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(keyStoreStream, keyStorePassword);

      // Set up key manager factory to use our key store
      char[] certificatePassword = "changeit".toCharArray();
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, certificatePassword);

      // Initialize the SSLContext to work with our key managers.
      KeyManager[] keyManagers = kmf.getKeyManagers();
      TrustManager[] trustManagers = new TrustManager[] { DUMMY_TRUST_MANAGER };
      SecureRandom secureRandom = new SecureRandom();

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, secureRandom);
      return sslContext;
    } catch (Exception e) {
      throw new Error("Failed to initialize SSLContext", e);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WebGui.class.getCanonicalName());
    meta.addDescription("web display");
    meta.addCategory("connectivity", "display");

    // MAKE NOTE !!! - we currently distribute myrobotlab.jar with a webgui
    // hence these following dependencies are zipped with myrobotlab.jar !
    // and are NOT listed as dependencies, because they are already included

    // Its now part of myrobotlab.jar - unzipped in
    // build.xml (part of myrobotlab.jar now)

    // meta.addDependency("io.netty", "3.10.0"); // netty-3.10.0.Final.jar
    // meta.addDependency("org.atmosphere.nettosphere", "2.3.0"); //
    // nettosphere-assembly-2.3.0.jar
    // meta.addDependency("org.atmosphere.nettosphere", "2.3.0");//
    // geronimo-servlet_3.0_spec-1.0.jar
    return meta;
  }

  public Integer port;

  public Integer sslPort;

  transient Nettosphere nettosphere;

  transient Broadcaster broadcaster;
  transient BroadcasterFactory broadcastFactory;

  public String root = "root";

  boolean useLocalResources = false;

  boolean autoStartBrowser = true;

  // SHOW INTERFACE
  // FIXME - allowAPI1(true|false)
  // FIXME - allowAPI2(true|false)
  // FIXME - allow Protobuf/Thrift/Avro
  // FIXME - NO JSON ENCODING SHOULD BE IN THIS FILE !!!

  public String startURL = "http://localhost:%d";

  transient final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<String, HttpSession>();

  // FIXME might need to change to HashMap<String, HashMap<String,String>> to
  // add client session
  // TODO - probably should have getters - to publish - currently
  // just marking as transient to remove some of the data load 10240 max frame
  transient Map<String, Panel> panels;

  // ================ Gateway begin ===========================

  transient Map<String, Map<String, Panel>> desktops;

  transient ApiFactory api = null;

  String currentDesktop = "default";

  transient LiveVideoStreamHandler stream = new LiveVideoStreamHandler();

  public WebGui(String n) {
    super(n);
    api = ApiFactory.getInstance(this);
    if (desktops == null) {
      desktops = new HashMap<String, Map<String, Panel>>();
    }
    if (!desktops.containsKey(currentDesktop)) {
      panels = new HashMap<String, Panel>();
      desktops.put(currentDesktop, panels);
    } else {
      panels = desktops.get(currentDesktop);
    }
    String name = Runtime.getRuntimeName();
    subscribe(name, "registered");
    // FIXME - "unregistered" / "released"

  }

  @Override
  public void addConnectionListener(String name) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean allowExport(String serviceName) {
    // TODO Auto-generated method stub
    return false;
  }

  public void autoStartBrowser(boolean autoStartBrowser) {
    this.autoStartBrowser = autoStartBrowser;
  }

  // ================ Gateway end ===========================

  // ================ AuthorizationProvider begin ===========================

  public void broadcast(Message msg) {
    try {
      if (broadcaster != null) {
        Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        codec.encode(bos, msg);
        bos.close();
        broadcaster.broadcast(new String(bos.toByteArray())); // wtf
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public void connect(String uri) throws URISyntaxException {
    // TODO Auto-generated method stub

  }

  SSLContext createSSLContext() {
    try {
      if (sslPort != null) {
        return SSLContext.getInstance("TLS");
      }
    } catch (Exception e) {
      log.warn("can not make ssl context", e);
    }
    return null;
  }

  // ================ AuthorizationProvider end ===========================

  // ================ Broadcaster begin ===========================

  public void extract() throws IOException {
    extract(false);
  }

  public void extract(boolean overwrite) throws IOException {

    // TODO - check resource version vs self version
    // overwrite if different ?

    FileIO.extractResources(overwrite);
  }

  @Override
  public HashMap<URI, Connection> getClients() {
    // TODO Auto-generated method stub
    return null;
  }

  // ================ Broadcaster end ===========================

  public Config.Builder getConfig() {
    Config.Builder configBuilder = new Config.Builder();
    try {
      boolean secureTest = false;
      if (secureTest) {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
        configBuilder.sslContext(createSSLContext());// .sslContext(sslCtx);
      }
    } catch (Exception e) {
      log.error("certificate creation threw", e);
    }

    configBuilder
        /*
         * did not work :( .resource(
         * "jar:file:/C:/mrl/myrobotlab/dist/myrobotlab.jar!/resource")
         * .resource(
         * "jar:file:/C:/mrl/myrobotlab/dist/myrobotlab.jar!/resource/WebGui" )
         */

        .resource("/stream", stream)
        // .resource("/video/ffmpeg.1443989700495.mp4", test)

        // for debugging
        .resource("./src/resource/WebGui").resource("./src/resource")
        // for runtime - after extractions
        .resource("./resource/WebGui").resource("./resource")

        // Support 2 APIs
        // REST - http://host/object/method/param0/param1/...
        // synchronous DO NOT SUSPEND
        .resource("/api", this)

        // if Jetty is in the classpath it will use it by default - we
        // want to use Netty
        // .initParam("org.atmosphere.websocket.maxTextMessageSize",
        // "100000")
        // .initParam("org.atmosphere.websocket.maxBinaryMessageSize",
        // "100000")
        .initParam("org.atmosphere.cpr.asyncSupport", "org.atmosphere.container.NettyCometSupport").initParam(ApplicationConfig.SCAN_CLASSPATH, "false")
        .initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true").port(port).host("0.0.0.0"); // all
    // ips

    SSLContext sslContext = createSSLContext();

    if (sslContext != null) {
      configBuilder.sslContext(sslContext);
    }
    // SessionSupport ss = new SessionSupport();

    configBuilder.build();
    return configBuilder;
  }

  @Override
  public List<Connection> getConnections(URI clientKey) {
    // TODO Auto-generated method stub
    return null;
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
      String key = (String) headerNames.nextElement();
      String value = request.getHeader(key);
      map.put(key.toLowerCase(), value);
    }

    return map;
  }

  public String getId(AtmosphereResource r) {
    String id = r.getRequest().getHeader("id");

    if (id == null) {
      id = "anonymous";
    }
    return id;
  }

  public Integer getPort() {
    return port;
  }

  @Override
  public String getPrefix(URI protocolKey) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * SSLContext createSSLContext() { try { if (sslPort != null) { return
   * SSLContext.getInstance("TLS"); } } catch (Exception e) {
   * log.warn("can not make ssl context", e); } return null; }
   */

  /**
   * With a single method Atmosphere does so much !!! It sets up the connection,
   * possibly gets a session, turns the request into something like a
   * HTTPServletRequest, provides us with input &amp; output streams - and
   * manages all the "long polling" or websocket upgrades on its own !
   * 
   * Atmosphere Rocks !
   * 
   * common to all apis is handled here - then delegated to the appropriate api
   * handler
   */
  @Override
  public void handle(AtmosphereResource r) {

    String apiKey = Api.getApiKey(r.getRequest().getRequestURI());

    AtmosphereRequest request = r.getRequest();
    
    
    log.debug(">> {} - {} - [{}]", request.getMethod(), request.getRequestURI(), request.body().asString());
    

    try {

      // FIXME - maintain single broadcaster for each session ?
      // Broadcaster bc = r.getBroadcaster();
      // if (bc != null || r.getBroadcaster() != broadcaster){
      r.setBroadcaster(broadcaster);
      // }

      String id = getId(r);

      handleSession(r);
      // FIXME - header SAS token for authentication ???
      // Map<String, String> headers = getHeadersInfo(request);

      // GET vs POST - post assumes low-level messaging
      // GET is high level synchronous
      // String httpMethod = request.getMethod();

      // get default encoder
      // FIXME FIXME FIXME - this IS A CODEC !!! NOT AN API-TYPE !!! -
      // CHANGE to MIME_TYPE_APPLICATION_JSON !!!

      // ========================================
      // POST || GET http://{host}:{port}/api/messages
      // POST || GET http://{host}:{port}/api/services
      // ========================================

      // TODO - add handleSwaggerApi
      switch (apiKey) {

        case ApiFactory.API_TYPE_MESSAGES: {
          handleMessagesApi(r);
          break;
        }

        case ApiFactory.API_TYPE_SERVICE: {
          handleServiceApi(r);
          break;
        }

        default: {
          // handleInvalidApi(r); // TODO - swagger list of apis ?
          throw new IOException("invalid api: " + apiKey);
        }
      }

    } catch (Exception e) {
      try {

        AtmosphereResponse response = r.getResponse();
        OutputStream out = response.getOutputStream();

        response.addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);
        Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);

        Status error = Status.error(e);
        Message msg = Message.createMessage(this, getName(), CodecUtils.getCallBackName("getStatus"), error);
        if (ApiFactory.API_TYPE_SERVICE.equals(apiKey)) {
          // for the purpose of only returning the data
          // e.g. http://api/services/runtime/getUptime -> return the uptime
          // only not the message
          if (msg.data == null) {
            log.warn("<< {}", CodecJson.encode(null));
            codec.encode(out, null);
          } else {
            // return the return type
            log.warn("<< {}", CodecJson.encode(msg.data[0]));
            codec.encode(out, msg.data[0]);
          }
        } else {
          // API_TYPE_MESSAGES
          // DEPRECATE - FOR LOGGING ONLY REMOVE
          log.warn("<< {}", CodecJson.encode(msg)); // FIXME if logTraffic
          codec.encode(out, msg);
        }

      } catch (Exception e2) {
        log.error("respond threw", e);
      }
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
   * @param r - request and response objects from Atmosphere server
   */
  public void handleMessagesApi(AtmosphereResource r) {
    try {
      AtmosphereResponse response = r.getResponse();
      AtmosphereRequest request = r.getRequest();
      OutputStream out = response.getOutputStream();

      if (!r.isSuspended()) {
        r.suspend();
      }
      response.addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);

      api.process(this, out, r.getRequest().getRequestURI(), request.body().asString());

      /*
       * // FIXME - GET or POST should work - so this "should" be unnecessary ..
       * if ("GET".equals(httpMethod)) { // if post and parts.length < 3 ?? //
       * respond(r, apiKey, "getPlatform", new Hello(Runtime.getId()));
       * HashMap<URI, ServiceEnvironment> env = Runtime.getEnvironments();
       * respond(r, apiKey, "getLocalServices", env); } else {
       * doNotUseThisProcessMessageAPI(codec, request.body()); // FIXME data is
       * double encoded .. can't put '1st' encoded json // api.process(this,
       * out, r.getRequest().getRequestURI(), request.body().asString()); }
       */

    } catch (Exception e) {
      log.error("handleMessagesApi -", e);
    }
  }

  /**
   * This is a middle level method to handle the details of Atmosphere and
   * http/ws level of processing request. It will eventually call
   * ApiFactory.process which handles the "pure" Jvm Java only processing
   * 
   * @param r
   *          r
   * @throws Exception
   *           e
   * 
   */
  public void handleServiceApi(AtmosphereResource r) throws Exception {
    AtmosphereRequest request = r.getRequest();
    AtmosphereResponse response = r.getResponse();
    OutputStream out = response.getOutputStream();
    response.addHeader("Content-Type", CodecUtils.MIME_TYPE_JSON);
    
    String hack = null;
    byte[] data = null;
    if (request.body() != null) {
      hack = request.body().asString();
      // data = request.body().asBytes();
      if (hack != null){ // FIXME - hack because request.body().asBytes() ALWAYS returns null !!
        data = hack.getBytes();
      }
    }
    api.process(out, r.getRequest().getRequestURI(), data);
  }

  public void handleSession(AtmosphereResource r) {
    AtmosphereRequest request = r.getRequest();
    HttpSession s = request.getSession(true);
    if (s != null) {
      log.debug("put session uuid {}", r.uuid());
      sessions.put(r.uuid(), request.getSession(true));
    }
  }

  public void hide(String name) {
    invoke("publishHide", name);
  }

  @Override
  public boolean isAuthorized(HashMap<String, String> security, String serviceName, String method) {
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

  /*
   * FIXME - needs to be LogListener interface with
   * LogListener.onLogEvent(String logEntry) !!!! THIS SHALL LOG NO ENTRIES OR
   * ABANDON ALL HOPE !!!
   * 
   * This is completely out of band - it does not use the regular queues inbox
   * or outbox
   * 
   * We want to broadcast this - but THERE CAN NOT BE ANY log.info/warn/error
   * etc !!!! or there will be an infinite loop and you will be at the gates of
   * hell !
   * 
   */
  public void onLogEvent(Message msg) {
    try {
      if (broadcaster != null) {
        Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        codec.encode(bos, msg);
        bos.close();
        broadcaster.broadcast(new String(bos.toByteArray())); // wtf
      }
    } catch (Exception e) {
      System.out.print(e.getMessage());
    }
  }

  public void onRegistered(ServiceInterface si) {
    // new service
    // subscribe to the status events
    subscribe(si.getName(), "publishStatus");
    subscribe(si.getName(), "publishState");

    // for distributed Runtimes
    if (si.isRuntime()) {
      subscribe(si.getName(), "registered");
    }

    invoke("publishPanel", si.getName());

    // broadcast it too
    // repackage message
    /*
     * don't need to do this :) Message m = createMessage(getName(),
     * "onRegistered", si); m.sender = Runtime.getInstance().getName();
     * broadcast(m);
     */
  }

  @Override
  public boolean preProcessHook(Message m) {
    // FIXME - problem with collisions of this service's methods
    // and dialog methods ?!?!?

    // broadcast
    broadcast(m);

    // if the method name is == to a method in the WebGui
    // process it
    if (methodSet.contains(m.method)) {
      // process the message like a regular service
      return true;
    }

    // otherwise send the message to the dialog with the senders name
    // broadcast(m);
    return false;
  }

  @Override
  public String publishConnect() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Connection publishConnect(Connection keys) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String publishDisconnect() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Status publishError() {
    // TODO Auto-generated method stub
    return null;
  }

  public String publishHide(String name) {
    return name;
  }

  public Panel publishPanel(String panelName) {

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
    start();
  }

  public boolean save() {
    return super.save();
  }

  /**
   * From UI events --to--&gt; MRL request to save panel data typically done
   * after user has changed or updated the UI in position, height, width, zIndex
   * etc.
   * 
   * If you need MRL changes of position or UI changes use publishPanel to
   * remotely control UI
   * 
   * @param panel - the panel which has been moved or resized
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
  public void sendRemote(String key, Message msg) throws URISyntaxException {
    // TODO Auto-generated method stub

  }

  @Override
  public void sendRemote(URI key, Message msg) {
    // TODO Auto-generated method stub

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
    invoke("publishPanel", panel);
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

      if (port == null) {
        port = 8888;
      }

      // Broadcaster b = broadcasterFactory.get();
      // a session "might" be nice - but for now we are stateless
      // SessionSupport ss = new SessionSupport();

      if (nettosphere != null && nettosphere.isStarted()) {
        // is running
        info("{} currently running on port {} - stop first, then start");
        return;
      }

      nettosphere = new Nettosphere.Builder().config(getConfig().build()).build();
      sleep(1000); // needed ?

      try {
        nettosphere.start();
      } catch (Exception e) {
        log.error("starting nettosphere failed", e);
      }

      broadcastFactory = nettosphere.framework().getBroadcasterFactory();
      // get default boadcaster
      broadcaster = broadcastFactory.get("/*");

      log.info("WebGui {} started on port {}", getName(), port);
      // get all instances

      // we want all onState & onStatus events from all services
      ServiceEnvironment se = Runtime.getLocalServices();
      for (String name : se.serviceDirectory.keySet()) {
        ServiceInterface si = se.serviceDirectory.get(name);
        onRegistered(si);
      }

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
      Logging.logError(e);
    }
  }

  public void startBrowser(String URL) {
    BareBonesBrowserLaunch.openURL(String.format(URL, port));
  }

  public void startService() {
    super.startService();
    // extract all resources
    // if resource directory exists - do not overwrite !
    // could whipe out user mods
    try {
      extract();
    } catch (Exception e) {
      Logging.logError(e);
    }

    start();
  }

  public void stop() {
    if (nettosphere != null) {

      log.info("stopping nettosphere");
      // Must not be called from a I/O-Thread to prevent deadlocks!
      (new Thread("stopping nettophere") {
        public void run() {
          /*
           * nettosphere.framework().removeAllAtmosphereHandler();
           * nettosphere.framework().resetStates();
           * nettosphere.framework().destroy();
           */
          nettosphere.stop();
        }
      }).start();
      sleep(1000);
    }
  }

  public void stopService() {
    super.stopService();
    stop();
  }

  // FIXME

  /**
   * UseLocalResources determines if references to JQuery JavaScript library are
   * local or if the library is linked to using content delivery network.
   * Default (false) is to use the CDN
   *
   * @param useLocalResources - true uses local resources fals uses cdn
   */
  public void useLocalResources(boolean useLocalResources) {
    this.useLocalResources = useLocalResources;
  }
  
  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {

      // Double level = Runtime.getBatteryLevel();
      // log.info("" + level);

      /*
       * VirtualArduino virtual = (VirtualArduino)Runtime.start("virtual",
       * "VirtualArduino"); virtual.connect("COM5");
       * 
       * Runtime.start("python", "Python");
       */
      // Runtime.start("arduino", "Arduino");
      // Runtime.start("srf05", "UltrasonicSensor");
      // Runtime.setRuntimeName("george");
      WebGui webgui = (WebGui)Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(true);
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
