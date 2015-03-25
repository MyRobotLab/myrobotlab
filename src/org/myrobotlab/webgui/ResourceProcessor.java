package org.myrobotlab.webgui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.java_websocket.util.Base64;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.NanoHTTPD;
import org.myrobotlab.net.http.Response;
import org.myrobotlab.security.BasicSecurity;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.WebGUI;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.slf4j.Logger;

public class ResourceProcessor implements HTTPProcessor {

	public HashSet<String> scannedDirectories = new HashSet<String>();

	public HashMap<String, String> searchAndReplace = new HashMap<String, String>();

	public final static Logger log = LoggerFactory.getLogger(ResourceProcessor.class);

	private WebGUI webgui;

	public ResourceProcessor(WebGUI webgui) {
		this.webgui = webgui;
		scan();
	}

	public byte[] filter(String filter, Map<String, String> header) {
		String inHost = header.get("host");
		String hostIP;
		int pos0 = inHost.lastIndexOf(":");
		if (pos0 > 0) {
			hostIP = inHost.substring(0, pos0);
		} else {
			hostIP = inHost;
		}
		log.info("preforming the following substitutions for myrobotlab.html");
		// log.info("from client @ {}", socket.getRemoteSocketAddress());
		log.info("<%=getHostAddress%> --> {}", hostIP);
		filter = filter.replace("<%=getHostAddress%>", hostIP);
		log.info("<%=wsPort%> --> {}", webgui.port);
		filter = filter.replace("<%=wsPort%>", webgui.port.toString());
		log.info("<%=runtimeName%> --> {}", Runtime.getInstance().getName());
		filter = filter.replace("<%=runtimeName%>", Runtime.getInstance().getName());
		log.info("<%=webguiName%> --> {}", webgui.getName());
		filter = filter.replace("<%=webguiName%>", webgui.getName());
		log.info("<%=httpPort%> --> {}", webgui.port.toString());
		filter = filter.replace("<%=httpPort%>", webgui.port.toString());
		// filter.replace(, newChar);

		if (webgui.useLocalResources()) {
			filter = filter.replace("<%=mrl.script.location%>", "");
		} else {
			filter = filter.replace("<%=mrl.script.location%>", "http://github.com/MyRobotLab/myrobotlab/tree/master/src/resource/WebGUI/");
		}
		return filter.getBytes();

	}

	// FIXME - deprecate
	@Override
	public HashSet<String> getURIs() {
		// TODO Auto-generated method stub
		return null;
	}

	public void scan() {
		try {
			List<File> files = FindFile.find(webgui.root, null);
			for (int i = 0; i < files.size(); ++i) {
				File file = files.get(i);
				String t = file.getPath().replace('\\', '/');
				String uri = t.substring(webgui.root.length());
				log.info(String.format("overriding with uri [%s]", uri));
				scannedDirectories.add(uri);
			}
		} catch (FileNotFoundException e) {
			log.info("root directory not found - no overrides");
		}
	}

	@Override
	public Response serve(String uri, String method, Map<String, String> header, Map<String, String> parms, String postBody) {
		return serveFile(uri, header, new File(webgui.root), true, scannedDirectories.contains(uri));
	}

	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	// FIXME - normalize further
	public Response serveFile(String uri, Map<String, String> header, File homeDir, boolean allowDirectoryListing, boolean isCustomFile) {

		String username;
		String password;

		//
		if (webgui.requiresSecurity()) {// && !authorized) {
			try {
				if (header.containsKey("authorization")) {
					String up = header.get("authorization");
					int pos = up.indexOf("Basic ");
					if (pos != -1) {
						up = up.substring(pos + 6);
					}
					// FIXME - depends on commons !!!!
					String usernameAndPassword = new String(Base64.decode(up)); // SHWEET
																				// CURRENTLY
																				// USING
																				// WEBSOCKETS
																				// VERSION
																				// !!!
																				// :P
					username = usernameAndPassword.substring(0, usernameAndPassword.lastIndexOf(":"));
					password = usernameAndPassword.substring(usernameAndPassword.lastIndexOf(":") + 1);
					String token = BasicSecurity.authenticate(username, password);

					if (token != null) {
						// authorized = true;
					} else {
						throw new Exception(String.format("no token for user %s", username));
					}
					log.info(usernameAndPassword);
				} else {
					throw new Exception("no authorization in header");
				}

			} catch (Exception e) {
				Logging.logError(e);
				Response r = new Response(e.getMessage());
				r.setStatus(Response.Status.FORBIDDEN);
				r.addHeader("WWW-Authenticate", "Basic realm=\"MyRobotLab\"");
				return r;
			}
		}

		// Remove URL arguments
		uri = uri.trim().replace(File.separatorChar, '/');
		if (uri.indexOf('?') >= 0) {
			uri = uri.substring(0, uri.indexOf('?'));
		}

		// Prohibit getting out of current directory
		if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0) {
			return new Response(Response.Status.FORBIDDEN, WSServer.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
		}

		File f;
		if (!isCustomFile) {
			// ---- begin resource ----------------
			// TODO ? hard coded /resource - are they going to pull up
			// WebGUI.class how useful is that ??
			String resoucePath = String.format("/resource%s", uri);
			InputStream fis = null;
			if (resoucePath.endsWith("/")) {
				// ends with a slash .. might be a directory - try index.html
				String documentIndex = String.format("%sindex.html", resoucePath);
				fis = FileIO.class.getResourceAsStream(documentIndex);
				if (fis == null) { // couldn't find document index try directory
									// listing, if allowed
					fis = FileIO.class.getResourceAsStream(resoucePath);
				} else {
					uri = documentIndex;
				}
			} else {
				// FileIO.getResource(resoucePath); FIXME - got tired of trying
				// to refactor
				// FileIO.getSource();
				fis = FileIO.class.getResourceAsStream(resoucePath);
			}

			if (fis == null) {
				return new Response(Response.Status.NOT_FOUND, WSServer.MIME_PLAINTEXT, "Error 404, file not found.");
			}

			try {
				// Get MIME type from file name extension, if possible
				String mime = null;
				int dot = uri.lastIndexOf('.');
				if (dot >= 0) {
					mime = (String) NanoHTTPD.theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
				}
				if (mime == null) {
					// mime = NanoHTTPD.MIME_DEFAULT_BINARY;
					mime = WSServer.MIME_PLAINTEXT;
				}

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = fis.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				fis.close();
				buffer.flush();

				byte[] content = null;

				// if ("/resource/WebGUI/myrobotlab.html".equals(uri)) {
				if (uri.endsWith(".mrl")) {
					content = filter(new String(buffer.toByteArray()), header);
					mime = NanoHTTPD.MIME_HTML;
				} else {
					content = buffer.toByteArray();
				}

				Response r = new Response(Response.Status.OK, mime, new ByteArrayInputStream(content));

				r.addHeader("Content-length", "" + content.length);
				return r;
			} catch (IOException ioe) {
				return new Response(Response.Status.FORBIDDEN, WSServer.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
			}
			// ---- end resource -------------------
		} else {
			// ---- begin file -----------------------
			f = new File(homeDir, uri);
			if (!f.exists())
				return new Response(Response.Status.NOT_FOUND, WSServer.MIME_PLAINTEXT, "Error 404, file not found.");

			// List the directory, if necessary
			if (f.isDirectory()) {
				// Browsers get confused without '/' after the
				// directory, send a redirect.
				if (!uri.endsWith("/")) {
					uri += "/";
					Response r = new Response(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
					r.addHeader("Location", uri);
					return r;
				}

				// First try index.html and index.htm
				if (new File(f, "index.html").exists())
					f = new File(homeDir, uri + "/index.html");
				else if (new File(f, "index.htm").exists())
					f = new File(homeDir, uri + "/index.htm");

				// No index file, list the directory
				else if (allowDirectoryListing) {
					String[] files = f.list();
					String msg = "<html><body><h1>Directory " + uri + "</h1><br/>";

					if (uri.length() > 1) {
						String u = uri.substring(0, uri.length() - 1);
						int slash = u.lastIndexOf('/');
						if (slash >= 0 && slash < u.length())
							msg += "<b><a href=\"" + uri.substring(0, slash + 1) + "\">..</a></b><br/>";
					}

					for (int i = 0; i < files.length; ++i) {
						File curFile = new File(f, files[i]);
						boolean dir = curFile.isDirectory();
						if (dir) {
							msg += "<b>";
							files[i] += "/";
						}

						msg += "<a href=\"" + NanoHTTPD.encodeUri(uri + files[i]) + "\">" + files[i] + "</a>";

						// Show file size
						if (curFile.isFile()) {
							long len = curFile.length();
							msg += " &nbsp;<font size=2>(";
							if (len < 1024)
								msg += curFile.length() + " bytes";
							else if (len < 1024 * 1024)
								msg += curFile.length() / 1024 + "." + (curFile.length() % 1024 / 10 % 100) + " KB";
							else
								msg += curFile.length() / (1024 * 1024) + "." + curFile.length() % (1024 * 1024) / 10 % 100 + " MB";

							msg += ")</font>";
						}
						msg += "<br/>";
						if (dir)
							msg += "</b>";
					}
					return new Response(Response.Status.OK, NanoHTTPD.MIME_HTML, msg);
				} else {
					return new Response(Response.Status.FORBIDDEN, WSServer.MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
				}
			}

			// ----- end file ------------------------
		}
		// }
		try {
			// Get MIME type from file name extension, if possible
			String mime = null;
			// int dot = f.getCanonicalPath().lastIndexOf('.');
			int dot = uri.lastIndexOf('.');
			if (dot >= 0) {
				mime = (String) NanoHTTPD.theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
			}
			if (mime == null) {
				mime = WSServer.MIME_PLAINTEXT;
			}

			// Support (simple) skipping:
			long startFrom = 0;
			String range = header.get("Range");
			if (range != null) {
				if (range.startsWith("bytes=")) {
					range = range.substring("bytes=".length());
					int minus = range.indexOf('-');
					if (minus > 0)
						range = range.substring(0, minus);
					try {
						startFrom = Long.parseLong(range);
					} catch (NumberFormatException nfe) {
					}
				}
			}

			InputStream fis;
			if (!isCustomFile) {
				fis = FileIO.class.getResourceAsStream(uri);
				if (fis == null) {
					return new Response(Response.Status.NOT_FOUND, WSServer.MIME_PLAINTEXT, "Error 404, file not found.");
				}

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = fis.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				fis.close();
				buffer.flush();

				byte[] content = null;
				// FIXME - this is not normalized - because the code around it
				// is
				// not normalized
				if (uri.endsWith(".mrl")) {
					content = filter(new String(buffer.toByteArray()), header);
					mime = NanoHTTPD.MIME_HTML;
				} else {
					content = buffer.toByteArray();
				}

				Response r = new Response(Response.Status.OK, mime, new ByteArrayInputStream(content));

				r.addHeader("Content-length", "" + content.length);
				return r;
			} else {
				fis = new FileInputStream(f);
				// fis.skip(startFrom); // TODO support skip in the future
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = fis.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				fis.close();
				buffer.flush();

				byte[] content = null;
				// FIXME - this is not normalized - because the code around it
				// is
				// not normalized

				if (uri.endsWith(".mrl")) {
					content = filter(new String(buffer.toByteArray()), header);
					mime = NanoHTTPD.MIME_HTML;
				} else {
					content = buffer.toByteArray();
				}

				Response r = new Response(Response.Status.OK, mime, new ByteArrayInputStream(content));
				r.addHeader("Content-length", "" + content.length);
				// r.addHeader("Content-length", "" + (f.length() - startFrom));
				// r.addHeader("Content-range", "" + startFrom + "-" +
				// (f.length() - 1) + "/" + f.length());
				return r;
			}

		} catch (IOException ioe) {
			return new Response(Response.Status.FORBIDDEN, WSServer.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
		}
	}

}
