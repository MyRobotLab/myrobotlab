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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.audio.AudioProcessor;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class AudioFile extends Service {
	static final long serialVersionUID = 1L;
	static Logger log = LoggerFactory.getLogger(AudioFile.class);

	// FIXME -
	// http://alvinalexander.com/java/java-audio-example-java-au-play-sound - so
	// much more simple
	// http://stackoverflow.com/questions/198679/convert-audio-stream-to-wav-byte-array-in-java-without-temp-file

	static String globalFileCacheDir = "audioFile";
	
	BlockingQueue<AudioData> audioQueue = new LinkedBlockingQueue<AudioData>();
	

	AudioProcessor audioProcessor = null;
	
	Object lock = new Object();
	
	static public class AudioData {
		
		/**
		 * mode can be either QUEUED MULTI PRIORITY INTERRUPT OR BLOCKING
		 */
		String mode = "QUEUED"; 
		public String fileName = null;
		public float volume = 1.0f;
		public float balance = 0.0f;
		
		public AudioData(String fileName) {
			this.fileName = fileName;
		}
	}

	public AudioFile(String n) {
		super(n);
	}
	
	public void startService(){
		
	}

	public void play(String filename) {
		// use File interface such that filename is preserved
		// but regardless of location (e.g. url, local, resource)
		// or type (mp3 wav) a stream is opened and the
		// pair is put on a queue to be played
		
		playFile("audioFile/" + filename + ".mp3", false);
	}
	
	public void play(AudioData data) {
		// use File interface such that filename is preserved
		// but regardless of location (e.g. url, local, resource)
		// or type (mp3 wav) a stream is opened and the
		// pair is put on a queue to be played
		
		if ("QUEUED".equals(data.mode)){
			// stick it on top of queue and let our default player play it
			if (audioProcessor == null){
				audioProcessor = new AudioProcessor(this);
				audioProcessor.start();
			}
			
			audioQueue.add(data);
			
		}				
	}
	
	public void pause(){
		// TODO - more than just default players
		// pause the current song
		audioProcessor.pause = true;
	}
	
	public void resume(){
		// TODO - more than just default players
		audioProcessor.pause = false;
		synchronized (lock) {
			lock.notify();
		}
	}

	public void playFile(String filename) {
		playFile(filename, false);
	}

	public void playFile(String filename, Boolean isBlocking) {
		/*
		try {

			InputStream is;
			if (isResource) {
				is = AudioFile.class.getResourceAsStream(filename);
			} else {
				is = new FileInputStream(filename);
			}

			if (!isBlocking) {
				BufferedInputStream bis = new BufferedInputStream(is);
				AdvancedPlayerThread player = new AdvancedPlayerThread(filename, bis);
				// players.put(filename, player);
				// players.add(player);
				player.start();

			} else {
				invoke("started");
				audioDevice.close();
				// audioDevice = new MRLSoundAudioDevice();
				// audioDevice.setGain(this.getVolume());
				// TODO: figure out how to properly just reuse the same sound
				// audio device.
				// for now, it seems we need to pass a new one each time.
				resetAudioDevice();
				AdvancedPlayer player = new AdvancedPlayer(is, audioDevice);
				player.setPlayBackListener(playbackListener);
				player.play();
				invoke("stopped");
				invoke("stoppedFile", filename);
			}

		} catch (Exception e) {
			Logging.logError(e);
			error("Problem playing file ", filename);
			return;
		}
	*/
	}

	public void playFileBlocking(String filename) {
		playFile(filename, true);
	}

	public void playResource(String filename) {
		playResource(filename, false);
	};

	public void playResource(String filename, Boolean isBlocking) {
		//playFile(filename, isBlocking, true);
	}

	public void silence() {
		/*
		Iterator<AdvancedPlayerThread> iter = players.iterator();
		while (iter.hasNext()) {
			try {
				AdvancedPlayerThread player = iter.next();
				player.player.close();
				player.interrupt();
			} catch (Exception e) {
				Logging.logError(e);
			}
			iter.remove();
		}
		*/
	}

	// refactor String publishPlaying(String name)
	public void started() {
		log.info("started");
	}

	public void stopped() {
		log.info("stopped");
	}

	public String stoppedFile(String filename) {
		log.info("stoppedFile {}", filename);
		return filename;
	}

	/**
	 * Specify the volume for playback on the audio file value 0.0 = off 1.0 =
	 * normal volume. (values greater than 1.0 may distort the original signal)
	 * 
	 * @param volume
	 */
	public void setVolume(float volume) {
		audioProcessor.setVolume(volume);
	}

	public float getVolume() {
		return audioProcessor.getVolume();
	}


	public boolean cacheContains(String filename) {
		File file = new File(globalFileCacheDir + File.separator + filename);
		return file.exists();
	}

	public void playCachedFile(String filename) {
		playFile(globalFileCacheDir + File.separator + filename);
	}

	public void cache(String filename, byte[] data) throws IOException {
		File file = new File(globalFileCacheDir + File.separator + filename);
		File parentDir = new File(file.getParent());
		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(globalFileCacheDir + File.separator + filename);
		fos.write(data);
		fos.close();
	}

	public static String getGlobalFileCacheDir() {
		return globalFileCacheDir;
	}
	
	@Override
	public String[] getCategories() {
		return new String[] { "sound" };
	}

	@Override
	public String getDescription() {
		return "Plays back audio file. Can block or multi-thread play";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			
			// FIXME 
			// TO - TEST
			// file types mp3 wav
			// file locations http resource jar! file cache !!!
			// basic controls - playMulti, playQueue, playBlocking

			AudioFile af = (AudioFile) Runtime.createAndStart("audio", "AudioFile");
			
			AudioData data = new AudioData("scruff.mp3");
			af.play(data);
			af.pause();
			af.resume();
			//af.playFile("C:\\dev\\workspace.kmw\\myrobotlab\\test.mp3", false, false);
			af.setVolume(0.50f);
			
			af.stop();

			boolean test = false;
			if (test) {
				af.silence();

				Joystick joystick = (Joystick) Runtime.createAndStart("joy", "Joystick");
				Python python = (Python) Runtime.createAndStart("python", "Python");
				AudioFile player = new AudioFile("player");
				// player.playFile(filename, true);
				player.startService();
				GUIService gui = (GUIService) Runtime.createAndStart("gui", "GUIService");

				joystick.setController(2);
				joystick.broadcastState();

				// BasicController control = (BasicController) player;

				player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\thump.mp3");
				player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\thump.mp3");
				player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\thump.mp3");
				player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\start.mp3");
				player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\radio.chatter.4.mp3");

				player.silence();

				// player.playResource("Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
				player.playResource("/resource/Clock/tick.mp3");
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
		// player.playBlockingWavFile("I am ready.wav");
		// player.play("hello my name is audery");
		// player.playWAV("hello my name is momo");
	}
	
	public void stop() {
		// dump the current song
		audioProcessor.isPlaying = false;
		// pause the next one if queued
		audioProcessor.pause = true;
	}

	public AudioData getNextAudioData() throws InterruptedException{
		return audioQueue.take();
	}
	
	public Object getLock(){
		return lock;
	}

}
