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
import java.util.HashSet;
import java.util.List;

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
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;

/**
 * AcapelaSpeech
 * 
 */
public class AcapelaSpeech extends Proxy implements TextListener, SpeechSynthesis {

	private static final long serialVersionUID = 1L;

	String voice = "Tyler";
	HashSet<String> voices = new HashSet<String>();

	String pathPrefix = null;

	PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();

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
	}

	public void startService() {
		super.startService();
		startPeer("audioFile");
		audioFile.startService();
	}

	public AudioFile getAudioFile() {
		return audioFile;
	}

	@Override
	public Boolean isSpeaking(Boolean b) {
		return b;
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
	public String saying(String speech) {
		return speech;
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
			nvps.add(new BasicNameValuePair("MyLanguages", "sonid8"));
			nvps.add(new BasicNameValuePair("MySelectedVoice", voice));
			nvps.add(new BasicNameValuePair("MyTextForTTS", toSpeak));
			nvps.add(new BasicNameValuePair("t", "1"));
			nvps.add(new BasicNameValuePair("SendToVaaS", ""));
			post.setEntity(new UrlEncodedFormEntity(nvps));
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
	public boolean speakBlocking(String toSpeak) {

		invoke("isSpeaking", true);
		invoke("saying", toSpeak);
		// audioFile.playFile(audioFileName, true);
		// sleep(afterSpeechPause);// important pause after speech
		invoke("isSpeaking", false);
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
		return null;
	}

	@Override
	public String publishEndSpeaking(String utterance) {
		return null;
	}

	@Override
	public void onText(String text) {

	}

	@Override
	public String getLanguage() {
		return null;
	}

	public int speak(String toSpeak) {
		try {

			String filename = Speech.getLocalFileName(this, toSpeak, "mp3");

			if (audioFile.cacheContains(filename)) {
				return audioFile.playCachedFile(filename);
			}

			byte[] b = getRemoteFile(toSpeak);
			audioFile.cache(filename, b);
			return audioFile.playCachedFile(filename);

		} catch (Exception e) {
			Logging.logError(e);
		}

		return -1;
	}

	public int speak(String voice, String toSpeak) {
		setVoice(voice);
		return speak(toSpeak);
	}

}
