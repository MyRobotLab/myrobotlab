package org.myrobotlab.webgui;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class REST {
	public final static Logger log = LoggerFactory.getLogger(REST.class);

	public static HashSet<String> defaultMethodFilter;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {

			Runtime.createAndStart("servo01", "Servo");
			Runtime.createAndStart("motor01", "Motor");
			REST rest = new REST();
			FileIO.stringToFile("rest.html", rest.getServices());

			Runtime.releaseAll();
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	// TODO - fixme -
	public String getServices() {
		if (defaultMethodFilter == null) {
			defaultMethodFilter = new HashSet<String>();
			defaultMethodFilter.add("main"); // don't want main
		}
		StringBuffer content = new StringBuffer();
		// String restServiceTemplate =
		// FileIO.fileToString("rest.service.template.html");
		String restServiceTemplate = FileIO.resourceToString("rest/rest.service.template.html");

		Iterator<Map.Entry<URI, ServiceEnvironment>> uriIt = Runtime.getServiceEnvironments().entrySet().iterator();
		while (uriIt.hasNext()) {
			Map.Entry<URI, ServiceEnvironment> pairs = uriIt.next();
			Iterator<Map.Entry<String, ServiceInterface>> serviceIt = pairs.getValue().serviceDirectory.entrySet().iterator();
			while (serviceIt.hasNext()) {
				Map.Entry<String, ServiceInterface> servicePair = serviceIt.next();
				String serviceName = servicePair.getKey();

				log.debug(String.format("building method signatures for %s", serviceName));

				ServiceInterface si = servicePair.getValue();
				String serviceType = si.getClass().getSimpleName();
				Method[] methods = si.getClass().getDeclaredMethods();// .getMethods();
																		// FIXME
																		// -
																		// configurable
																		// MORE
																		// methods
																		// return
				TreeMap<String, Method> ms = new TreeMap<String, Method>();

				// building key from method name and ordinal - since the
				// RESTProcessor's can only handle non-dupes of this signature
				for (int i = 0; i < methods.length; ++i) {
					Method m = methods[i];
					String signature = String.format("%s.%d", m.getName(), (m.getParameterTypes() != null) ? m.getParameterTypes().length : 0);
					// disregard anonymous and inner static classes
					if (signature.contains("$") || defaultMethodFilter.contains(signature)) {
						continue;
					}
					log.debug(signature);
					ms.put(signature, m);
				}

				StringBuffer service = new StringBuffer("");
				service.append("<table border=\"1	\">");
				for (Map.Entry<String, Method> me : ms.entrySet()) {
					Method m = me.getValue();
					log.info(m.getName());
					Class<?>[] params = m.getParameterTypes();

					service.append("<tr><td>");
					StringBuffer javadocParams = new StringBuffer();
					javadocParams.append("");
					if (params.length > 0) {
						service.append(String
								.format("<form id=\"%1$s.%2$s\" method=\"GET\" action=\"/services/%1$s/%2$s\" > <a href=\"#\" onClick=\"buildRestURI(document.getElementById('%1$s.%2$s')); return false;\">%2$s</a>",
										serviceName, m.getName()));
						service.append(String.format("<input id=\"p0\" type=\"hidden\" value=\"%s\"/>", serviceName));
						service.append(String.format("<input id=\"p1\" type=\"hidden\" value=\"%s\"/>", m.getName()));
						for (int i = 0; i < params.length; ++i) {
							javadocParams.append(params[i].getCanonicalName());
							if (i != params.length - 1) {
								javadocParams.append(", ");
							}
							service.append(String.format("<input id=\"p%d\" type=\"text\" />", i + 2));
						}

						service.append(String.format("</form>"));

					} else {
						service.append(String.format("<a href=\"/services/%s/%s\">%s</a>", serviceName, m.getName(), m.getName()));
					}

					String javadocURL = String.format("http://rawgit.com/MyRobotLab/myrobotlab/master/javadoc/org/myrobotlab/service/%s.html#%s(%s)", serviceType, m.getName(),
							javadocParams);

					service.append(String
							.format("</td><td><a target=\"_blank\" href=\"%s\"><img src=\"/unknown_grey.png\" width=\"25\" height=\"25\" /></a></td></tr>", javadocURL));
				}
				service.append("</table>");

				String tmp1 = restServiceTemplate.replaceAll("%serviceName%", serviceName);
				String tmp2 = tmp1.replaceAll("%serviceType%", serviceType);
				content.append(tmp2.replaceAll("%methods%", service.toString()));
			}
		}

		// TODO - resource
		// String restTemplate = FileIO.fileToString("rest.template.html");
		String restTemplate = FileIO.resourceToString("rest/rest.template.html");
		String html = restTemplate.replaceAll("%content%", content.toString());
		return html;
	}

}
