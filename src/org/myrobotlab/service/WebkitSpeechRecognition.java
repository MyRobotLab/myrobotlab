package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.TextPublisher;

/**
 * 
 * WebkitSpeechRecognition - uses the speech recognition that is built into the chrome web browser
 * this service requires the webgui to be running.
 *
 */
public class WebkitSpeechRecognition extends Service implements SpeechRecognizer, TextPublisher {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WebkitSpeechRecognition(String reservedKey) {
		super(reservedKey);
	}

	@Override
	public String publishText(String text) {
		return text;
	}

	@Override
	public void listeningEvent() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pauseListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public String recognized(String word) {
		return word;
	}

	@Override
	public void resumeListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public void startListening() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stopListening() {
		// TODO Auto-generated method stub
	}

	@Override
	public String[] getCategories() {
		return new String[]{"speech recognition"};
	}

	@Override
	public String getDescription() {
		return "Google Chrome webkit speech";
	}
	
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			
			Runtime.start("webgui", "WebGui");
			Runtime.start("webkitspeechrecognition", "WebkitSpeechRecognition");
			// Runtime.start("myo", "MyoThalmic");
			// remote.connect("tcp://127.0.0.1:6767");

			// Runtime.start("macgui", "GUIService");

			// MyoThalmic myo = (MyoThalmic) Runtime.start("myo", "MyoThalmic");
			// myo.connect();

			// myo.addMyoDataListener(python);

			// Runtime.start("python", "Python");
			// Runtime.start("remote", "RemoteAdapter");
			// Runtime.start("arduino", "Arduino");// Runtime.start("clock01",
			// "Clock"); Runtime.start("clck3", "Clock");
			// Runtime.start("gui", "GUIService");

			// webgui.extract();
			/*
			 * Runtime.start("clck", "Clock"); Runtime.start("clck2", "Clock");
			 * Runtime.start("clck3", "Clock");
			 * 
			 * Runtime.start("clck", "Clock"); Runtime.start("clck2", "Clock");
			 * Runtime.start("clck3", "Clock");
			 */

			/*
			 * Message msg = webgui.createMessage("runtime", "start", new
			 * Object[]{"arduino", "Arduino"}); String json =
			 * Encoder.toJson(msg); log.info(json); // Runtime.start("gui",
			 * "GUIService"); log.info(json);
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}
	}
	

}
