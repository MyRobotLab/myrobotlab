package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Keyboard extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Keyboard.class.getCanonicalName());
	
	// FIXME add to Service
	HashMap<String, Command> commands = null;

	public class Command {
		public String name;
		public String method;
		public Object[] params;
		Command(String name, String method, Object[] params)
		{
			this.name = name;
			this.method = method;
			this.params = params;
		}
	}

	public Keyboard(String n) {
		super(n);
	}

	public String keyCommand(String cmd) {
		log.info(cmd);
		if (commands != null && commands.containsKey(cmd))
		{	Command currentCommand = commands.get(cmd);
			send(currentCommand.name, currentCommand.method, currentCommand.params);
		}		
		return cmd;
	}
	
	// TODO - should this be in Service ?????
	public void addCommand(String actionPhrase, String name, String method, Object...params)
	{
		if (commands == null)
		{
			commands = new HashMap<String, Command>();
		}
		commands.put(actionPhrase, new Command(name, method, params));
	}
	
	@Override
	public String getDescription() {
		return "keyboard";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Keyboard keyboard 	= (Keyboard)Runtime.createAndStart("keyboard", "Keyboard");
		Log log	 			= (Log)Runtime.createAndStart("log", "Log");
		Clock clock			= (Clock)Runtime.createAndStart("clock", "Clock");
		GUIService gui 		= (GUIService)Runtime.createAndStart("gui", "GUIService");

		keyboard.addCommand("S", clock.getName(), "startClock");
		
		log.subscribe("keyCommand", keyboard.getName(), "log", String.class, String.class);
		//clock.subscribe("keyCommand", keyboard.getName(), "startClock", String.class, String.class);

	}

}
