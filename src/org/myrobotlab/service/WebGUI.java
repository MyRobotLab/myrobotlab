package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
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

public class WebGUI extends Service implements AuthorizationProvider, Gateway, Handler {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WebGUI.class);

	Integer port = 7777;
	transient Nettosphere nettosphere;
	transient Broadcaster broadcaster;
	transient BroadcasterFactory broadcastFactory;

	public String root = "root";
	boolean useLocalResources = false;
	boolean autoStartBrowser = true;

	public String startURL = "http://127.0.0.1:%d/index.html";

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
		broadcaster.broadcast(msg); // wtf
	}

	// ================ Broadcaster end ===========================

	public void startService() {
		super.startService();
		// Broadcaster b = broadcasterFactory.get();

		// a session "might" be nice - but for now we are stateless
		// SessionSupport ss = new SessionSupport();

		Config.Builder configBuilder = new Config.Builder();
		configBuilder
				// .resource("C:\\tools\\myrobotlab-WebGUI\\src\\resource\\WebGUI")

				// .resource("./root")
				.resource("./src/resource/WebGUI")
				//.resource("./src/resource/MaVo_WebGUI")
				.resource("./src/resource")
				//.resource("./src/resource")
				// .resource("./rest") SHOULD I DO THIS ?
				// .resource(this)
				// Support 2 APIs
				// REST - http://host/object/method/param0/param1/...
				// synchronous DO NOT SUSPEND
				.resource("/api", this)
				// TODO - go beyond Servlets
				// .resource("/api", WebGUIServlet.class)

				// For mvn exec:java
				// .resource("./src/main/resources")

				// For running inside an IDE
				// .resource("./nettosphere-samples/games/src/main/resources")

				// if Jetty is in the classpath it will use it by default - we
				// want to use Netty
				.initParam("org.atmosphere.cpr.asyncSupport", "org.atmosphere.container.NettyCometSupport")
				.initParam(ApplicationConfig.SCAN_CLASSPATH, "false")
				.initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true").port(port).host("0.0.0.0").build();
		// .host("127.0.0.1").build();
		Nettosphere s = new Nettosphere.Builder().config(configBuilder.build()).build();

		s.start();

		broadcastFactory = s.framework().getBroadcasterFactory();
		// get default boadcaster
		broadcaster = broadcastFactory.get("/*");

		log.info("WebGUI2 {} started on port {}", getName(), port);

		// BufferedReader br = new BufferedReader(new
		// InputStreamReader(System.in));
		if (autoStartBrowser) {
			log.info("auto starting default browser");
			BareBonesBrowserLaunch.openURL(String.format(startURL, port));
		}

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

		try {

			AtmosphereRequest request = r.getRequest();
			AtmosphereResponse response = r.getResponse();
			InputStream in = r.getRequest().getInputStream();
			out = r.getResponse().getOutputStream();

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
			String httpMethod = request.getMethod();
			
			// get default encoder
			codec = CodecFactory.getCodec(Encoder.MIME_TYPE_MESSAGES);

			if (pathInfo != null) {
				parts = pathInfo.split("/");
			}

			if (parts == null || parts.length < 3) {
				response.addHeader("Content-Type", codec.getMimeType());
				handleError(out, codec, "API", "http(s)://{host}:{port}/api/{api-type}/{Object}/{Method}");
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
			 interesting ....
			 switch (r.transport()) {
					case JSONP:
					case LONG_POLLING:
					      event.getResource().resume();
					    break;
					case WEBSOCKET:
					case STREAMING:
					  res.getWriter().flush();
					break;
					}
			 */

			response.addHeader("Content-Type", codec.getMimeType());

			ArrayList<MethodEntry> info = null;
			if (parts.length == 3) {
				// *** /api/messages **

				// starndard JSON asynchronous message with POST'ed parameters
				if ("messages".equals(parts[2]) && "POST".equals(httpMethod)){
					Body body = request.body();
					Message msg = Encoder.fromJson(body.asString(), Message.class);
					msg.sender = getName();
					log.info(String.format("got msg %s", msg.toString()));
					out(msg);
					return;
				}

				
				ServiceEnvironment si = Runtime.getLocalServices();
				
				/*
				 * TODO - relfect with javdoc info log.info("inspecting");
				 * 
				 * Method[] methods = clazz.getDeclaredMethods(); info = new
				 * ArrayList<MethodInfo>(); for (Method method : methods) { if
				 * (!filter.contains(method.getName())) { MethodInfo m = new
				 * MethodInfo(); m.name = method.getName(); Class<?>[] types =
				 * method.getParameterTypes(); m.parameterTypes = new
				 * String[types.length]; for (int i = 0; i < types.length; ++i)
				 * { m.parameterTypes[i] = types[i].getSimpleName() }
				 * m.returnType = method.getReturnType().getSimpleName(); //
				 * NULL // ? info.add(m); } }
				 */

				respond(out, codec, "getLocalServices", si);
				return;
			} else if (parts.length == 4) {
				// *** /api/messages/runtime ***
				ServiceInterface si = Runtime.getService(parts[3]);
				Method[] methods = si.getDeclaredMethods();
				respond(out, codec, "getDeclaredMethods", si);
				return;
			}/*
			 * else if (parts.length > 3) { String serviceName = parts[2];
			 * String method = parts[3];
			 * 
			 * ServiceInterface si = Runtime.getService(serviceName);
			 * 
			 * // decode parameters String[] params = new String[parts.length -
			 * 4]; // <- this i "wrong" - a big assumption that they are
			 * "Strings" Object[] for (int i = 0; i < params.length; ++i){
			 * 
			 * }
			 * 
			 * }
			 */

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
					handleError(out, codec, "BadInput", String.format("client said it would send %d bytes but only %d were read", cl, bytesRead));
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
					String result = java.net.URLDecoder.decode(parts[i + 4], "UTF-8");
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

			Object ret = method.invoke(si, params);
			respond(out, codec, method.getName(), ret);

			MethodCache.cache(clazz, method);

			// FIXME - there is no content mime-type being set !!! this would
			// depend on codec being used
			// FIXME - currently a keyword - "json" internally defines the codec
			// - getMimeType !!

		} catch (Exception e) {
			handleError(out, codec, e);
		}

	}

	// FIXME !!! - ALL CODECS SHOULD HANDLE MSG INSTEAD OF OBJECT !!!
	// THEN YOU COULD ALSO HAVE urlToMsg(URL url)
	// "lower layer encoders can strip down to the data" !!!
	public void respond(OutputStream out, Codec codec, String method, Object ret) throws Exception {
		// getName() ? -> should it be AngularJS client name ?
		Message msg = createMessage(getName(), Encoder.getCallBackName(method), ret);
		codec.encode(out, msg);
	}

	public void handleError(OutputStream out, Codec codec, Throwable e) {
		handleError(out, codec, e.getMessage(), Logging.logError(e));
	}

	// FIXME - APP_EVENT_LOG for normalizing (if available)
	public void handleError(OutputStream out, Codec codec, String key, String detail) {
		try {
			log.error(detail);
			Status error = new Status(getName(), StatusLevel.ERROR, key, detail);
			respond(out, codec, "handleError", error);
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

		// FIXME - collisions may exist
		// if the method name is == to a method in the GUIService
		if (methodSet.contains(m.method)) {
			// process the message like a regular service
			return true;
		}

		// otherwise send the message to the dialog with the senders name
		broadcast(m);
		return false;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			// Uri.
			// Uri myUri = Uri.parse("http://stackoverflow.com");

			WebGUI webgui = (WebGUI) Runtime.start("webgui", "WebGUI");
			// webgui.extract();
			/*
			Runtime.start("clck", "Clock");
			Runtime.start("clck2", "Clock");
			Runtime.start("clck3", "Clock");

			Runtime.start("clck", "Clock");
			Runtime.start("clck2", "Clock");
			Runtime.start("clck3", "Clock");
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
