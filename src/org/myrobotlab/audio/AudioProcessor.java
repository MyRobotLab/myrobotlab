package org.myrobotlab.audio;

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.AudioFile.AudioData;

// FIXME - make runnable
public class AudioProcessor extends Thread {

	// REFERENCES -
	// http://www.javalobby.org/java/forums/t18465.html
	// http://sourcecodebrowser.com/libjlayer-java/1.0/classjavazoom_1_1jl_1_1player_1_1_audio_device_base__coll__graph.png
	// - does JavaZoom spi decoder just need to be in the classpath ? - because
	// there is not any direct reference to it
	// it seems to make sense - some how the file gets decoded enough - so that
	// a audio decoder can be slected from some
	// internal registry ... i think

	Queue<String> commands = new ConcurrentLinkedQueue<String>();

	float volume = 1.0f;
	// float targetVolume = currentVolume;

	float targetVolume = volume;

	float balance = 0.0f;

	float targetBalance = balance;

	AudioFile myService = null;

	public boolean pause = false;
	
	public boolean isPlaying = false;

	public AudioProcessor(AudioFile audioFile) {
		myService = audioFile;
	}

	public void play(AudioData data) {

		// validate data
		targetVolume = data.volume;
		targetBalance = data.balance;

		AudioInputStream din = null;
		try {
			File file = new File(data.fileName);
			AudioInputStream in = AudioSystem.getAudioInputStream(file);
			AudioFormat baseFormat = in.getFormat();
			AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(), false);
			din = AudioSystem.getAudioInputStream(decodedFormat, in);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

			if (line != null) {
				line.open(decodedFormat);
				byte[] buffer = new byte[4096];

				// Start
				line.start();

				int nBytesRead = 0;
				isPlaying = true;
				while (isPlaying && (nBytesRead = din.read(buffer, 0, buffer.length)) != -1) {
					// byte[] goofy = new byte[4096];
					/*
					 * HEE HEE .. if you want to make something sound "bad" i'm
					 * sure its clipping as 130 pushes some of the values over
					 * the high range for (int i = 0; i < data.length; ++i){
					 * data[i] = (byte)(data[i] + 130); }
					 */

					if (volume != targetVolume) {
						if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {

							FloatControl volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
							float scaled = (float) (Math.log(targetVolume) / Math.log(10.0) * 20.0);
							volume.setValue(scaled); 
						}
						volume = targetVolume;
					}

					if (balance != targetBalance) {
						try {
							FloatControl control = (FloatControl) line.getControl(FloatControl.Type.BALANCE);
							control.setValue(balance);
							balance = targetBalance;
						} catch (Exception e) {
							Logging.logError(e);
						}
					}

					// BooleanControl
					// muteControl=(BooleanControl)source.getControl(BooleanControl.Type.MUTE);
					/*
					 * if (volume == 0) { muteControl.setValue(true); }
					 */

					// the buffer of raw data could be published from here
					// if a reference of the service is passed in

					line.write(buffer, 0, nBytesRead);

					if (pause) {
						Object lock = myService.getLock();
						synchronized (lock) {
							lock.wait();
						}
					}
				}
				// Stop
				line.drain();
				line.stop();
				line.close();
				din.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (din != null) {
				try {
					din.close();
				} catch (IOException e) {
				}
			}
		}
	}

	boolean isRunning = false;

	@Override
	public void run() {
		isRunning = true;

		try {
			while (isRunning) {
				
				if (pause) {
					Object lock = myService.getLock();
					synchronized (lock) {
						lock.wait();
					}
				}

				AudioData data = myService.getNextAudioData();
				play(data);
			}
		} catch (Exception e) {
			isRunning = false;
		}
		// default waits on queued audio requests

	}

	public void setVolume(float volume) {
		targetVolume = volume;
	}

	public static void main(String[] args) {
		/*
		 * AudioPlayer player = new AudioPlayer();
		 * 
		 * // jlp.play("NeroSoundTrax_test1_PCM_Stereo_CBR_16SS_6000Hz.wav");
		 * AudioData data = new AudioData("aaa.mp3"); // data.volume = 120.0f;
		 * data.balance = -1;
		 * 
		 * player.play(data);
		 */
	}

	public float getVolume() {
		return volume;
	}

}
