package org.myrobotlab.service;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import org.aitools.programd.Core;
import org.aitools.programd.util.URLTools;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;

/**
 * Program D service for MyRobotLab
 * Uses AIML to create a ChatBot
 * 
 * To get this working, download ProgramD (to get the AIML and config files)
 * More Info at http://aitools.org/Program_D 
 * 
 * @author kwatters
 *
 */
public class ProgramD extends Service {

	private URL baseURL;
	private String corePropertiesPath;
	private Core core = null;
	
	public ProgramD(String reservedKey) {
		super(reservedKey);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "AIML interpreter based on Program D";
	}

	@Override
	public void stopService() {
		// TODO Auto-generated method stub
		super.stopService();
	}

	@Override
	public void startService() {
		// TODO Auto-generated method stub
		super.startService();

	}

	/**
	 * Load the AIML Brain and config.  This must be called after the service is created.
	 * 
	 * @param programDPath - should be the full path to the ProgramD root
	 * @param corePropertiesPath - should be the full path to the core.xml file for programD
	 */
	public void loadCore(String programDPath, String corePropertiesPath) {
		
		try {
			baseURL = new URL("file:/" + programDPath);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 try {
			core = new Core(baseURL, URLTools.createValidURL(corePropertiesPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param text
	 * @param userId
	 * @param robotName
	 * @return
	 */
	public String getResponse(String text, String userId, String chatbotName) {
		 String res = core.getResponse(text, userId, chatbotName);
		 System.out.println(res);
		 return res;
	}
	
	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		ProgramD alice = (ProgramD) Runtime.createAndStart("alice", "ProgramD");
		String progDPath = "C:/dev/workspace.kmw/ProgramD/";
		String corePropertiesPath = "c:\\dev\\workspace.kmw\\ProgramD\\conf\\core.xml";
		alice.loadCore(progDPath,corePropertiesPath);
		String response = alice.getResponse("Hello.", "1", "SampleBot");
		System.out.println("Alice" + response);
	}

}
