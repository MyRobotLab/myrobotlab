package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class _TemplateService extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			_TemplateService template = (_TemplateService) Runtime.start("template", "_TemplateService");
			template.test();

			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public _TemplateService(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
}
