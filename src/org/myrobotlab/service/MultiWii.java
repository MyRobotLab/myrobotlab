package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MultiWii extends Service {
	
	transient public Serial serial;
	transient public Serial uart;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MultiWii.class);

	public MultiWii(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			MultiWii template = (MultiWii)Runtime.start("template", "_TemplateService");
			template.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
