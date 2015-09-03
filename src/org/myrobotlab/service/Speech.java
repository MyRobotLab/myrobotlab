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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.digest.DigestUtils;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;

import com.sun.tools.javac.util.List;

public class Speech extends Proxy implements TextListener, SpeechSynthesis {

	private static final long serialVersionUID = 1L;

	SpeechSynthesis proxy;

	public Speech(String n) {
		super(n);
	}
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("proxy", "MarySpeech", "default speech");
		return peers;
	}
	
	// AudioFile.getGlobalFileCacheDir() + File.separator +
	
	static public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException{
		return  provider.getClass().getSimpleName() 
				+ File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") 
				+ File.separator + DigestUtils.md5Hex(toSpeak) + "." + audioFileType;
	}

	@Override
	public Boolean isSpeaking(Boolean b) {
		return proxy.isSpeaking(b);
	}

	@Override
	public List<String> getVoices() {
		return (List<String>)proxy.getVoices();
	}

	@Override
	public boolean setVoice(String voice) {
		return proxy.setVoice(voice);
	}

	@Override
	public String saying(String t) {
		return proxy.saying(t);
	}

	@Override
	public void setLanguage(String l) {
		proxy.setLanguage(l);
	}

	@Override
	public boolean speak(String toSpeak) {
		return proxy.speak(toSpeak);
	}

	@Override
	public boolean speakBlocking(String toSpeak) {
		return proxy.speakBlocking(toSpeak);
	}
	
	@Override
	public void setVolume(float volume) {
		proxy.setVolume(volume);
	}

	@Override
	public float getVolume() {
		return proxy.getVolume();
	}

	@Override
	public void interrupt() {
		proxy.interrupt();
	}

	@Override
	public String publishStartSpeaking(String utterance) {
		return proxy.publishStartSpeaking(utterance);
	}

	@Override
	public String publishEndSpeaking(String utterance) {
		return proxy.publishEndSpeaking(utterance);
	}

	@Override
	public void onText(String text) {
		((TextListener) proxy).onText(text);
	}

	@Override
	public String getLanguage() {
		return proxy.getLanguage();
	}

	public void startService() {
		super.startService();
		proxy = (SpeechSynthesis) startPeer("proxy");
	}

	public void setSpeechProvider(String serviceTypeName) {
		try {
			if (proxy != null) {
				((ServiceInterface) proxy).releaseService();
			}

			proxy = (SpeechSynthesis) startPeer("proxy", serviceTypeName);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}
	
	public String getSpeechProvider(){
		if (proxy != null){
			return proxy.getClass().getSimpleName();
		}
		
		return null;
	}
	
	public void speakVoices(){
		java.util.List<String> voices = proxy.getVoices();
		for (int i = 0; i < voices.size(); ++i){
			proxy.speakBlocking(String.format("Hello, my speech provider is %s and my voice name is %s -  I am %d of %d voices", getSpeechProvider(), voices.get(i), i+1, voices.size()));
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		// LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {

			// setSpeechType before service is created !
			// Speech.setPeer("proxy", "AcapelaSpeech");

			Speech speech = (Speech) Runtime.create("speech", "Speech");
			speech.setSpeechProvider("AcapelaSpeech");
			speech.startService();
			speech.speakVoices();
			/*
			speech.speak("Hello World");
			speech.setVoice("Rhona");
			sp
			speech.speak(String.format("Hello World from %s, my name is %s  and I have %d different voices", speech.getSpeechProvider(), speech.getVoice(), speech.getVoices().size()));
			*/
			

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public String getVoice() {
		return proxy.getVoice();
	}

}
