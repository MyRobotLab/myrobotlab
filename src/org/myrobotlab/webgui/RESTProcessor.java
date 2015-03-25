package org.myrobotlab.webgui;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
public class RESTProcessor implements HTTPProcessor {

	static public class RESTException extends Exception {
		private static final long serialVersionUID = 1L;

		public RESTException(String format) {
			super(format);
		}
	}

	public final static Logger log = LoggerFactory.getLogger(RESTProcessor.class.getCanonicalName());

	private HashSet<String> uris = new HashSet<String>();

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

	public void addURI(String uri) {
		uris.add(uri);
	}

	@Override
	public HashSet<String> getURIs() {
		return uris;
	}

	@Override
	public Response serve(String uri, String method, Map<String, String> header, Map<String, String> parms, String postBody) {

		String returnFormat = "gson";

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

		if ("/services".equals(uri)) {
			// get runtime list
			log.info("services request");
			REST rest = new REST();
			String services = rest.getServices();

			Response response = new Response(Status.OK, "text/html", services);
			return response;

		} else if (keys.length > 3) {
			// get a specific service instance - execute method --with
			// parameters--
			String serviceName = keys[2];
			String fn = keys[3];
			Object[] typedParameters = null;

			ServiceInterface si = org.myrobotlab.service.Runtime.getService(serviceName);
			if (si == null) {
				log.error(String.format("%s service not found", serviceName));
				Response response = new Response(Status.OK, "text/plain", String.format("%s service not found", serviceName));
				return response;
			}

			// get parms
			if (keys.length > 4) {
				// copy paramater part of rest uri
				String[] stringParams = new String[keys.length - 4];
				for (int i = 0; i < keys.length - 4; ++i) {
					stringParams[i] = keys[i + 4];
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
			String xml = null;

			// Message msg = new Message();
			// FIXME FIXME FIXME - put in a ResponseHanlder object with simple
			// imputs/outputs String InputStream / OutputStream !!!!
			if ("xml".equals(returnFormat)) {

				if (returnObject != null) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					// if (returnObject)
					try {

						ReturnType returnType = new ReturnType();

						// FIXME - handle
						if (returnObject.getClass() == ArrayList.class) {
							returnType.arrayList = (ArrayList<Object>) returnObject;
						} else if (returnObject.getClass() == Array.class) {
							returnType.array = (Object[]) returnObject;
						} else if (returnObject.getClass() == HashMap.class) {
							returnType.map = (HashMap<String, Object>) returnObject;
						} else {
							returnType.returnObject = returnObject;
						}

						// serializer.write(returnType, out);
						// FIXME !! USE JAXB - it comes with the JVM - or better
						// yet - don't use XML

					} catch (Exception e) {
						// TODO Auto-generated catch block SOAP FAULT ??
						e.printStackTrace();
					}
					xml = String.format("<%sResponse>%s</%sResponse>", fn, new String(out.toByteArray()), fn);
				} else {
					xml = String.format("<%sResponse />", fn);
				}

				Response response = new Response(Status.OK, "text/xml", xml);
				return response;

			} else if ("gson".equals(returnFormat)) {
				ByteArrayOutputStream out = null;
				// FIXME - a "response" of some sort is important - even if the
				// returned object is null
				// the signal which you get from a event can be significant -
				// the quetion arrises
				// gson is to encode the returned data - but if there is no data
				// - it still
				// should return ... "blank" ?

				String encodedResponse = "null"; // Hah .. dorky yet
													// appropriate?

				try {
					if (returnObject != null) {

						/*------------- hacked out
						Gson gson = new Gson(); // FIXME - threadsafe? singeton?
						out = new ByteArrayOutputStream();
						JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
						writer.setIndent("  ");

						log.info("gson - 3");
						writer.beginArray();
						log.info("gson - 4");
						gson.toJson(returnObject, returnObject.getClass(), writer);
						// for (Message message : messages) {
						// gson.toJson(message, Message.class, writer);
						// }
						writer.endArray();
						writer.close();

						log.info("gson - 4");
						encodedResponse = new String(out.toByteArray());
						---------------hacked out ---- */
						log.info(String.format("gson encoding [%s]", returnObject.getClass().getSimpleName()));
						encodedResponse = Encoder.toJson(returnObject);
						log.info("done");

					}
				} catch (Exception e) {
					Logging.logError(e);
				}

				Response response = new Response(Status.OK, "application/json", encodedResponse);
				return response;
			}

			// handle response depending on type
			// TODO - make structured !!!
			// Right now just return string object
			// Response response = new Response("200 OK", "text/xml",
			// (returnObject == null)?String.format("<%sResponse></%sResponse>",
			// method, method):returnObject.toString());
			Response response = new Response(Status.OK, "text/xml", (returnObject == null) ? String.format("<%sResponse></%sResponse>", method, method) : xml);
			return response;
		}
		return null;
	}

}
