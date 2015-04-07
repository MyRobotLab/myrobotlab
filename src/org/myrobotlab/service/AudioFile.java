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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import org.myrobotlab.audio.MRLSoundAudioDevice;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class AudioFile extends Service {

	// The myrobotlab audio device
	private MRLSoundAudioDevice audioDevice = new MRLSoundAudioDevice();
	
	public class AdvancedPlayerThread extends Thread {
		AdvancedPlayer player = null;
		String filename;

		public AdvancedPlayerThread(String filename, BufferedInputStream bis) {
			super(filename);

			try {
				this.filename = filename;			
				resetAudioDevice();
				this.player = new AdvancedPlayer(bis, audioDevice);
				player.setPlayBackListener(playbackListener);
				
			} catch (Exception e) {
				Logging.logError(e);
			}
		}

		@Override
		public void run() {
			try {
				invoke("started");
				player.play();
				invoke("stopped");
				invoke("stoppedFile", filename);
			} catch (Exception e) {
				Logging.logError(e);
			}
		}
	}

	public class AePlayWave extends Thread {

		private String filename;

		private Position curPosition;

		private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

		public AePlayWave() {
		}

		public AePlayWave(String wavfile) {
			filename = wavfile;
			curPosition = Position.NORMAL;
		}

		public AePlayWave(String wavfile, Position p) {
			filename = wavfile;
			curPosition = p;
		}

		public void playAeWavFile(String filename) {
			playAeWavFile(filename, Position.LEFT);
		}

		public void playAeWavFile(String filename, Position p) {

			this.filename = filename;
			this.curPosition = p;

			File soundFile = new File(filename);
			if (!soundFile.exists()) {
				System.err.println("Wave file not found: " + filename);
				return;
			}

			AudioInputStream audioInputStream = null;
			try {
				audioInputStream = AudioSystem.getAudioInputStream(soundFile);
			} catch (UnsupportedAudioFileException e1) {
				e1.printStackTrace();
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}

			AudioFormat format = audioInputStream.getFormat();
			SourceDataLine auline = null;
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			try {
				auline = (SourceDataLine) AudioSystem.getLine(info);
				auline.open(format);
			} catch (LineUnavailableException e) {
				logException(e);
				return;
			} catch (Exception e) {
				logException(e);
				return;
			}

			if (auline.isControlSupported(FloatControl.Type.PAN)) {
				FloatControl pan = (FloatControl) auline.getControl(FloatControl.Type.PAN);
				if (curPosition == Position.RIGHT)
					pan.setValue(1.0f);
				else if (curPosition == Position.LEFT)
					pan.setValue(-1.0f);
			}

			auline.start();
			int nBytesRead = 0;
			byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

			try {
				while (nBytesRead != -1) {
					nBytesRead = audioInputStream.read(abData, 0, abData.length);
					if (nBytesRead >= 0)
						auline.write(abData, 0, nBytesRead);
				}
			} catch (IOException e) {
				logException(e);
				return;
			} finally {
				auline.drain();
				auline.close();
			}

		}

		public Boolean playingFile(Boolean b) {
			return b;
		}

		// for non-blocking use
		@Override
		public void run() {
			playAeWavFile(filename, curPosition);
		}
	}

	enum Position {
		LEFT, RIGHT, NORMAL
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(AudioFile.class.getCanonicalName());

	transient AePlayWave wavPlayer = new AePlayWave();

	int pausedOnFrame = 0;

	transient PlaybackListener playbackListener = new PlaybackListener() {
		@Override
		public void playbackFinished(PlaybackEvent event) {
			pausedOnFrame = event.getFrame();
		}
	};
	// FIXME -
	// http://alvinalexander.com/java/java-audio-example-java-au-play-sound - so
	// much more simple
	// http://stackoverflow.com/questions/198679/convert-audio-stream-to-wav-byte-array-in-java-without-temp-file
	// REMOVE AePlayWave !!!

	// public transient HashMap<String, AdvancedPlayerThread> players = new
	// HashMap<String, AdvancedPlayerThread>();
	public transient List<AdvancedPlayerThread> players = Collections.synchronizedList(new ArrayList<AdvancedPlayerThread>());

	// http://stackoverflow.com/questions/14085199/mp3-to-wav-conversion-in-java
	// TODO - great method .. although why pass in the audio format ???
	public static byte[] convert(byte[] sourceBytes, AudioFormat audioFormat) {
		if (sourceBytes == null || sourceBytes.length == 0 || audioFormat == null) {
			throw new IllegalArgumentException("Illegal Argument passed to this method");
		}

		ByteArrayInputStream bais = null;
		ByteArrayOutputStream baos = null;
		AudioInputStream sourceAIS = null;
		AudioInputStream convert1AIS = null;
		AudioInputStream convert2AIS = null;

		try {
			bais = new ByteArrayInputStream(sourceBytes);
			sourceAIS = AudioSystem.getAudioInputStream(bais);
			AudioFormat sourceFormat = sourceAIS.getFormat();
			AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(),
					sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
			convert1AIS = AudioSystem.getAudioInputStream(convertFormat, sourceAIS);
			convert2AIS = AudioSystem.getAudioInputStream(audioFormat, convert1AIS);

			baos = new ByteArrayOutputStream();

			byte[] buffer = new byte[8192];
			while (true) {
				int readCount = convert2AIS.read(buffer, 0, buffer.length);
				if (readCount == -1) {
					break;
				}
				baos.write(buffer, 0, readCount);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			Logging.logError(e);
			return null;
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (Exception e) {
				}
			}
			if (convert2AIS != null) {
				try {
					convert2AIS.close();
				} catch (Exception e) {
				}
			}
			if (convert1AIS != null) {
				try {
					convert1AIS.close();
				} catch (Exception e) {
				}
			}
			if (sourceAIS != null) {
				try {
					sourceAIS.close();
				} catch (Exception e) {
				}
			}
			if (bais != null) {
				try {
					bais.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {

			AudioFile af = (AudioFile) Runtime.createAndStart("audio", "AudioFile");
			af.playFile("C:\\dev\\workspace.kmw\\myrobotlab\\test.mp3", false, false);
			af.setVolume(1.0F);
			
			
			if (false) {
			af.silence();

			af.convert("C:\\tools\\Tarsos-master\\test.wav");

			Joystick joystick = (Joystick) Runtime.createAndStart("joy", "Joystick");
			Python python = (Python) Runtime.createAndStart("python", "Python");
			AudioFile player = new AudioFile("player");
			// player.playFile(filename, true);
			player.startService();
			GUIService gui = (GUIService) Runtime.createAndStart("gui", "GUIService");

			joystick.setController(2);
			joystick.broadcastState();

			python.subscribe(joystick.getName(), "button1", "input");

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

	public AudioFile(String n) {
		super(n);
	}

	public void convert(String filename) {
		try {
			File soundFile = new File(filename);
			FileInputStream fileStream = null;
			if (!soundFile.exists()) {
				System.err.println("Wave file not found: " + filename);
				return;
			}

			AudioInputStream audioInputStream = null;

			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
			fileStream = new FileInputStream(soundFile);

			AudioFormat format = audioInputStream.getFormat();

			byte[] bytes = convert(FileIO.toByteArray(fileStream), format);

			FileOutputStream fileOuputStream = new FileOutputStream("out.wav");
			fileOuputStream.write(bytes);
			fileOuputStream.close();

		} catch (Exception e) {
			Logging.logError(e);
			return;
		}

	}

	@Override
	public String[] getCategories() {
		return new String[] { "sound" };
	}

	@Override
	public String getDescription() {
		return "Plays back audio file. Can block or multi-thread play";
	}

	// FIXME - bad assumptions
	public void play(String name) {
		playFile("audioFile/" + name + ".mp3", false);
	}

	public void playBlockingWavFile(String filename) {
		wavPlayer.playAeWavFile(filename);
	}

	public void playFile(String filename) {
		playFile(filename, false);
	}

	/* BEGIN - TODO - reconcile - find how javazoom plays wave */

	public void playFile(String filename, Boolean isBlocking) {
		playFile(filename, isBlocking, false);
	}

	public void playFile(String filename, Boolean isBlocking, Boolean isResource) {
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
				players.add(player);
				player.start();

			} else {
				invoke("started");
				audioDevice.close();
				//audioDevice = new MRLSoundAudioDevice();
				//audioDevice.setGain(this.getVolume());
				// TODO: figure out how to properly just reuse the same sound audio device.
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

	}

	private void resetAudioDevice() {
		float volume = audioDevice.getGain();
		audioDevice = new MRLSoundAudioDevice();
		audioDevice.setGain(volume);
	}

	public void playFileBlocking(String filename) {
		playFile(filename, true);
	}

	public void playResource(String filename) {
		playResource(filename, false);
	};

	public void playResource(String filename, Boolean isBlocking) {
		playFile(filename, isBlocking, true);
	}

	public void playWAV(String name) {
		// new AePlayWave("audioFile/" + name + ".wav").start();
		wavPlayer.playAeWavFile("audioFile/" + name + ".wav");
	}

	public void playWAVFile(String name) {
		// new AePlayWave(name).start();
		wavPlayer.playAeWavFile(name);
	}

	public void silence() {
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
	}

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

	public MRLSoundAudioDevice getAudioDevice() {
		return audioDevice;
	}

	public void setAudioDevice(MRLSoundAudioDevice audioDevice) {
		this.audioDevice = audioDevice;
	}

	/**
	 * Specify the volume for playback on the audio file 
	 * value 0.0 = off  1.0 = normal volume.  
	 * (values greater than 1.0 may distort the original signal)
	 * @param volume
	 */
	public void setVolume(float volume) {
		audioDevice.setGain(volume);
	}
	
	public float getVolume() {
		return audioDevice.getGain();
	}

}
