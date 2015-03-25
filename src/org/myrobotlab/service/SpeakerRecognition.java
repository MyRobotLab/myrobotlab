package org.myrobotlab.service;

import java.io.File;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.bitsinharmony.recognito.MatchResult;
import com.bitsinharmony.recognito.Recognito;
import com.bitsinharmony.recognito.VoicePrint;

/**
 * 
 * @author GroG Service Wrapper for - https://github.com/amaurycrickx/recognito
 * 
 */
public class SpeakerRecognition extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(SpeakerRecognition.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			SpeakerRecognition template = new SpeakerRecognition("template");
			template.startService();
			template.test();

			Runtime.createAndStart("gui", "GUIService");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public SpeakerRecognition(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "speech recognition", "control" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public Status test() {

		Status status = super.test();

		try {

			// Create a new Recognito instance defining the audio sample rate to
			// be
			// used
			// Recognito<String> recognito = new Recognito<>(16000.0f);
			Recognito<String> recognito = new Recognito<String>(16000.0f);

			VoicePrint print = recognito.createVoicePrint("me4", new File("me4.wav"));
			recognito.createVoicePrint("me3", new File("me3.wav"));
			recognito.createVoicePrint("me2", new File("me2.wav"));

			// handle persistence the way you want, e.g.:
			// myUser.setVocalPrint(print);
			// userDao.saveOrUpdate(myUser);

			// Now check if the King is back
			List<MatchResult<String>> matches = recognito.identify(new File("me1.wav"));

			log.info(String.format("match count %d", matches.size()));
			for (int i = 0; i < matches.size(); ++i) {
				MatchResult<String> match = matches.get(i);
				System.out.println(String.format("match with %s %d percent positive", match.getKey(), match.getLikelihoodRatio()));
			}

			/*
			 * if (match.getKey().equals("me3")) {
			 * System.out.println("me is back !!! " + match.getLikelihoodRatio()
			 * + "% positive about it..."); }
			 */
		} catch (Exception e) {
			status.addError(e);
		}
		return status;
	}

}
