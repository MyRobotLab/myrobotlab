package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequest.Body;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;
import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecFactory;
import org.myrobotlab.codec.Encoder;
import org.myrobotlab.codec.MethodCache;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.Zip;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.StatusLevel;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.Connection;
//import org.myrobotlab.service.WebGUI3.Error;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
//import org.myrobotlab.webgui.WebGUIServlet;
import org.slf4j.Logger;

//@ManagedService(path = "/api")
//@ManagedService(path = "/snake")

/**
 * 
 * WebGUI - This service is the AngularJS based GUI
 *
 */
public class WebGUI extends Service implements AuthorizationProvider, Gateway, Handler {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WebGUI.class);

	Integer port = 8888;

	transient Nettosphere nettosphere;
	transient Broadcaster broadcaster;
	transient BroadcasterFactory broadcastFactory;

	public String root = "root";
	boolean useLocalResources = false;
	boolean autoStartBrowser = true;

	public String startURL = "http://localhost:%d/index.html";

	// FIXME might need to change to HashMap<String, HashMap<String,String>> to
	// add
	// client session
	public HashMap<String, String> servicePanels = new HashMap<String, String>();

	// FIXME - shim for Shoutbox
	// deprecate ???
	public static class WebMsg {
		String clientid;
		// socket
		Message msg;
	}

	// SHOW INTERFACE
	// FIXME - allowAPI1(true|false)
	// FIXME - allowAPI2(true|false)
	// FIXME - allow Protobuf/Thrift/Avro
	// FIXME - NO JSON ENCODING SHOULD BE IN THIS FILE !!!

	public WebGUI(String n) {
		super(n);
	}

	// ================ Gateway begin ===========================

	@Override
	public void addConnectionListener(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connect(String uri) throws URISyntaxException {
		// TODO Auto-generated method stub

	}

	@Override
	public HashMap<URI, Connection> getClients() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Connection> getConnections(URI clientKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrefix(URI protocolKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection publishNewConnection(Connection keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendRemote(String key, Message msg) throws URISyntaxException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendRemote(URI key, Message msg) {
		// TODO Auto-generated method stub

	}

	// ================ Gateway end ===========================

	// ================ AuthorizationProvider begin ===========================

	@Override
	public boolean allowExport(String serviceName) {
		// TODO Auto-generated method stub
		return false;
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

	// ================ AuthorizationProvider end ===========================

	// ================ Broadcaster begin ===========================
	public void broadcast(Message msg) {
		try {
			if (broadcaster != null) {
				Codec codec = CodecFactory.getCodec(Encoder.MIME_TYPE_MESSAGES);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				codec.encode(bos, msg);
				bos.close();
				broadcaster.broadcast(new String(bos.toByteArray())); // wtf
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	// ================ Broadcaster end ===========================

	public void startService() {
		super.startService();
		// Broadcaster b = broadcasterFactory.get();
		// a session "might" be nice - but for now we are stateless
		// SessionSupport ss = new SessionSupport();

		// extract all resources
		// if resource directory exists - do not overwrite !
		// could whipe out user mods
		FileIO.extractResources();

		Config.Builder configBuilder = new Config.Builder();
		configBuilder
				/*
				 * did not work :( .resource(
				 * "jar:file:/C:/mrl/myrobotlab/dist/myrobotlab.jar!/resource")
				 * .resource(
				 * "jar:file:/C:/mrl/myrobotlab/dist/myrobotlab.jar!/resource/WebGUI"
				 * )
				 */

				// for debugging
				.resource("./src/resource/WebGUI")
				.resource("./src/resource")
				// for runtime - after extractions
				.resource("./resource/WebGUI")
				.resource("./resource")

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
				.initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true").port(port).host("0.0.0.0").build();

		Nettosphere s = new Nettosphere.Builder().config(configBuilder.build()).build();
		s.start();

		broadcastFactory = s.framework().getBroadcasterFactory();
		// get default boadcaster
		broadcaster = broadcastFactory.get("/*");

		log.info("WebGUI2 {} started on port {}", getName(), port);

		if (autoStartBrowser) {
			log.info("auto starting default browser");
			BareBonesBrowserLaunch.openURL(String.format(startURL, port));
		}

		// get all instances

		// we want all onState & onStatus events from all services
		ServiceEnvironment se = Runtime.getLocalServices();
		for (String name : se.serviceDirectory.keySet()) {
			ServiceInterface si = se.serviceDirectory.get(name);
			onRegistered(si);
		}

		// additionally we will want onState & onStatus events from all services
		// from all new services which were created "after" the webgui
		// so susbcribe to our Runtimes methods of interest
		Runtime runtime = Runtime.getInstance();
		subscribe(runtime.getName(), "registered");
		subscribe(runtime.getName(), "released");
	}

	public void onRegistered(ServiceInterface si) {
		// new service
		// subscribe to the status events
		subscribe(si.getName(), "publishStatus");
		subscribe(si.getName(), "publishState");
		
		// for distributed Runtimes
		if (si.isRuntime()){
			subscribe(si.getName(), "registered");
		}

		// broadcast it too
		// repackage message
		/*
		 * don't need to do this :) Message m = createMessage(getName(),
		 * "onRegistered", si); m.sender = Runtime.getInstance().getName();
		 * broadcast(m);
		 */
	}

	@Override
	public String[] getCategories() {
		return new String[] { "display" };
	}

	@Override
	public String getDescription() {
		return "web enabled gui";
	}

	public Map<String, String> getHeadersInfo(HttpServletRequest request) {

		Map<String, String> map = new HashMap<String, String>();

		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key.toLowerCase(), value);
		}

		return map;
	}

	/**
	 * With a single method Atmosphere does so much !!! It sets up the
	 * connection, possibly gets a session, turns the request into something
	 * like a HTTPServletRequest, provides us with input & output streams - and
	 * manages all the "long polling" or websocket upgrades on its own !
	 * 
	 * Atmosphere Rocks !
	 */
	@Override
	public void handle(AtmosphereResource r) {

		Codec codec = null;
		OutputStream out = null;
		String httpMethod = r.getRequest().getMethod();

		try {

			AtmosphereRequest request = r.getRequest();
			AtmosphereResponse response = r.getResponse();
			InputStream in = r.getRequest().getInputStream();
			out = r.getResponse().getOutputStream();

			r.setBroadcaster(broadcaster);

			Map<String, String> headers = getHeadersInfo(request);

			if (headers.containsKey("content-type")) {
				log.info(String.format(String.format("in encoding : content-type %s", headers.get("content-type"))));
			}
			if (headers.containsKey("accept")) {
				log.info(String.format(String.format("out encoding : accept %s", headers.get("accept"))));
			}

			// FIXME reconstruct REST request & log
			String pathInfo = request.getPathInfo();
			String[] parts = null;

			// GET vs POST - post assumes low-level messaging
			// GET is high level synchronous
			// String httpMethod = request.getMethod();

			// get default encoder
			codec = CodecFactory.getCodec(Encoder.MIME_TYPE_MESSAGES);

			if (pathInfo != null) {
				parts = pathInfo.split("/");
			}

			if (parts == null || parts.length < 3) {
				response.addHeader("Content-Type", codec.getMimeType());
				handleError(httpMethod, out, codec, "API", "http(s)://{host}:{port}/api/{api-type}/{Object}/{Method}");
				return;
			}

			// set specified encoder

			String apiTypeKey = parts[2];

			if ("messages".equals(apiTypeKey)) {
				if (!r.isSuspended()) {
					r.suspend();
				}
			}

			String codecMimeType = Encoder.getKeyToMimeType(apiTypeKey);
			if (!codecMimeType.equals(codec.getMimeType())) {
				// request to switch codec types on
				codec = CodecFactory.getCodec(codecMimeType);
			}

			/*
			 * interesting .... switch (r.transport()) { case JSONP: case
			 * LONG_POLLING: event.getResource().resume(); break; case
			 * WEBSOCKET: case STREAMING: res.getWriter().flush(); break; }
			 */

			response.addHeader("Content-Type", codec.getMimeType());

			ArrayList<MethodEntry> info = null;
			if (parts.length == 3) {
				// ========================================
				// POST http://{host}:{port}/api/messages
				// ========================================
				// posted message api
				if ("messages".equals(parts[2]) && "POST".equals(httpMethod)) {
					Body body = request.body();
					processMessageAPI(codec, body);
					return;
				}

				// ServiceEnvironment env = Runtime.getLocalServices();
				HashMap<URI, ServiceEnvironment> env = Runtime.getEnvironments();

				// FIXME - getEnvironments()
				// FIXME - relfect with javdoc info log.info("inspecting");
				respond(out, codec, "getLocalServices", env);
				return;
			} else if (parts.length == 4) {
				// *** /api/messages/runtime ***
				ServiceInterface si = Runtime.getService(parts[3]);
				Method[] methods = si.getDeclaredMethods();
				respond(out, codec, "getDeclaredMethods", si);
				return;
			}

			String name = parts[2];

			ServiceInterface si = Runtime.getService(name);
			Class<?> clazz = si.getClass();
			Class<?>[] paramTypes = null;
			Object[] params = new Object[0];

			// FIXME - decode body assumption is that its in an ARRAY
			// MUST MAKE DECISION ON PRECEDENCE
			// String body = convertStreamToString(in);
			int cl = request.getContentLength();
			byte[] body = null;

			if (cl > 0) {
				body = new byte[cl];
				int bytesRead = in.read(body);
				if (bytesRead != cl) {
					handleError(httpMethod, out, codec, "BadInput", String.format("client said it would send %d bytes but only %d were read", cl, bytesRead));
					return;
				}
			}

			// FIXME - sloppy to convert to String here - should be done in the
			// Encoder (if that happens)
			String b = null;
			if (body != null) {
				b = new String(body);
			}
			log.info(String.format("POST Body [%s]", b));

			// FIXED ME
			// 1. get method "name" and incoming ordinal - generate method
			// signature (optional)- check method cache
			// 2. "attempt" to get method
			// 3. (optional) - if failure - scan methods - find one with
			// signature - cache it - call it
			String methodName = String.format("%s", parts[3]);

			// decoded array of encoded parameters
			Object[] encodedArray = new Object[0];

			// BODY - PARAMETERS
			if (cl > 0) {
				// REQUIREMENT must be in an encoded array - even binary
				// 1. decode the array
				// 2. will need to decode contents of each parameter later based
				// on signature of reflected method

				encodedArray = codec.decodeArray(b);

				// WE NOW HAVE ORDINAL

				// URI - PARAMETERS - TODO - define added encoding spec > 5 ?
			} else if (parts.length > 4) {
				// REQUIREMENT must be in an encoded array - even binary
				// 1. array is URI /
				// 2. will need to decode contents of each parameter later based
				// on signature of reflected method

				// get params from uri - its our array
				// difference is initial state regardless of encoding we are
				// guaranteed the URI parts are strings
				// encodedArray = new Object[parts.length - 3];
				encodedArray = new Object[parts.length - 4];

				for (int i = 0; i < encodedArray.length; ++i) {
					String result = URLDecoder.decode(parts[i + 4], "UTF-8");
					encodedArray[i] = result;
				}

				// WE NOW HAVE ORDINAL
			}

			// FETCH AND MERGE METHOD - we have ordinal count now - but NOT the
			// decoded
			// parameters
			// NOW HAVE ORDINAL - fetch the method with its types
			paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), methodName, encodedArray.length);
			// WE NOW HAVE ORDINAL AND TYPES
			params = new Object[encodedArray.length];

			// DECODE AND FILL THE PARAMS
			for (int i = 0; i < params.length; ++i) {

				params[i] = codec.decode(encodedArray[i], paramTypes[i]);
			}

			Method method = clazz.getMethod(methodName, paramTypes);

			// NOTE --------------
			// strategy of find correct method with correct parameter types
			// "name" is the strongest binder - but without a method cache we
			// are condemned to scan through all methods
			// also without a method cache - we have to figure out if the
			// signature would fit with instanceof for each object
			// and "boxed" types as well

			// best to fail - then attempt to resolve through scanning through
			// methods and trying types - then cache the result

			// FIXME - this is duplicated in processMessageAPI :(
			if (si.isLocal()) {
				log.info("{} is local", name);
				Object ret = method.invoke(si, params);
				respond(out, codec, method.getName(), ret);
			} else {
				log.info("{} is is remote", name);
				Message msg = createMessage(name, method.getName(), params);
				out(msg);
			}

			MethodCache.cache(clazz, method);

			// FIXME - there is no content mime-type being set !!! this would
			// depend on codec being used
			// FIXME - currently a keyword - "json" internally defines the codec
			// - getMimeType !!

		} catch (Exception e) {
			handleError(httpMethod, out, codec, e);
		}

	}

	public void processMessageAPI(Codec codec, Body body) throws Exception {

		// first decoding will give you an array of types in msg.data[]
		// but they are un-coerced - we need the method signature candidate
		// to determine what we should coerce them into
		Message msg = Encoder.fromJson(body.asString(), Message.class);
		if (msg == null) {
			log.error(String.format("msg is null %s", body.asString()));
			return;
		}
		msg.sender = getName();
		log.info(String.format("got msg %s", msg.toString()));

		// out(msg);

		// get the service
		ServiceInterface si = Runtime.getService(msg.name);
		if(si == null){
			error("could not get service %s for msg %s", msg.name, msg);
			return;
		}
		Class<?> clazz = si.getClass();

		Class<?>[] paramTypes = null;
		Object[] params = new Object[msg.data.length];
		// decoded array of encoded parameters
		// FIXME - not "really" correct !
		Object[] encodedArray = msg.data;// new Object[msg.data.length];

		// encodedArray = codec.decodeArray(b);

		paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), msg.method, encodedArray.length);

		StringBuffer sb = new StringBuffer(String.format("(%s)%s.%s(", clazz.getSimpleName(), msg.name, msg.method));
		for (int i = 0; i < paramTypes.length; ++i) {
			if (i != 0) {
				sb.append(",");
			}
			sb.append(paramTypes[i].getSimpleName());
		}
		sb.append(")");
		log.info(sb.toString());

		// WE NOW HAVE ORDINAL AND TYPES
		params = new Object[encodedArray.length];

		// DECODE AND FILL THE PARAMS
		for (int i = 0; i < params.length; ++i) {
			params[i] = codec.decode(encodedArray[i], paramTypes[i]);
		}

		// FIXME FIXME FIXME !!!!
		// Service.invoke needs to use method cach BUT - internal queues HAVE
		// type information
		// AND decoded json DOES NOT - needs to be optimized such that it knows
		// the encoding
		// before using the method cache - and the "hint" determins
		// getBestCanidate !!!!

		Method method = clazz.getMethod(msg.method, paramTypes);

		// NOTE --------------
		// strategy of find correct method with correct parameter types
		// "name" is the strongest binder - but without a method cache we
		// are condemned to scan through all methods
		// also without a method cache - we have to figure out if the
		// signature would fit with instanceof for each object
		// and "boxed" types as well

		// best to fail - then attempt to resolve through scanning through
		// methods and trying types - then cache the result

		// FIXME - not good - using my thread to execute another services
		// method and put its return on the the services out queue :P
		if (si.isLocal()) {
			log.info("{} is local", si.getName());

			Object retobj = method.invoke(si, params);

			// FIXME - Is this how to support synchronous ?
			// What does this mean ?
			// respond(out, codec, method.getName(), ret);

			si.out(msg.method, retobj);
		} else {
			log.info("{} is remote", si.getName());
			// send(msg.name, msg.method, msg.data);
			send(msg.name, msg.method, params);
			// out(msg); LETHAL !
		}

		MethodCache.cache(clazz, method);
	}

	// FIXME !!! - ALL CODECS SHOULD HANDLE MSG INSTEAD OF OBJECT !!!
	// THEN YOU COULD ALSO HAVE urlToMsg(URL url)
	// "lower layer encoders can strip down to the data" !!!
	public void respond(OutputStream out, Codec codec, String method, Object ret) throws Exception {
		// getName() ? -> should it be AngularJS client name ?
		Message msg = createMessage(getName(), Encoder.getCallBackName(method), ret);
		codec.encode(out, msg);
	}

	public void handleError(String httpMethod, OutputStream out, Codec codec, Throwable e) {
		handleError(httpMethod, out, codec, e.getMessage(), Logging.logError(e));
	}

	// FIXME - APP_EVENT_LOG for normalizing (if available)
	public void handleError(String httpMethod, OutputStream out, Codec codec, String key, String detail) {
		try {
			log.error(detail);
			Status error = new Status(getName(), StatusLevel.ERROR, key, detail);
			if ("POST".equals(httpMethod)) {
				broadcast(createMessage(getName(), "onStatus", error));
			} else {
				respond(out, codec, "handleError", error);
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void extract() {
		try {
			Zip.extractFromFile("./myrobotlab.jar", "root", "resource/WebGUI");
		} catch (IOException e) {
			error(e);
		}
	}

	/**
	 * - use the service's error() pub sub return public void handleError(){
	 * 
	 * }
	 */

	/**
	 * determines if references to JQuery JavaScript library are local or if the
	 * library is linked to using content delivery network. Default (false) is
	 * to use the CDN
	 * 
	 * @param b
	 */
	public void useLocalResources(boolean useLocalResources) {
		this.useLocalResources = useLocalResources;
	}

	public void autoStartBrowser(boolean autoStartBrowser) {
		this.autoStartBrowser = autoStartBrowser;
	}

	@Override
	public boolean preProcessHook(Message m) {
		// FIXME - problem with collisions of this service's methods
		// and dialog methods ?!?!?

		// broadcast
		broadcast(m);

		// if the method name is == to a method in the WebGUI
		// process it
		if (methodSet.contains(m.method)) {
			// process the message like a regular service
			return true;
		}

		// otherwise send the message to the dialog with the senders name
		// broadcast(m);
		return false;
	}

	public void savePanel(String name, String panel) {
		servicePanels.put(name, panel);
	}

	public String loadPanel(String name) {
		if (servicePanels.containsKey(name)) {
			return servicePanels.get(name);
		}
		return null;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			
			//Runtime.start("python", "Python");			
			// Runtime.start("gui", "GUIService");
			//RemoteAdapter remote = (RemoteAdapter)Runtime.start("remote","RemoteAdapter");
			//remote.startListening();
			// remote.setDefaultPrefix("x-");
			//remote.setDefaultPrefix("");
			Runtime.start("python", "Python");
			Runtime.start("webgui", "WebGUI");
			//Runtime.start("python", "Python");
			//Runtime.start("myo", "MyoThalmic");
			// remote.connect("tcp://127.0.0.1:6767");
			
			//Runtime.start("macgui", "GUIService");
			
			// MyoThalmic myo = (MyoThalmic) Runtime.start("myo", "MyoThalmic");
			//myo.connect();
			
			// myo.addMyoDataListener(python);

			// Runtime.start("python", "Python");
			// Runtime.start("remote", "RemoteAdapter");
			// Runtime.start("arduino", "Arduino");// Runtime.start("clock01",
			// "Clock"); Runtime.start("clck3", "Clock");
			// Runtime.start("gui", "GUIService");

			// webgui.extract();
			/*
			 * Runtime.start("clck", "Clock"); Runtime.start("clck2", "Clock");
			 * Runtime.start("clck3", "Clock");
			 * 
			 * Runtime.start("clck", "Clock"); Runtime.start("clck2", "Clock");
			 * Runtime.start("clck3", "Clock");
			 */

			/*
			 * Message msg = webgui.createMessage("runtime", "start", new
			 * Object[]{"arduino", "Arduino"}); String json =
			 * Encoder.toJson(msg); log.info(json); // Runtime.start("gui",
			 * "GUIService"); log.info(json);
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}
	}
}
