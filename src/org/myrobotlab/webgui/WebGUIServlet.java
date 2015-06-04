package org.myrobotlab.webgui;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.myrobotlab.codec.Codec;
import org.myrobotlab.codec.CodecFactory;
import org.myrobotlab.codec.MethodCache;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.WebGUI3;
import org.myrobotlab.service.WebGUI3.Error;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebGUIServlet extends HttpServlet {
	
	// - AMAZING - -
	// http://eclipse.org/jetty/documentation/current/embedded-examples.html#embedded-minimal-servlet
	// FIXME - api default is protobuf - need json/xml (jettison & jaxb)
	// encoding on the uri

	// FIXME - common APP_EVENT_LOG !!!!

	// FIXME - REFLECT ! - then some default for hitting table directly -
	// Hbase.getRecords(table, startKey, endKey)
	// FIXME - subscribe ? - do we need it ?

	// FIXME - HBase connectivity
	// FIXME - FOTAServlet
	
	// FIXME - System (self - health - dashboard - memory - threads - cpu - HBase connectivity - other stats)
	// FIXME - Stats for requests - URI count ?
	// FIXME - get version working 
	
	private static final long serialVersionUID = 1L;

	static Logger log = LoggerFactory.getLogger(WebGUI3.class);
	
	static boolean initialized = false;
	static HashSet<String> filter = new HashSet<String>();

	public static final String ENTITY_PACKAGE = "com.daimler.hbase.entity";

	// public static final String ERROR_IO = "IOERROR"

	// TODO - future make dynamic depending on request headers or other
	// parameters
	Codec codec = CodecFactory.getCodec("json");
	InputStream in;
	OutputStream out;

	// Unfortunately Java can not reflect the classes within a package
	// so this silly list needs to be maintained
	// FIXME - cleanest way is to iterate through the jar's class files :P
	// consider this a documentation of published accessor objects
	final static public String[] OBJECTS = new String[] { "SystemInfo","Table" };
	

protected void doGet(HttpServletRequest request, HttpServletResponse response) {
	process(request, response);
}

public synchronized void init() {
	//initialized = true;
	if (!initialized) {
		// initialize logging
		// will be deprecated in hbase .98

		filter.add("main");

		initialized = true;
	}
}

protected void doPost(HttpServletRequest request, HttpServletResponse response) {
	// FIXME - decode POST - Body !!!
	// CONTRACT - must be an array of encoded types !!!
	process(request, response);
}

public static class MethodInfo {
	public String name;
	public String[] parameterTypes;
	public String returnType;
}

public Map<String, String> getHeadersInfo(HttpServletRequest request) {

	Map<String, String> map = new HashMap<String, String>();

	Enumeration headerNames = request.getHeaderNames();
	while (headerNames.hasMoreElements()) {
		String key = (String) headerNames.nextElement();
		String value = request.getHeader(key);
		map.put(key.toLowerCase(), value);
	}

	return map;
}

// FIXME - use a Message wrapper ? - would require "double encoding" of
// Object[] data ??? - Messages typically are not needed in Synchronous
// communication
// but to have a facility which would wrap would probably be a good idea..
// Messages probably need an encoding field & return address ;) - encoding
// with type mapping
//
// FIXME - precedence URI /{Object}/{method}/[{param0}/{param1}/...]
// if POSTED with Body = [

// FIXME - MethodCache - requires a method signature key - the name is the
// most significant key part
// next would be ordinal (Stopping at ordinal) - method ordinal of same
// count and different types cannot be used
// the third would be parameter "type name" or actual type - actual type
// cannot be used when parameters are encoded

protected void process(HttpServletRequest request, HttpServletResponse response) {
	try {

		in = request.getInputStream();
		out = response.getOutputStream();

		Map<String, String> headers = getHeadersInfo(request);

		if (headers.containsKey("content-type")) {
			log.info(String.format(String.format("in encoding : content-type %s", headers.get("content-type"))));
		}
		if (headers.containsKey("accept")) {
			log.info(String.format(String.format("out encoding : accept %s", headers.get("accept"))));
		}

		// FIXME - allow different encodings
		response.addHeader("Content-Type", codec.getMimeType());
		
		// FIXME reconstruct REST request & log
		String pathInfo = request.getPathInfo();
		String[] parts = null;

		// literal "/api" is the resource key in Jetty

		if (pathInfo != null) {
			parts = pathInfo.split("/");
		}

		if (parts == null || parts.length < 2) {
			handleError(
					"API",
					String.format("http(s)://{host}:{port}/api/{Object}/{Method} where {Object}=%s - to query Object methods use http(s)://{host}:{port}/api/{Object}",
							Arrays.toString(OBJECTS)));
			return;
		}

		

		// inspect
		/*
		if (parts.length > 1) {
			objectName = parts[1];
			clazzName = String.format("%s.%s", ENTITY_PACKAGE, parts[1]);

			clazz = Class.forName(clazzName);
		}
		*/

		ArrayList<MethodInfo> info = null;
		if (parts.length == 2) {
			ServiceEnvironment si = Runtime.getLocalServices();
			
			/* TODO - relfect with javdoc info
			log.info("inspecting");
			
			Method[] methods = clazz.getDeclaredMethods();
			info = new ArrayList<MethodInfo>();
			for (Method method : methods) {
				if (!filter.contains(method.getName())) {
					MethodInfo m = new MethodInfo();
					m.name = method.getName();
					Class<?>[] types = method.getParameterTypes();
					m.parameterTypes = new String[types.length];
					for (int i = 0; i < types.length; ++i) {
						m.parameterTypes[i] = types[i].getSimpleName();
					}
					m.returnType = method.getReturnType().getSimpleName(); // NULL
																			// ?
					info.add(m);
				}
			}
			*/
			
			
			codec.encode(out, si);
			return;
		} else if (parts.length == 3) {
			ServiceInterface si = Runtime.getService(parts[2]);
			Method[] methods = si.getDeclaredMethods();
			codec.encode(out, methods);
			return;
		}/* else if (parts.length > 3) {
			String serviceName = parts[2];
			String method = parts[3];
			
			ServiceInterface si = Runtime.getService(serviceName);
			
			// decode parameters 
			String[] params = new String[parts.length - 4]; // <- this i "wrong" - a big assumption that they are "Strings"
			Object[] 
			for (int i = 0; i < params.length; ++i){
				
			}
			
		}*/
		
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
				handleError("BadInput", String.format("client said it would send %d bytes but only %d were read", cl, bytesRead));
				return;
			}
		}
		
		// FIXME - sloppy to convert to String here - should be done in the Encoder (if that happens)
		String b = null;
		if (body != null){
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
				
		// FETCH AND MERGE METHOD - we have ordinal count now - but NOT the decoded
		// parameters
		// NOW HAVE ORDINAL - fetch the method with its types
		paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), methodName, encodedArray.length);
		// WE NOW HAVE ORDINAL AND TYPES
		params = new Object[encodedArray.length];

		// DECODE AND FILL THE PARAMS
		for (int i = 0; i < params.length; ++i) {
			
			params[i] = codec.decode(encodedArray[i], paramTypes[i]);
		}

		Method method = clazz.getMethod(methodName, paramTypes); // getDeclaredMethod
																	// zod
																	// !!!

		// NOTE --------------
		// strategy of find correct method with correct parameter types
		// "name" is the strongest binder - but without a method cache we
		// are condemned to scan through all methods
		// also without a method cache - we have to figure out if the
		// signature would fit with instanceof for each object
		// and "boxed" types as well

		// best to fail - then attempt to resolve through scanning through
		// methods and trying types - then cache the result

		Object retobj = method.invoke(si, params);
		codec.encode(out, retobj);
		
		MethodCache.cache(clazz, method);
		
		// FIXME - there is no content mime-type being set !!! this would depend on codec being used
		// FIXME - currently a keyword - "json" internally defines the codec - getMimeType !!

	} catch (Exception e) {
		handleError(e);
	}

}

public void handleError(Throwable e) {
	handleError(e.getMessage(), Logging.logError(e));
}

public void handleError(String msg) {
	handleError("unknown", msg);
}

// FIXME - APP_EVENT_LOG for normalizing (if available)
public void handleError(String key, String msg) {
	try {
		log.error(msg);
		Error error = new Error(key, msg);
		codec.encode(out, error);
	} catch (Exception e) {
		Logging.logError(e);
	}
}

// route based on request
public class Request {

	// FIXME - correct mime types

	// general case
	// application/octet-stream <<- best

	// application/protobuf
	// application/x-protobuf
	// application/x-google-protobuf
	// application/json

	String defaultInEncoding = "json";
	String defaultOutEncoding = "json";

	public Request(HttpServletRequest request) {
		// FIXME - get content header request info

		// FIXME - determine /api default & parsed values
	}

	public String getInEncoding() {
		return defaultInEncoding;
	}

	public String getOutEncoding() {
		return defaultOutEncoding;
	}

}

/*
 * Serializing using ProtoBuff example PROTOBUFF Person john =
 * Person.newBuilder().setId(1234).setName("John Doe"
 * ).setEmail("jdoe@example.com") .addPhone(Person.PhoneNumber.newBuilder
 * ().setNumber("555-4321").setType(Person.PhoneType.HOME)).build();
 * OutputStream out = response.getOutputStream();
 * out.write(john.toByteArray()); out.flush();
 */
// response.getWriter().write("<html><body>GET response</body></html>");

}
