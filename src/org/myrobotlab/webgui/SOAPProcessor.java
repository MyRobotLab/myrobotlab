package org.myrobotlab.webgui;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.TypeConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.http.Response;
import org.myrobotlab.net.http.Response.Status;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

// FIXME - normalize - make only ResourceProcessor (its twin) - move all this to Encoder !!!
public class SOAPProcessor implements HTTPProcessor {

	/**
	 * a CodeBlock is an execution unit ready to be invoked with converted
	 * parameters and data
	 * 
	 */
	static public class CodeBlock {
		public Method method;
		public Object[] params;

		CodeBlock(Method method, Object[] params) {
			this.method = method;
			this.params = params;
		}
	}

	static public class RESTException extends Exception {
		private static final long serialVersionUID = 1L;

		public RESTException(String format) {
			super(format);
		}
	}

	public final static Logger log = LoggerFactory.getLogger(SOAPProcessor.class.getCanonicalName());

	MessageFactory factory = null;

	private HashSet<String> uris = new HashSet<String>();

	static private String templateResponse = null;

	/**
	 * Decodes the percent encoding scheme. <br/>
	 * For example: "an+example%20string" -> "an example string"
	 */
	private static String decodePercent(String str, boolean decodeForwardSlash) {

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
			case '+':
				sb.append(' ');
				break;
			case '%':
				if ("2F".equalsIgnoreCase(str.substring(i + 1, i + 3)) && !decodeForwardSlash) {
					log.info("found encoded / - leaving");
					sb.append("%2F");
				} else {
					sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
				}
				i += 2;
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return new String(sb.toString().getBytes());

	}

	// TODO - this is a specific xml decode - e.g. Jaxb versus simplexml
	static public CodeBlock getCodeBlockFromXML(String serviceType, String methodName, ArrayList<SOAPBodyElement> parms) {
		ArrayList<Method> candidates = Encoder.getMethodCandidates(serviceType, methodName, parms.size());
		Object[] params = new Object[parms.size()];
		for (int i = 0; i < candidates.size(); ++i) {
			Method m = candidates.get(i);
			Class<?>[] mParms = m.getParameterTypes();
			boolean converted = false;
			// parameter converter
			try {
				for (int j = 0; j < parms.size(); ++j) {
					SOAPBodyElement parm = parms.get(j);
					String value = parm.getValue();
					// if the parm has a value it is a simple type
					// cuz that's the way my xml rolls ;)
					if (value != null) {
						// instead of reflectively invoking a converter
						// i chose manual construction - for possible
						// performance gain
						Class<?> c = mParms[j];
						if (c == Integer.class || c == int.class) {
							params[j] = Integer.parseInt(value);
						} else if (c == String.class) {
							params[j] = value;
						} else if (c == Float.class || c == float.class) {
							params[j] = Float.parseFloat(value);
						} else if (c == Boolean.class || c == boolean.class) {
							params[j] = Boolean.parseBoolean(value);
						} else if (c == Byte.class || c == byte.class) {
							params[j] = Byte.parseByte(value);
						} else if (c == Double.class || c == double.class) {
							params[j] = Double.parseDouble(value);
						} else if (c == Long.class || c == long.class) {
							params[j] = Long.parseLong(value);
						} else if (c == Short.class || c == short.class) {
							params[j] = Short.parseShort(value);
						} else if (c == Character.class || c == char.class) {
							// FIXME - if value.length > 1 - ERROR !! ABORT
							if (value.length() > 1) {
								throw new Exception(String.format("conversion to char - incorrect size of string %s", value));
							}
							params[j] = value.charAt(0);
						}
					}
				} // for each parameter
				converted = true;
			} catch (Exception e) {
				Logging.logError(e);
				converted = false;
			}

			if (converted) {
				return new CodeBlock(m, params);
			}

		}

		log.error(String.format("could not make CodeBlock for %s.%s.%d", serviceType, methodName, parms.size()));
		return null;
	}

	// TODO - encode
	// FIXME - needs to be in .net or .framework
	public static Object invoke(String uri) throws RESTException {
		// String returnFormat = "gson";

		// TODO top level is return format /html /text /soap /xml /gson /json a
		// default could exist - start with SOAP response
		// default is not specified but adds {/rest/xml} /services ...
		// TODO - custom display /displays
		// TODO - structured rest fault responses

		String[] keys = uri.split("/");

		// decode everything
		for (int i = 0; i < keys.length; ++i) {
			keys[i] = decodePercent(keys[i], true);
		}

		// FIXME -
		// /api/returnEncodingType/parameterEncoding/service/method/param0/param1....

		if ("/services".equals(uri)) {
			// get runtime list
			log.info("services request");
			REST rest = new REST();
			String services = rest.getServices();

			Response response = new Response(Status.OK, "text/html", services);
			return response;

		} else if (keys.length > 2) { // FIXME 3-1 ??? how to answer
			// get a specific service instance - execute method --with
			// parameters--
			String serviceName = keys[1]; // FIXME how to handle
			String fn = keys[2];
			Object[] typedParameters = null;

			ServiceInterface si = org.myrobotlab.service.Runtime.getService(serviceName);

			// get parms
			if (keys.length > 2) {
				// copy paramater part of rest uri
				String[] stringParams = new String[keys.length - 3];
				for (int i = 0; i < keys.length - 3; ++i) {
					stringParams[i] = keys[i + 3];
				}

				// FIXME FIXME FIXME !!!!
				// this is an input format decision !!! .. it "SHOULD" be
				// determined based on inbound uri format
				typedParameters = TypeConverter.getTypedParamsFromJson(si.getClass(), fn, stringParams);
			}

			// TODO - handle return type -
			// TODO top level is return format /html /text /soap /xml /gson
			// /json /base16 a default could exist - start with SOAP response
			Object returnObject = si.invoke(fn, typedParameters);
			return returnObject;
		} else {
			throw new RESTException(String.format("invalid uri %s", uri));
		}
	}

	public SOAPProcessor() {
		templateResponse = FileIO.resourceToString("soap/response.xml");
	}

	public void addURI(String uri) {
		uris.add(uri);
	}

	@Override
	public HashSet<String> getURIs() {
		return uris;
	}

	// FIXME - can't throw out - kills thread...
	@Override
	public Response serve(String uri, String method, Map<String, String> header, Map<String, String> parms, String postBody) {
		try {

			String returnFormat = "xml";

			// TODO top level is return format /html /text /soap /xml /gson
			// /json a
			// default could exist - start with SOAP response
			// default is not specified but adds {/rest/xml} /services ...
			// TODO - custom display /displays
			// TODO - structured rest fault responses

			String[] keys = uri.split("/");

			// decode everything
			for (int i = 0; i < keys.length; ++i) {
				keys[i] = decodePercent(keys[i], true);
			}

			/*
			 * 
			 * String soapAction = null; String[] sHeader =
			 * headers.getHeader("SOAPAction");
			 */
			if (factory == null) {
				factory = MessageFactory.newInstance();
			}

			SOAPMessage msg = factory.createMessage(null, new ByteArrayInputStream(postBody.getBytes()));

			SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
			SOAPBody body = env.getBody();

			ArrayList<SOAPBodyElement> params = new ArrayList<SOAPBodyElement>();

			Iterator<Object> itr = body.getChildElements();
			String fn = null;
			while (itr.hasNext()) {
				Object m1 = itr.next();
				if (!(m1 instanceof SOAPBodyElement)) {
					continue;
				}
				SOAPBodyElement methodBody = (SOAPBodyElement) m1;
				fn = methodBody.getLocalName();
				log.info(String.format("found method %s", fn));

				Iterator<Object> mitr = methodBody.getChildElements();
				while (mitr.hasNext()) {
					Object p1 = mitr.next();
					if (!(p1 instanceof SOAPBodyElement)) {
						continue;
					}
					SOAPBodyElement paramBody = (SOAPBodyElement) p1;
					String paramName = paramBody.getLocalName();
					String paramValue = paramBody.getValue();
					log.info(String.format("found param %s=%s ", paramName, paramValue));
					params.add(paramBody);

					// FIXME - check for child elements - if no - then it can be
					// converted to a simple data type
				}
			}

			String serviceName = keys[1];
			ServiceInterface si = org.myrobotlab.service.Runtime.getService(serviceName);
			if (si == null) {
				log.error(String.format("%s service not found", serviceName));
				Response response = new Response(Status.OK, "text/plain", String.format("%s service not found", serviceName));
				return response;
			}

			Object responseObject = null;
			CodeBlock cb = getCodeBlockFromXML(si.getClass().getCanonicalName(), fn, params);
			if (cb != null) {
				// FIXME FIXME FIXME FIXME FIXME FIXME !!!!
				// optimize by an overloaded invoke which accepts the
				// Method I currently have in CodeBlock - which was pulled up by
				// a HashMap and
				// not reflected !!!!
				// responseObject = si.invoke(cb.method.getName(), cb.params);
				responseObject = si.invoke(fn, cb.params);
			}

			String xml = null;

			if ("xml".equals(returnFormat)) {

				/*
				 * if (returnObject != null) { ByteArrayOutputStream out = new
				 * ByteArrayOutputStream(); // if (returnObject) try {
				 * 
				 * ReturnType returnType = new ReturnType();
				 * 
				 * // FIXME - handle if (returnObject.getClass() ==
				 * ArrayList.class) { returnType.arrayList = (ArrayList<Object>)
				 * returnObject; } else if (returnObject.getClass() ==
				 * Array.class) { returnType.array = (Object[]) returnObject; }
				 * else if (returnObject.getClass() == HashMap.class) {
				 * returnType.map = (HashMap<String, Object>) returnObject; }
				 * else { returnType.returnObject = returnObject; }
				 * 
				 * serializer.write(returnType, out);
				 * 
				 * } catch (Exception e) { // TODO Auto-generated catch block
				 * SOAP FAULT ?? e.printStackTrace(); } xml =
				 * String.format("<%sResponse>%s</%sResponse>", fn, new
				 * String(out.toByteArray()), fn); } else { xml =
				 * String.format("<%sResponse />", fn); }
				 */
				xml = templateResponse.replaceAll("%methodName%", fn);
				xml = xml.replaceAll("%responseObject%", (responseObject == null) ? "" : responseObject.toString());
				Response response = new Response(Status.OK, "text/xml", xml);
				return response;

			}

			// handle response depending on type
			// TODO - make structured !!!
			// Right now just return string object
			// Response response = new Response("200 OK", "text/xml",
			// (returnObject ==
			// null)?String.format("<%sResponse></%sResponse>",
			// method, method):returnObject.toString());
			Response response = new Response(Status.OK, "text/xml", (responseObject == null) ? String.format("<%sResponse></%sResponse>", method, method) : xml);
			return response;

		} catch (Exception e) {
			Logging.logError(e);
		}
		return null;
	}

}
