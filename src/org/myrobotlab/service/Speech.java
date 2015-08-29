/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.util.ArrayList;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;

public class Speech extends Proxy implements TextListener, SpeechSynthesis {

	private static final long serialVersionUID = 1L;
	
	SpeechSynthesis speechProxy;
	TextListener textProxy;

	public Speech(String n) {
		super(n);
	}

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("speech", "MarySpeech", "default speech");		
		return peers;
	}

	@Override
	public Boolean isSpeaking(Boolean b) {
		return speechProxy.isSpeaking(b);
	}

	@Override
	public ArrayList<String> getVoices() {
		return speechProxy.getVoices();
	}

	@Override
	public boolean setVoice(String voice) {
		return speechProxy.setVoice(voice);
	}

	@Override
	public String saying(String t) {
		return speechProxy.saying(t);
	}

	@Override
	public void setLanguage(String l) {
		speechProxy.setLanguage(l);
	}

	@Override
	public boolean speak(String toSpeak) {
		return speechProxy.speak(toSpeak);
	}

	@Override
	public boolean speakBlocking(String toSpeak) {
		return speechProxy.speakBlocking(toSpeak);
	}

	@Override
	public void setVolume(float volume) {
		speechProxy.setVolume(volume);
	}

	@Override
	public float getVolume() {
		return speechProxy.getVolume();
	}

	@Override
	public void interrupt() {
		speechProxy.interrupt();
	}

	@Override
	public String publishStartSpeaking(String utterance) {
		return speechProxy.publishStartSpeaking(utterance);
	}

	@Override
	public String publishEndSpeaking(String utterance) {
		return speechProxy.publishEndSpeaking(utterance);
	}

	@Override
	public void onText(String text) {
		textProxy.onText(text);
	}

	@Override
	public String getLanguage() {
		return speechProxy.getLanguage();
	}
	
	public void startService(){
		super.startService();
		speechProxy = (SpeechSynthesis) startPeer("speech");
		textProxy = (TextListener) startPeer("speech");
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		
		Speech speech = (Speech)Runtime.start("speech", "Speech");
		speech.speak("Hello World");
		speech.speakBlocking("Hello World");
	}

	@Override
	public String getVoice() {
		return speechProxy.getVoice();
	}
	
}
