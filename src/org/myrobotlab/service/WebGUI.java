package org.myrobotlab.service;

import java.util.HashMap;
import java.util.HashSet;

import org.java_websocket.WebSocket;
import org.myrobotlab.fileLib.Zip;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.security.BasicSecurity;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.webgui.WSServer;
import org.myrobotlab.webgui.WSServer.WSMsg;
import org.slf4j.Logger;

public class WebGUI extends Service implements AuthorizationProvider {

	// import javax.xml.transform.Transformer;

	// TODO - important !!!
	// api's XML, Text, HTML or other formatting of return type needs to be
	// encoded as part of the URI "BEFORE" the method request & paramters !!!!
	// e.g. http://127.0.0.1:7777/api/xml/services/arduino01/digitalWrite/13/1
	// Consistency is important to maintain the REST API !!!!
	// Jenkins did it right -
	// https://wiki.jenkins-ci.org/display/JENKINS/Remote+access+API
	// http://server/jenkins/crumbIssuer/api/xml (or /api/json) !
	// HA !! realize that there are 2 encodings !!! inbound and return
	// VERY SIMPLE BUT IT MUST BE CONSISTENT !!!
	// http://mrl:7777/api/<inbound format>/<outbound
	// format>/<method>/<parameters>/
	// http://mrl:7777/api/<rest>/<xml>/<method>/<params> !!! inbound is rest -
	// return format is xml !!!
	// http://mrl:7777/api/<soap>/<xml>/<method>/<params> !!! inbound is soap -
	// return format is xml !!!
	// http://mrl:7777/api/<soap>/<soap>/<method>/<params> !!! inbound is soap -
	// return format is soap !!!
	// http://mrl:7777/api/<resource>/<json>/<method>/<params> !!! inbound is
	// resource request - return format is json !!!
	// default is /<rest>/<gson>/<method>/<params>

	// dropped - NanoHTTPD in favor of websockets with user call-back
	// http://nanohttpd.com/

	// FIXME !!! SINGLE WebServer/Socket server - capable of long polling
	// fallback
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WebGUI.class);

	public Integer port = 7777;

	boolean autoStartBrowser = true;

	boolean useLocalResources = true;

	public String startURL = "http://127.0.0.1:%d/index.html";

	public String root = "resource";

	public int messages = 0;

	transient WSServer wss;

	public HashMap<String, String> clients = new HashMap<String, String>();

	private HashSet<String> allowMethods = new HashSet<String>();

	private HashSet<String> excludeMethods = new HashSet<String>();

	private HashSet<String> allowServices = new HashSet<String>();

	private HashSet<String> excludeServices = new HashSet<String>();

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		WebGUI webgui = (WebGUI) Runtime.start("webgui", "WebGUI");

		//webgui.test();
		// webgui.useLocalResources(true);
		// webgui.autoStartBrowser(false);
		// Runtime.createAndStart("webgui", "WebGUI");
		// webgui.useLocalResources(true);

	}

	public WebGUI(String n) {
		super(n);
		// first message web browser client is getRegistry
		// so we want it routed back here to deliver to client
		subscribe(Runtime.getInstance().getIntanceName(), "getRegistry");
	}

	public void addConnectListener(Service service) {
		addListener("publishConnect", service.getName(), "onConnect", WebSocket.class);
	}

	public void addDisconnectListener(Service service) {
		addListener("publishDisconnect", service.getName(), "onDisconnect", WebSocket.class);
	}

	public boolean addUser(String username, String password) {
		return BasicSecurity.addUser(username, password);
	}

	public void addWSMsgListener(Service service) {
		addListener("publishWSMsg", service.getName(), "onWSMsg", WSMsg.class);
	}

	public boolean allowDirectMessaging(boolean b) {
		wss.allowDirectMessaging(b);
		return b;
	}

	@Override
	public boolean allowExport(String serviceName) {
		// TODO Auto-generated method stub
		return false;
	}

	public void allowMethod(String method) {
		allowMethods.add(method);
	}

	public void allowREST(Boolean b) {
		if (wss != null) {
			wss.allowREST(b);
		}
	}

	public void autoStartBrowser(boolean autoStartBrowser) {
		this.autoStartBrowser = autoStartBrowser;
	}

	/**
	 * expanding of all resource data from WebGUI onto the file system so that
	 * it may be customized by the user
	 */
	public void customize() {
		try {
			Zip.extractFromFile("./myrobotlab.jar", "./resource", "resource");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public String[] getCategories() {
		return new String[] { "display", "control" };
	}

	@Override
	public String getDescription() {
		return "The new web enabled GUIService 2.0 !";
	}

	public Integer getPort() {
		return port;
	}

	@Override
	public boolean isAuthorized(HashMap<String, String> security, String serviceName, String method) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAuthorized(Message msg) {
		String method = msg.method;
		String service = msg.name;

		if (allowMethods.size() > 0 && !allowMethods.contains(method)) {
			return false;
		}

		if (excludeMethods.size() > 0 && excludeMethods.contains(method)) {
			return false;
		}

		return true;
	}

	public void openURL(String url) {
		BareBonesBrowserLaunch.openURL(url);
	}

	/**
	 * called by the framework pre process of messages so that data can be
	 * routed back to the correct subcomponent and control of the WebGUI can
	 * still be maintained like a "regular" service
	 * 
	 */
	@Override
	public boolean preProcessHook(Message m) {
		// FIXME - problem with collisions of this service's methods
		// and dialog methods ?!?!?

		// if the method name is == to a method in the GUIService
		if (methodSet.contains(m.method)) {
			// process the message like a regular service
			return true;
		}

		// otherwise send the message to the dialog with the senders name
		sendToAll(m);
		return false;
	}

	public WebSocket publishConnect(WebSocket conn) {
		return conn;
	}

	public WebSocket publishDisconnect(WebSocket conn) {
		return conn;
	}

	// ============== security begin =========================
	// FIXME - this will have to be keyed by the service name
	// if the global datastructures are to be in Security

	// specifically for a gateway
	// this interface should be incorporated into Security Service

	// interesting - regular expresion matching .. its a combined key !
	// Service.method or perhaps sender.Service.method ?

	public WSMsg publishWSMsg(WSMsg wsmsg) {
		return wsmsg;
	}

	public void restart() {
		stop();
		startWebSocketServer(port);
	}

	/**
	 * sends JSON encoded MyRobotLab Message to all clients currently connected
	 * through web sockets
	 * 
	 * @param msg
	 *            message to broadcast
	 */
	public void sendToAll(Message msg) {
		++messages;
		// String json = Encoder.toJson(msg, Message.class); //toJson(msg);
		// RECENTLY CHANGED
		String json = Encoder.toJson(msg);
		log.debug(String.format("webgui ---to---> all clients [%s]", json));
		if (messages % 500 == 0) {
			info(String.format("sent %d messages to %d clients", messages, wss.connections().size())); // TODO
																										// modulus
		}

		if (json != null) {
			wss.sendToAll(json);
		} else {
			log.error(String.format("toJson %s.%s is null", msg.name, msg.method));
		}
	}

	public Integer setPort(Integer port) {
		this.port = port;
		return port;
	}

	/**
	 * starts and web socket server, auto launches browser if
	 * autoStartBrowser=true
	 * 
	 * @return true if both servers started
	 */
	public boolean start() {
		// TODO - make sure re-entrant
		boolean result = startWebSocketServer(port);
		log.info("using local resources is {}", useLocalResources);
		log.info("starting web socket server on port {} result is {}", port, result);
		if (autoStartBrowser) {
			log.info("auto starting default browser");
			BareBonesBrowserLaunch.openURL(String.format(startURL, port));
		}
		if (!result) {
			warn("could not start properly");
		}
		return result;
	}

	@Override
	public void startService() {
		super.startService();
		start();
	}

	/**
	 * @param port
	 *            - port to start server on default is 7778
	 * @return - true if successfully started
	 */
	public boolean startWebSocketServer(Integer port) {
		try {

			this.port = port;

			if (wss != null) {
				wss.stop();
			}

			wss = new WSServer(this, port);
			wss.start();
			return true;
		} catch (Exception e) {
			Logging.logError(e);
		}

		return false;
	}

	public void stop() {
		try {
			if (wss != null) {
				wss.stop();
				wss = null;
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public void stopService() {
		super.stopService();
		stop();
	}

	// ============== security end =========================

	@Override
	public Status test() {
		Status status = super.test();

		try {

			// test re-entrant starting
			WebGUI webgui = (WebGUI) Runtime.start(getName(), "WebGUI");

		} catch (Exception e) {
			status.addError(e);
		}

		return status;
	}

	/**
	 * @return whether instance is using CDN for delivery of JavaScript
	 *         libraries to browser
	 */
	public boolean useLocalResources() {
		return useLocalResources;
	}

	/**
	 * determines if references to JQuery JavaScript library are local or if the
	 * library is linked to using content delivery network. Default (false) is
	 * to use the CDN
	 * 
	 * @param b
	 */
	public void useLocalResources(boolean b) {
		useLocalResources = b;
	}

}
