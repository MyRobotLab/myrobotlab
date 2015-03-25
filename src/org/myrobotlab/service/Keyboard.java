package org.myrobotlab.service;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class Keyboard extends Service {

	public class Command {
		public String name;
		public String method;
		public Object[] params;

		Command(String name, String method, Object[] params) {
			this.name = name;
			this.method = method;
			this.params = params;
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Keyboard.class);
	// TODO - needs capability to re-map keys
	// FIXME add to Service
	HashMap<String, Command> commands = null;
	HashMap<String, String> remap = new HashMap<String, String>();
	transient BlockingQueue<String> blockingData = new LinkedBlockingQueue<String>();

	boolean isBlocking = false;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Keyboard keyboard = (Keyboard) Runtime.start("keyboard", "Keyboard");
		keyboard.test();

	}

	public Keyboard(String n) {
		super(n);
	}

	// TODO - should this be in Service ?????
	public void addCommand(String actionPhrase, String name, String method, Object... params) {
		if (commands == null) {
			commands = new HashMap<String, Command>();
		}
		commands.put(actionPhrase, new Command(name, method, params));
	}

	/**
	 * this method is what other services would use to subscribe to keyboard
	 * events
	 * 
	 * @param service
	 */
	public void addKeyListener(Service service) {
		addListener("publishKey", service.getName(), "onKey", String.class);
	}

	public void addKeyListener(String serviceName) {
		ServiceInterface s = Runtime.getService(serviceName);
		addKeyListener((Service) s);
	}

	public void clearMappings() {
		remap.clear();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "control" };
	}

	@Override
	public String getDescription() {
		return "keyboard";
	}

	/**
	 * This method will be called by graphic components from here it will invoke
	 * the MRL pub/sub publishKey from which other services may listen for key
	 * events
	 * 
	 * @param key
	 * @return
	 */
	public String keyCommand(String key) {
		log.info(key);
		if (commands != null && commands.containsKey(key)) {
			Command currentCommand = commands.get(key);
			send(currentCommand.name, currentCommand.method, currentCommand.params);
		}
		if (isBlocking) {
			blockingData.add(key);
		}
		if (remap.containsKey(key)) {
			invoke("publishKey", remap.get(key));
		} else {
			invoke("publishKey", key);
		}
		return key;
	}

	/**
	 * a onKey event handler for testing purposes only
	 * 
	 * @param key
	 * @return
	 */
	public String onKey(String key) {
		log.info(String.format("onKey [%s]", key));
		return key;
	}

	/**
	 * internal publishing point - private ?
	 * 
	 * @param key
	 */
	public String publishKey(String key) {
		return key;
	}

	public String readKey() {
		try {
			isBlocking = true;
			String ret = blockingData.take();
			isBlocking = false;
			return ret;
		} catch (Exception e) {
			Logging.logError(e);
		}
		isBlocking = false;
		return null;
	}

	public void reMap(String from, String to) {
		remap.put(from, to);
	}

	@Override
	public Status test() {
		Status status = super.test();
		Keyboard keyboard = (Keyboard) Runtime.start(getName(), "Keyboard");
		// TODO simulate keypress ?
		Runtime.start("gui", "GUIService");
		keyboard.addKeyListener(keyboard);

		return status;
	}

}
