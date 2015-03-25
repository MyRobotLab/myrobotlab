package org.myrobotlab.webgui;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.http.Response;
import org.myrobotlab.net.http.ResponseException;
import org.myrobotlab.service.WebGUI;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.slf4j.Logger;

/**
 * WSServer - to be used as a general purpose HTTP server extends
 * WebSocketServer for web socket support clients which do not implement
 * websockets are processed with registered processors HTTP 1.1 support
 * 
 * @author GroG
 * 
 */
public class WSServer extends WebSocketServer {

	public static class WSMsg {
		public WebSocket socket;
		public Message msg;

		public WSMsg(WebSocket conn, Message msg) {
			this.socket = conn;
			this.msg = msg;
		}
	}

	public final static Logger log = LoggerFactory.getLogger(WSServer.class.getCanonicalName());
	private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
	private HashMap<String, HTTPProcessor> processors = new HashMap<String, HTTPProcessor>();

	HTTPProcessor defaultProcessor;
	public static final String HTTP_OK = "200 OK";
	public static final String HTTP_REDIRECT = "301 Moved Permanently";

	// public static final String HTTP_NOT_AUTHORIZED = "401 Not Authorized";
	public static final String HTTP_NOT_AUTHORIZED = "401 Access Denied";
	public static final String HTTP_FORBIDDEN = "403 Forbidden";
	public static final String HTTP_NOTFOUND = "404 Not Found";
	public static final String HTTP_BADREQUEST = "400 Bad Request";
	public static final String HTTP_INTERNALERROR = "500 Internal Server Error";

	public static final String HTTP_NOTIMPLEMENTED = "501 Not Implemented";
	/**
	 * Common mime types for dynamic content
	 */
	public static final String MIME_PLAINTEXT = "text/plain";
	public static final String MIME_HTML = "text/html";

	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

	/**
	 * necessary for certain web displays and gui's when javascript or anglular
	 * needs to send a message directly to a service vs. have the service
	 * subscribe to a publishing point.
	 */
	private boolean allowDirectMessaging = true;
	private Inbox inbox;

	private WebGUI webgui;

	public WSServer(InetSocketAddress address) {
		super(address);
	}

	public WSServer(WebGUI webgui, int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.webgui = webgui;

		// FIXME !! - shorthand - /api/method ...
		// FIXME !! - longhand /api/returnType/paramType/method ....
		// FIXME !!
		// processors.put("/api/soap", new SOAPProcessor());
		// processors.put("/api/html/html/rest", new RESTProcessor());
		// processors.put("/api", new RESTProcessor());
		// default uri map

		// processors.put("/api/soap", new SOAPProcessor()); SOAP is stupid
		// FIXME - refactor with jvm's JAXB default order - NO ANNOTATIONS !!! -
		// no header? (that'd be good)

		processors.put("/services", new RESTProcessor());
		defaultProcessor = new ResourceProcessor(webgui);
		processors.put("/resource", defaultProcessor);// FIXME < wrong should be
														// root
		this.inbox = webgui.getInbox();
	}

	public boolean allowDirectMessaging(boolean b) {
		allowDirectMessaging = b;
		return b;
	}

	public void allowREST(Boolean b) {
		if (b) {
			processors.put("/services", new RESTProcessor());
		} else {
			if (processors.containsKey("/services")) {
				processors.remove("/services");
			}
		}
	}

	/**
	 * Decodes the sent headers and loads the data into Key/value pairs
	 */
	public void decodeHeader(String in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers) throws ResponseException {

		if (in == null) {
			log.error("decode header in is null");
			return;
		}
		int pos0 = in.indexOf("\r");

		if (pos0 == -1) {
			log.error(String.format("bad header %s no \\r", in));
			return;
		}

		String inLine = in.substring(0, pos0);

		StringTokenizer st = new StringTokenizer(inLine);
		if (!st.hasMoreTokens()) {
			throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
		}

		pre.put("method", st.nextToken());

		if (!st.hasMoreTokens()) {
			throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
		}

		String uri = st.nextToken();

		// Decode parameters from the URI
		int qmi = uri.indexOf('?');
		if (qmi >= 0) {
			decodeParms(uri.substring(qmi + 1), parms);
			uri = decodePercent(uri.substring(0, qmi));
		} else {
			// uri = decodePercent(uri);
		}

		// If there's another token, it's protocol version,
		// followed by HTTP headers. Ignore version but parse headers.
		// NOTE: this now forces header names lowercase since they are
		// case insensitive and vary by client.
		++pos0;
		if (st.hasMoreTokens()) {

			String str = in.substring(pos0 + 1);

			StringTokenizer st2 = new StringTokenizer(str, "\r\n");

			while (st2.hasMoreElements()) {
				String line = (String) st2.nextElement();
				int p = line.indexOf(':');
				if (p >= 0)
					headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
			}

		}

		pre.put("uri", uri);

	}

	/**
	 * Decodes parameters in percent-encoded URI-format ( e.g.
	 * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
	 * NOTE: this doesn't support multiple identical keys due to the simplicity
	 * of Map.
	 */
	private void decodeParms(String parms, Map<String, String> p) {
		if (parms == null) {
			p.put(QUERY_STRING_PARAMETER, "");
			return;
		}

		p.put(QUERY_STRING_PARAMETER, parms);
		StringTokenizer st = new StringTokenizer(parms, "&");
		while (st.hasMoreTokens()) {
			String e = st.nextToken();
			int sep = e.indexOf('=');
			if (sep >= 0) {
				p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
			} else {
				p.put(decodePercent(e).trim(), "");
			}
		}
	}

	/**
	 * Decode percent encoded <code>String</code> values.
	 * 
	 * @param str
	 *            the percent encoded <code>String</code>
	 * @return expanded form of the input, for example "foo%20bar" becomes
	 *         "foo bar"
	 */
	protected String decodePercent(String str) {
		String decoded = null;
		try {
			decoded = URLDecoder.decode(str, "UTF8");
		} catch (UnsupportedEncodingException ignored) {
		}
		return decoded;
	}

	/**
	 * Find byte index separating header from body. It must be the last byte of
	 * the first two sequential new lines.
	 */
	private int findHeaderEnd(final byte[] buf, int rlen) {
		int splitbyte = 0;
		while (splitbyte + 3 < rlen) {
			if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
				return splitbyte + 4;
			}
			splitbyte++;
		}
		return 0;
	}

	// ///////////////////// FROM NANOHTTPD ///////////////////////

	/**
	 * Find the byte positions where multipart boundaries start.
	 */
	private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
		int matchcount = 0;
		int matchbyte = -1;
		List<Integer> matchbytes = new ArrayList<Integer>();
		for (int i = 0; i < b.limit(); i++) {
			if (b.get(i) == boundary[matchcount]) {
				if (matchcount == 0)
					matchbyte = i;
				matchcount++;
				if (matchcount == boundary.length) {
					matchbytes.add(matchbyte);
					matchcount = 0;
					matchbyte = -1;
				}
			} else {
				i -= matchcount;
				matchcount = 0;
				matchbyte = -1;
			}
		}
		int[] ret = new int[matchbytes.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = matchbytes.get(i);
		}
		return ret;
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		String clientkey = String.format("%s:%d", conn.getRemoteSocketAddress().getAddress().getHostAddress(), conn.getRemoteSocketAddress().getPort());
		webgui.clients.remove(clientkey);
		webgui.invoke("publishDisconnect", conn);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		Logging.logError(ex);
		if (conn != null) {
			// some errors like port binding failed may not be assignable to a
			// specific websocket
		}
	}

	// FIXME - return aggregate of conn & MRL Message to publish
	// keep authorization...
	@Override
	public void onMessage(WebSocket conn, String message) {

		log.info("webgui <---to--- client {}", message);

		Message msg = Encoder.fromJson(message, Message.class);
		msg.sender = conn.getRemoteSocketAddress().getAddress().getHostAddress();

		// FIXME - move to Security service
		if (!webgui.isAuthorized(msg)) {
			webgui.error("unAuthorized message !!! %s.%s from sender %s", msg.name, msg.method, msg.sender);
			return;
		}

		if (allowDirectMessaging) { // FIXME - this "could" be done at the inbox
									// level ? check to see if you did it there
			inbox.add(msg);
		}

		webgui.invoke("publishWSMsg", new WSMsg(conn, msg));

	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		String clientkey = String.format("%s:%d", conn.getRemoteSocketAddress().getAddress().getHostAddress(), conn.getRemoteSocketAddress().getPort());
		log.info(String.format("onOpen %s", clientkey));
		log.info(String.format("onOpen %s", conn.getLocalSocketAddress().getHostName()));
		log.info(String.format("onOpen %s", conn.getRemoteSocketAddress().getHostName()));
		webgui.clients.put(clientkey, clientkey);
		webgui.invoke("publishConnect", conn);
	}

	@Override
	public void onRawOpen(WebSocket conn, ByteBuffer d) {

		try {
			if (conn == null) {
				log.error("conn is null");
				return;
			}

			log.info(String.format("onRawOpen %s", conn.getRemoteSocketAddress()));

			// this is the whole buffer i think
			String s = new String(d.array());
			// log.info(s);

			String sub = s.substring(0, d.limit());
			log.info(sub);

			int pos0 = sub.indexOf("\r\n\r\n");

			if (pos0 == -1) {
				log.error("invalid http header");
				return;
			}

			String headerStr = sub.substring(0, pos0);
			String postBody = null;

			if (pos0 > 0) {
				postBody = sub.substring(pos0 + 4);
			}

			HashMap<String, String> parms = new HashMap<String, String>();
			HashMap<String, String> headers = new HashMap<String, String>();

			// Decode the header into parms and header java properties
			Map<String, String> pre = new HashMap<String, String>();

			// FIXME - return an object - pre ???
			decodeHeader(headerStr, pre, parms, headers);
			String uri = pre.get("uri");
			String method = pre.get("method");

			// ////////////// webserver //////////////////////////
			log.info(String.format("%s [%s]", method, uri));
			String[] keys = uri.split("/");
			String key = null;
			if (keys.length > 1) {
				key = String.format("/%s", keys[1]);
			}

			// FIXME use different response encoders Encoder.base64 etc
			Response r = null;

			// FIXME - parameter encoding & return encoding needs to be resolved
			// in framework - before processing
			// FIXME hacked up
			// begin new api interface
			if (uri.startsWith("/api/soap")) {
				HTTPProcessor processor = processors.get("/api/soap");
				// String paramEncoding = keys[2];

				r = processor.serve(uri.substring("/api/soap".length()), method, headers, parms, postBody);

			} else if (processors.containsKey(key)) {

				HTTPProcessor processor = processors.get(key);
				log.debug(String.format("uri hook - [%s] invoking %s", key, processor.getClass().getSimpleName()));
				r = processor.serve(uri, method, headers, parms, postBody);

			} else {
				log.info("pre -defaultProcessor");
				r = defaultProcessor.serve(uri, method, headers, parms, postBody);
				log.info("post -defaultProcessor");
			}

			if (r == null) {
				log.info("---------------CLOSE-------------------------");
				return;
			}

			// serializing r
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			r.send(out);
			out.flush();

			byte[] ba = out.toByteArray();
			log.info(String.format("sending %d bytes", ba.length));
			conn.send(ba);
			// conn.close();

		} catch (Exception e) {
			// attempt a 500
			Logging.logError(e);
		} finally {
			conn.close();
		}

	}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	// FIXME - on release all - this throws an exception and doesn't complete -
	// but is it worth
	// the overhead of a try???
	public void sendToAll(String text) {
		Collection<WebSocket> con = connections();
		log.info("webgui ---to---> client ");
		synchronized (con) {
			for (WebSocket c : con) {
				c.send(text);
			}
		}
	}

}
