package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/*
 import java.io.File;
 import java.io.IOException;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.security.acl.Acl;
 import java.util.ArrayList;
 import java.util.List;

 import javax.print.attribute.standard.Media;
 import com.google.api.client.http.FileContent;
 */

// https://developers.google.com/+/domains/quickstart/java
public class GoogleAPI extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(GoogleAPI.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		try {
			GoogleAPI template = new GoogleAPI("template");
			template.startService();

			Runtime.createAndStart("gui", "GUIService");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public GoogleAPI(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "cloud" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

}
