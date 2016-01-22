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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;

/**
 * AcapelaSpeech
 * 
 */
public class AcapelaSpeech extends Service implements TextListener, SpeechSynthesis {

	private static final long serialVersionUID = 1L;

	String voice = "Ryan";
	HashSet<String> voices = new HashSet<String>();

	String pathPrefix = null;

	transient PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();

	// FIXME - PEER notation
	transient AudioFile audioFile = new AudioFile("audioFile");

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("audioFile", "AudioFile", "audioFile");

		return peers;
	}

	public AcapelaSpeech(String n) {
		super(n);
		connectionManager.setMaxTotal(10);

		voices.add("Leila");
		voices.add("Laia");
		voices.add("Eliska");
		voices.add("Mette");
		voices.add("Zoe");
		voices.add("Jasmijn");
		voices.add("Tyler");
		voices.add("Deepa");
		voices.add("Rhona");
		voices.add("Rachel");
		voices.add("Sharon");
		voices.add("Hanna");
		voices.add("Sanna");
		voices.add("Justine");
		voices.add("Louise");
		voices.add("Manon");
		voices.add("Claudia");
		voices.add("Dimitris");
		voices.add("Fabiana");
		voices.add("Sakura");
		voices.add("Minji");
		voices.add("Lulu");
		voices.add("Bente");
		voices.add("Monika");
		voices.add("Marcia");
		voices.add("Celia");
		voices.add("Alyona");
		voices.add("Biera");
		voices.add("Ines");
		voices.add("Rodrigo");
		voices.add("Elin");
		voices.add("Samuel");
		voices.add("Kal");
		voices.add("Mia");
		voices.add("Ipek");
		voices.add("Ryan");
	}

	public void startService() {
		super.startService();
		startPeer("audioFile");
		audioFile.startService();
		// attach a listener when the audio file ends playing.
		audioFile.addListener("finishedPlaying", this.getName(), "publishEndSpeaking");
		
	}

	public AudioFile getAudioFile() {
		return audioFile;
	}


	@Override
	public ArrayList<String> getVoices() {
		return new ArrayList<String>(voices);
	}

	@Override
	public String getVoice() {
		return voice;
	}

	@Override
	public boolean setVoice(String voice) {
		this.voice = voice;
		return voices.contains(voice);
	}

	@Override
	public void setLanguage(String l) {
		// FIXME ! "MyLanguages", "sonid8" ???
		// FIXME - implement !!!
	}

	public String getMp3Url(String toSpeak) {
		HttpPost post = null;

		try {

			HttpClient client = new DefaultHttpClient(connectionManager);

			// request form & send text
			String url = "http://www.acapela-group.com/demo-tts/DemoHTML5Form_V2.php?langdemo=Powered+by+%3Ca+href%3D%22http%3A%2F%2Fwww.acapela-vaas.com"
					+ "%22%3EAcapela+Voice+as+a+Service%3C%2Fa%3E.+For+demo+and+evaluation+purpose+only%2C+for+commercial+use+of+generated+sound+files+please+go+to+"
					+ "%3Ca+href%3D%22http%3A%2F%2Fwww.acapela-box.com%22%3Ewww.acapela-box.com%3C%2Fa%3E";

			post = new HttpPost(url);
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("MyLanguages", "sonid10"));
			nvps.add(new BasicNameValuePair("MySelectedVoice", voice));
			nvps.add(new BasicNameValuePair("MyTextForTTS", toSpeak));
			nvps.add(new BasicNameValuePair("t", "1"));
			nvps.add(new BasicNameValuePair("SendToVaaS", ""));
			UrlEncodedFormEntity formData = new UrlEncodedFormEntity(nvps, "UTF-8");
			post.setEntity(formData);
			HttpResponse response = client.execute(post);

			log.info(response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();

			byte[] b = FileIO.toByteArray(entity.getContent());

			/*
			 * form response with javascript redirect to mp3 url long ts =
			 * System.currentTimeMillis(); FileOutputStream fos = new
			 * FileOutputStream(String.format("response.%d.html", ts));
			 * fos.write(b); fos.close();
			 */

			// parse out mp3 file url
			String mp3Url = null;
			String data = new String(b);
			String startTag = "var myPhpVar = '";
			int startPos = data.indexOf(startTag);
			if (startPos != -1) {
				int endPos = data.indexOf("';", startPos);
				if (endPos != -1) {
					mp3Url = data.substring(startPos + startTag.length(), endPos);
				}
			}

			if (mp3Url == null) {
				error("could not get mp3 back from Acapela server !");
			}

			return mp3Url;

		} catch (Exception e) {
			Logging.logError(e);
		} finally {
			if (post != null) {
				post.releaseConnection();
			}
		}

		return null;

	}

	public byte[] getRemoteFile(String toSpeak) {

		String mp3Url = getMp3Url(toSpeak);

		HttpGet get = null;
		byte[] b = null;
		try {
			HttpClient client = new DefaultHttpClient(connectionManager);
			HttpResponse response = null;

			// fetch file
			get = new HttpGet(mp3Url);
			log.info("mp3Url {}", mp3Url);

			// get mp3 file & save to cache
			response = client.execute(get);

			log.info("got {}", response.getStatusLine());
			HttpEntity entity = response.getEntity();

			// cache the mp3 content
			b = FileIO.toByteArray(entity.getContent());

			EntityUtils.consume(entity);
		} catch (Exception e) {
			Logging.logError(e);
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}

		return b;
	}

	@Override
	public boolean speakBlocking(String toSpeak) throws IOException {

		
		// FIXME !!
		speak(toSpeak);
		// audioFile.playFile(to, true);
		// sleep(afterSpeechPause);// important pause after speech

		
		
		// invoke("publishEndSpeaking", toSpeak);
		
	 try {
	      Thread.sleep(100);
	      log.info("Done speaking pause 100 ms.");
	  } catch (InterruptedException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	  }
		
		
		return false;
	}

	@Override
	public void setVolume(float volume) {

	}

	@Override
	public float getVolume() {
		return 0;
	}

	@Override
	public void interrupt() {

	}

	@Override
	public String publishStartSpeaking(String utterance) {
		log.info("Acapela Speech publishing Start Speaking: {}", utterance);
		return utterance;
	}

	@Override
	public String publishEndSpeaking(String utterance) {
		log.info("Acapela Speech publishing End Speaking: {}", utterance);
		return utterance;
	}

	@Override
	public void onText(String text) {
        log.info("ON Text Called: {}", text);
        try {
            speak(text);
        } catch (Exception e) {
            Logging.logError(e);
        }
	}

	@Override
	public String getLanguage() {
		return null;
	}

	public int speak(String toSpeak) throws IOException {
		log.info(String.format("speak %s", toSpeak));
		invoke("publishStartSpeaking", toSpeak);
		
		try {
            Thread.sleep(100);
            log.info("Ok.. starting to speak.");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		
			String filename = this.getLocalFileName(this, toSpeak, "mp3");

			if (audioFile.cacheContains(filename)) {
				return audioFile.playCachedFile(filename);
			}

			byte[] b = getRemoteFile(toSpeak);
			audioFile.cache(filename, b);
			
			// TODO: gotta pass a callback down so we know when the file finishes playing.
			return audioFile.playCachedFile(filename);
	}

	public int speak(String voice, String toSpeak) throws IOException {
		setVoice(voice);
		return speak(toSpeak);
	}

	@Override
	public String[] getCategories() {
		// TODO Auto-generated method stub
		return new String[]{"speech"};
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Acapela group speech syntesis service.";
	}

	@Override
    public String getLocalFileName(SpeechSynthesis provider, String toSpeak, String audioFileType) throws UnsupportedEncodingException{
		// TODO: make this a base class sort of thing.
		return  provider.getClass().getSimpleName() 
				+ File.separator + URLEncoder.encode(provider.getVoice(), "UTF-8") 
				+ File.separator + DigestUtils.md5Hex(toSpeak) + "." + audioFileType;
	}

	@Override
	public void addEar(SpeechRecognizer ear) {
		// TODO: move this to a base class. it's basically the same for all mouths/ speech synth stuff.
        // when we add the ear, we need to listen for request confirmation
        addListener("publishStartSpeaking", ear.getName(), "onStartSpeaking");
        addListener("publishEndSpeaking", ear.getName(), "onEndSpeaking");
		
	}

	public void onRequestConfirmation(String text) {
		try {
			speakBlocking(String.format("did you say. %s", text));
		} catch(Exception e){
			Logging.logError(e);
		}
	}

	@Override
	public List<String> getLanguages() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			Runtime.start("webgui","WebGui");
			AcapelaSpeech speech = (AcapelaSpeech)Runtime.start("speech", "AcapelaSpeech");
			speech.speak("what is going on");
			//speech.speakBlocking("Répète après moi");
			speech.speak("hello there my name is ryan");
			speech.speak("hello world");
			speech.speak("one two three four");
			// arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
			// arduino.connect(port);
			// arduino.broadcastState();
		} catch(Exception e){
			Logging.logError(e);
		}
		
	}
	
}
