package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequest.Body;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.Serializer;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;
import org.myrobotlab.fileLib.Zip;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.TypeConverter;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.service.interfaces.ServiceInterface;
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

		//Broadcaster b = broadcasterFactory.get();
		
		// a session "might" be nice - but for now we are stateless
		// SessionSupport ss = new SessionSupport();

		Config.Builder configBuilder = new Config.Builder();
		configBuilder
		.resource("C:\\tools\\myrobotlab-WebGUI\\src\\resource\\WebGUI")
		
				.resource("./root")
				
				// .resource("./rest")  SHOULD I DO THIS ?
				// .resource(this)
				// Support 2 APIs
				// REST - http://host/object/method/param0/param1/...  synchronous DO NOT SUSPEND
				.resource("/api", this)
				// For mvn exec:java
				// .resource("./src/main/resources")

				// For running inside an IDE
				// .resource("./nettosphere-samples/games/src/main/resources")
		
				.initParam(ApplicationConfig.SCAN_CLASSPATH, "false")
				.initParam(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true")
				.port(port).host("127.0.0.1").build();
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
		return "used as a general template";
	}

	/**
	 * With a single method Atmosphere does so much !!!
	 * It sets up the connection, possibly gets a session, turns the
	 * request into something like a HTTPServletRequest, provides us with 
	 * input & output streams - and manages all the "long polling" or websocket
	 * upgrades on its own !
	 * 
	 * Atmosphere Rocks !
	 */
	@Override
	public void handle(AtmosphereResource r) {
		OutputStream out = null;
		try {
			
			//Broadcaster b = event.broadcaster();
			//b.broadcast("{message:\"thats what she said\"}");
			//log.info("broadcaster from resource is {}", b);
			// r.getResponse().write("Hello World").write(" from Nettosphere").flushBuffer();

			AtmosphereResourceEvent event = r.getAtmosphereResourceEvent();
			AtmosphereRequest request = r.getRequest();
			AtmosphereResponse response = r.getResponse();
			Serializer serializer = r.getSerializer();
			
			out = r.getResponse().getOutputStream();
			InputStream in = r.getRequest().getInputStream();
			Body body = request.body();
			String data = body.asString();

			// request info
			String uuid = r.uuid();
			String pathInfo = request.getPathInfo();
			String trailingCharacter = null;
			if (pathInfo != null) {
					
			}
			
			int length = request.getContentLength();
			String httpMethod = request.getMethod();
			
			log.info(String.format("%s client %s length %d pathInfo %s", httpMethod, uuid, length, pathInfo));
			log.info(String.format("data %s", data));

			
			// See - http://myrobotlab.org/content/myrobotlab-web-api for details
			
			// FORMAT is http://host/api(/encoding=JSON/decoding=JSON/)/{api-type}/{service name}/{method}/{param0}/{param1}
			
			// API 1 - synchronous - not suspended - default encoding & decoding are JSON
			// GET http://host/api/services/{service name}/{method}/{param0}/{param1}

			// API 2 - asynchronous - is suspended (connection remains open) - default encoding & decoding are JSON
			// POST http://host/api/messages
			
			
			// TODO - implement non-default non-JSON encodings - e.g. Thrift/Avro ?
			// String encoding = "json";
			// String decoding = "json";
			
			String[] parts = pathInfo.split("/");
			// FIXME - min size check - with response showing expected format
			// KINDER-GENTLER - fewer parts returns possible selections e.g.
			// if no apiType is specified - tell them what it could be (services | messages)
			if (parts.length < 3){
				throw new IOException("http://host:port/api/{api-type}/...  api-type must be (services | messages), please refer to http://myrobotlab.org/content/myrobotlab-web-api for details");
			}
			
			String apiType = parts[2];
		
			if ("messages".equals(apiType)){
				
				// suspend the connection
				if (!r.isSuspended()){
					r.suspend();
				}
								
				// de-serialize message
				// broadcaster.broadcast(json);
				// out.write(json.getBytes());
				// out.flush();
				
				// FIXME - single Encoder.invoke() !!!
				// FIXME - needs to be pushed to CLI !!! - returns Objects - Encode can encode
			} else if ("services".equals(apiType)){	
				
				if ("/api/services".equals(pathInfo)){
					String services = Encoder.toJson(Runtime.getServices());
					out.write(services.getBytes());
					out.flush();
					// close ?
					return;
					
				} else if  ("/api/services/".equals(pathInfo)){
					Encoder.write(out, Runtime.getServiceNames());
					out.flush();
					return;
				} else if (parts.length == 4 && !"/".equals(trailingCharacter)) {
					// /api/services/{service}
					// which is - give me the {service} state
					
					// FIXME clean up - uniform encoding & errors
					String sname = parts[3];
					ServiceInterface si = Runtime.getService(sname);
					if(si == null){
						throw new IOException(String.format("could not return service", sname));
					}
					
					out.write(Encoder.toJson(si).getBytes());
					out.flush();
					
					return;
				} else if (parts.length == 4 && "/".equals(trailingCharacter)){
					// /api/services/{service}/
					// which is - give me the runtime methods
					// should have fully type parameter descriptions ?
					
					// FIXME clean up - uniform encoding & errors
					String sname = parts[3];
					ServiceInterface si = Runtime.getService(sname);
					if(si == null){
						//FIXME  return error !
						// this is synchronous
						out.write(Encoder.toJson(error("could not return service", sname)).getBytes());
						out.flush();
						return;
					}
				
					out.write(Encoder.toJson(si.getDeclaredMethodNames()).getBytes());
					out.flush();
					
					return;
				}
				
				// on to a service instance  runtime vs runtime/
				
				// test if length is at least > 5
				// if not return error + correct format + http reference :)
				
				// FIXME ALL URI DECODING IS THE SAME - SAME AS CLI & SAME AS subscribe !!!
				// service <- gives data state
				// service/ <- gives methods
				
				// get a specific service instance - execute method --with
				// parameters--
				String serviceName = parts[3]; // FIXME how to handle
				String fn = parts[4];
				Object[] typedParameters = null;

				ServiceInterface si = org.myrobotlab.service.Runtime.getService(serviceName);

				// get parms
				if (parts.length > 4) {
					// copy paramater part of rest uri
					String[] stringParams = new String[parts.length - 5];
					for (int i = 0; i < parts.length - 5; ++i) {
						stringParams[i] = parts[i + 5];
					}

					// FIXME FIXME FIXME !!!!
					// this is an input format decision !!! .. it "SHOULD" be
					// determined based on inbound uri format

					typedParameters = TypeConverter.getTypedParamsFromJson(si.getClass(), fn, stringParams);
				}

				// TODO - handle return type -
				// TODO top level is return format /html /text /soap /xml /gson
				// /json /base16 a default could exist - start with SOAP response
				Object ret = si.invoke(fn, typedParameters);
				//return returnObject;
				// encode object
				// return it ..

				Encoder.write(out, ret);
				out.flush();
				
				/*
				String json = Encoder.toJson(ret);
				out.write(json.getBytes());
				out.flush();
				*/
				
				return;
				
			} else {
				throw new IOException("http://host:port/api/{api-type}/...  api-type must be (services | messages)");
			}

			// finding original
			// String uuidOiginal = (String)request.getAttribute(ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID);
			// AtmosphereResource resource = AtmosphereResourceFactory.getDefault().find(uuid);

			// r.getResponse().write().flushBuffer();
			// if ! /api then file/resource system

		} catch (IOException e) {
			// for "clean" API errors
			try {
				//Status status = new Status(e);
				Status status = Status.error(e.getMessage());
				Encoder.write(out, status);
				out.flush();
			} catch(Exception ex){
				error(ex);
			}
		} catch (Exception e){
			// any other error we are 
			// going to dump full stack trace
			try {
				Status status = new Status(e);				
				Encoder.write(out, status);
				out.flush();
			} catch(Exception ex){
				error(ex);
			}
		}
	}
	
	public void extract(){
		try {
			Zip.extractFromFile("./myrobotlab.jar", "root", "resource/WebGUI");
		} catch (IOException e) {
			error(e);
		}
	}
	
	/** - use the service's error() pub sub return
	public void handleError(){
		
	}
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

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			
			//Uri.
			//Uri myUri = Uri.parse("http://stackoverflow.com");

			WebGUI webgui = (WebGUI) Runtime.start("webgui", "WebGUI");
			// webgui.extract();
			
			/*
			Message msg = webgui.createMessage("runtime", "start", new Object[]{"arduino", "Arduino"});
			String json = Encoder.toJson(msg);
			log.info(json);
			// Runtime.start("gui", "GUIService");
			log.info(json);
			*/

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
