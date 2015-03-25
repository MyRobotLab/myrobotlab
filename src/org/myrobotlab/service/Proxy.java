package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MessageListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * simple Java class which allows interaction of classes which can not be
 * instanciated on local platform
 * 
 */
public class Proxy extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Proxy.class.getCanonicalName());

	private String mimicName = null;
	private String mimicType = null;
	// private Service target = null;
	private HashMap<MessageListener, Object> listeners = new HashMap<MessageListener, Object>();

	// TODO - override getName & getType depending on OS/JVM

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		try {

			Proxy template = new Proxy("proxy");
			template.startService();
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Proxy(String n) {
		super(n);
	}

	public void addMessageListener(MessageListener listener) {
		listeners.put(listener, null);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "framework" };
	}

	@Override
	public String getDescription() {
		return "a Proxy service capable of proxying classes which can not or should not be created";
	}

	@Override
	public void in(Message msg) {
		for (MessageListener listener : listeners.keySet()) {
			listener.receive(msg);
		}
	}

	public void removeMessageListener(MessageListener listener) {
		listeners.remove(listener);
	}

	public void setTargetService(String mimicName) {
		// target = s;
		this.mimicName = mimicName;
		// mimicType = s.getClass(). FIXME - no direct getClass calls..
	}

}
