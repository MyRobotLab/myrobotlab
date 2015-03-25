/**
 * 
 * Thanks Tritonus.org for handling byte float buffer array complexities
 * Thanks Minim (http://code.compartmental.net/tools/minim/) for an example of
 * root mean squared example of getLevel() an of course
 * Thanks Google !
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
 * The working part of this was eventually traced back to:
 * http://www.developer.com/java/other/article.php/1565671/Java-Sound-An-Introduction.htm
 * And I would like to give all well deserved credit to
 * Richard G. Baldwin's excellent and comprehensive tutorial regarding the many
 * details of sound and Java
 * 
 * References :
 *  http://www.jsresources.org/faq_audio.html#calculate_power
 *  http://stackoverflow.com/questions/1026761/how-to-convert-a-byte-array-to-its-numeric-value-java
 *  http://www.daniweb.com/software-development/java/code/216874
 *  http://code.google.com/apis/language/translate/v2/using_rest.html
 * 
 * */

package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javaFlacEncoder.FLAC_FileEncoder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.speech.TranscriptionThread;
import org.slf4j.Logger;
import org.tritonus.share.sampled.FloatSampleBuffer;

public class GoogleSTT extends Service implements SpeechRecognizer {

	/**
	 * @author grog Does the audio capturing, rms, and data copying. Should
	 *         probably be refactored into an AudioCaptureThread which could be
	 *         shared with other Services.
	 */
	class CaptureThread extends Thread {
		private Service myService = null;

		CaptureThread(Service s) {
			this(s, s.getName() + "_capture");
		}

		CaptureThread(Service s, String n) {
			super(n);
			myService = s;
		}

		@Override
		public void run() {
			boolean x = true;

			int transcriptionIndex = 0;

			while (x) {
				synchronized (isListening) {
					try {
						// if we are told not to listen - we will
						// wait without pulling data off the targetDataLine
						while (!isListening) {
							isListening.wait();
						}
					} catch (InterruptedException ex) {
						log.debug("capture thread interrupted");
						return;
					}
				}

				int byteBufferSize = buffer.getByteArrayBufferSize(targetDataLine.getFormat());
				rawBytes = new byte[byteBufferSize];// TODO - create buffer here
													// ?
				log.info("starting capture with " + bufferSize + " buffer size and " + byteBufferSize + " byte buffer length");
				byteArrayOutputStream = new ByteArrayOutputStream();
				stopCapture = false; // FIXME - remove

				try {
					while (!stopCapture) {

						// read from the line
						int cnt = targetDataLine.read(rawBytes, 0, rawBytes.length);
						// convert to float samples
						buffer.setSamplesFromBytes(rawBytes, 0, targetDataLine.getFormat(), 0, buffer.getSampleCount());

						rms = level(buffer.getChannel(0)); // cheezy
						if (rms > rmsThreshold) {
							log.info("rms " + rms + " will begin recording ");
							isCapturing = true;
							captureStartTimeMS = System.currentTimeMillis();
						}

						// && isListening && thresholdReached && (listenTime <
						// minListenTime)
						if (cnt > 0 && isCapturing) {
							byteArrayOutputStream.write(rawBytes, 0, cnt);
						}// end if

						captureTimeMS = System.currentTimeMillis() - captureStartTimeMS;

						if (isCapturing == true && captureTimeMS > captureTimeMinimumMS && rms < rmsThreshold) {
							isCapturing = false;
							stopCapture = true;
						}

					}// end while capture

					byteArrayOutputStream.flush();
					byteArrayOutputStream.close();

					++transcriptionIndex;
					saveWavAsFile(byteArrayOutputStream.toByteArray(), audioFormat, "googletts_" + transcriptionIndex + ".wav");
					encoder.encode(new File("googletts_" + transcriptionIndex + ".wav"), new File("googletts_" + transcriptionIndex + ".flac"));
					transcribe("googletts_" + transcriptionIndex + ".flac");
					stopCapture = false;

				} catch (Exception e) {
					log.error(Service.stackToString(e));
				}

			}// while (isRunning)
		} // run
	} // CaptureThread

	public final static Logger log = LoggerFactory.getLogger(GoogleSTT.class.getCanonicalName());

	private static final long serialVersionUID = 1L;

	// microphone capture
	boolean stopCapture = false;

	transient ByteArrayOutputStream byteArrayOutputStream;

	transient AudioFormat audioFormat;

	transient TargetDataLine targetDataLine;
	transient AudioInputStream audioInputStream;
	transient SourceDataLine sourceDataLine;

	transient CaptureThread captureThread = null;

	String language = "en";

	private Boolean isListening = true;
	// audio format
	float sampleRate = 8000.0F; // 8000,11025,16000,22050,44100
	int sampleSizeInBits = 16; // 8,16
	int channels = 1; // 1,2 TODO - check for 2 & triangulation
	boolean signed = true; // true,false

	boolean bigEndian = false;
	// transcribing
	public final static int SUCCESS = 1;
	public final static int ERROR = 2;
	public final static int TRANSCRIBING = 3;

	transient TranscriptionThread transcription = null;

	// encoding
	transient FLAC_FileEncoder encoder; // TODO - memory encoder
	// root mean square level detection and capture management
	// TODO - auto-gain adjustment
	float rms;
	float rmsThreshold = 0.0050f;
	transient public byte[] rawBytes;
	boolean isCapturing = false;
	long captureStartTimeMS;
	long captureTimeMinimumMS = 1200;
	long captureTimeMS;
	transient private FloatSampleBuffer buffer;

	private int bufferSize = 512; // TODO - experiment with sampling size

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		GoogleSTT stt = new GoogleSTT("stt");
		// stt.startService();
		stt.captureAudio();
		stt.stopAudioCapture();
	}

	public static void saveWavAsFile(byte[] byte_array, AudioFormat audioFormat, String file) {
		try {
			long length = byte_array.length / audioFormat.getFrameSize();
			ByteArrayInputStream bais = new ByteArrayInputStream(byte_array);
			AudioInputStream audioInputStreamTemp = new AudioInputStream(bais, audioFormat, length);
			File fileOut = new File(file);
			AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

			if (AudioSystem.isFileTypeSupported(fileType, audioInputStreamTemp)) {
				AudioSystem.write(audioInputStreamTemp, fileType, fileOut);
			}
		} catch (Exception e) {
		}
	}

	/*
	 * public double volumeRMS(double[] raw) { double sum = 0d; if
	 * (raw.length==0) { return sum; } else { for (int ii=0; ii<raw.length;
	 * ii++) { sum += raw[ii]; } } double average = sum/raw.length;
	 * 
	 * double[] meanSquare = new double[raw.length]; double sumMeanSquare = 0d;
	 * for (int ii=0; ii<raw.length; ii++) { sumMeanSquare +=
	 * Math.pow(raw[ii]-average,2d); meanSquare[ii] = sumMeanSquare; } double
	 * averageMeanSquare = sumMeanSquare/raw.length; double rootMeanSquare =
	 * Math.pow(averageMeanSquare,0.5d);
	 * 
	 * return rootMeanSquare; }
	 */
	public static double toDouble(byte[] data) {
		if (data == null || data.length != 8)
			return 0x0;
		return Double.longBitsToDouble(toLong(data));
	}

	public static int toInt(byte[] bytes) {
		int result = 0;
		for (int i = 0; i < 4; i++) {
			result = (result << 8) - Byte.MIN_VALUE + bytes[i];
		}
		return result;
	}

	public static long toLong(byte[] data) {
		if (data == null || data.length != 8)
			return 0x0;
		return (long) (0xff & data[0]) << 56 | (long) (0xff & data[1]) << 48 | (long) (0xff & data[2]) << 40 | (long) (0xff & data[3]) << 32
				| (long) (0xff & data[4]) << 24 | (long) (0xff & data[5]) << 16 | (long) (0xff & data[6]) << 8 | (long) (0xff & data[7]) << 0;
	}

	public GoogleSTT(String n) {
		super(n);
		encoder = new FLAC_FileEncoder();
	}

	public void captureAudio() {
		try {
			audioFormat = getAudioFormat();
			log.info("sample rate         " + sampleRate);
			log.info("channels            " + channels);
			log.info("sample size in bits " + sampleSizeInBits);
			log.info("signed              " + signed);
			log.info("bigEndian           " + bigEndian);
			log.info("data rate is " + sampleRate * sampleSizeInBits / 8 + " bytes per second");
			// create a data line with parameters
			DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
			// attempt to find & get an input data line with those parameters
			targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			targetDataLine.open(audioFormat);
			targetDataLine.start();

			// create buffer for root mean square level detection
			buffer = new FloatSampleBuffer(targetDataLine.getFormat().getChannels(), bufferSize, targetDataLine.getFormat().getSampleRate());

			// capture from microphone
			captureThread = new CaptureThread(this);
			captureThread.start();
		} catch (Exception e) {
			log.error(Service.stackToString(e));
		}
	}

	private AudioFormat getAudioFormat() {
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "speech recognition" };
	}

	/*
	 * public synchronized float level() { float level = 0; for (int i = 0; i <
	 * samples.length; i++) { level += (samples[i] * samples[i]); } level /=
	 * samples.length; level = (float) Math.sqrt(level); return level; }
	 */
	@Override
	public String getDescription() {
		return "Uses the Google Speech To Text service";
	}

	public float level(float[] samples) {
		float level = 0;
		for (int i = 0; i < samples.length; i++) {
			level += (samples[i] * samples[i]);
		}
		level /= samples.length;
		level = (float) Math.sqrt(level);
		return level;
	}

	/**
	 * Event is sent when the listening Service is actually listening. There is
	 * some delay when it initially loads.
	 */
	@Override
	public void listeningEvent() {
		return;
	}

	@Override
	public void pauseListening() {
		// TODO Auto-generated method stub

	}

	public void publishRecognized(String recognizedText) {
		invoke("recognized", recognizedText);
	}

	@Override
	public String recognized(String word) {
		return word;
	}

	@Override
	public void releaseService() {
		super.releaseService();
		stopAudioCapture();
	}

	@Override
	public void resumeListening() {
		// TODO Auto-generated method stub

	}

	public synchronized boolean setListening(boolean b) {
		isListening = b;
		isListening.notifyAll();
		return b;
	}

	@Override
	public void startListening() {

	}

	public void stopAudioCapture() {
		stopCapture = true;
	}

	@Override
	public void stopListening() {

	}

	private void transcribe(String path) {
		// only interrupt if available
		// transcription.interrupt();

		Logging.logTime("start");
		Logging.logTime("pre new transcription " + path);
		TranscriptionThread transcription = new TranscriptionThread(this, this.getName() + "_transcriber", language);
		transcription.debug = true;
		Logging.logTime("pre new thread start");
		transcription.start();
		Logging.logTime("pre transcription");
		transcription.startTranscription(path);
		Logging.logTime("post transcription");

		// threads.add(transcription);
	}

}
