package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * MultiWii - this is a skeleton service intended as a place holder to 
 * support controling the MultiWii
 *
 * MultiWii is a general purpose software to control a multirotor RC model.
 * http://www.multiwii.com/
 */
public class MultiWii extends Service {

	transient public Serial serial;

	transient public Serial uart;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MultiWii.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			MultiWii template = (MultiWii) Runtime.start("template", "_TemplateService");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public MultiWii(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "control" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

}
