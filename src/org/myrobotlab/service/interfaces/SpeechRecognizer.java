package org.myrobotlab.service.interfaces;

public interface SpeechRecognizer {
	
	public String recognized(String word);
	public void publishRecognized(String recognizedText);
	/**
	 * Event is sent when the listening Service is actually listening. There is
	 * some delay when it initially loads.
	 */
	public void listeningEvent();
	
	public void startListening();
	public void stopListening();
	
	/**
	 * method to suppress recognition listening events This is important when
	 * a Speech Recognizer is listening --> then Speaking, typically you don't want the STT to
	 * listen to its own speech, it causes a feedback loop and with STT not
	 * really very accurate, it leads to weirdness -- additionally it does not
	 * recreate the speech processor - so its not as heavy handed
	 */
	public void pauseListening();

	public void resumeListening();
}
