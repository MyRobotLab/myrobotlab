package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class TextTransform extends Service implements TextListener, TextPublisher {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TextTransform.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			TextTransform transform = (TextTransform) Runtime.start("transform", "TextTransform");
			transform.test();

			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public TextTransform(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "data", "filter" };
	}

	@Override
	public String getDescription() {
		return "used as a general textual transform - can be pipelined together";
	}

	@Override
	public void onText(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public String publishText(String text) {
		return text;
	}

	@Override
	public Status test() {
		Status status = super.test();

		return status;
	}
}
