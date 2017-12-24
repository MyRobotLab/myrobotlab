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
 * The working part of this was eventually traced back to:
 * http://www.developer.com/java/other/article.php/1565671/Java-Sound-An-Introduction.htm
 * And I would like to give all well deserved credit to
 * Richard G. Baldwin's excellent and comprehensive tutorial regarding the many
 * details of sound and Java
 * 
 * */

package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * AudioCapture - a service that can record and playback from a microphone.
 * 
 */
public class AudioCapture extends Service {
  public final static Logger log = LoggerFactory.getLogger(AudioCapture.class.getCanonicalName());

  private static final long serialVersionUID = 1L;

  public static boolean stopCapture = false;

  ByteArrayOutputStream byteArrayOutputStream;

  AudioFormat audioFormat;

  transient TargetDataLine targetDataLine;

  transient AudioInputStream audioInputStream;

  transient SourceDataLine sourceDataLine;

  // Audio format fields
  float sampleRate = 16000.0F;
  // 8000,11025,16000,22050,44100
  int sampleSizeInBits = 16;
  // 8,16
  int channels = 1;
  // 1,2
  boolean signed = true;
  // true,false
  boolean bigEndian = false;
  // true,false

  class CaptureThread extends Thread {
    // An arbitrary-size temporary holding
    // buffer
    byte tempBuffer[] = new byte[10000];

    @Override
    public void run() {
      byteArrayOutputStream = new ByteArrayOutputStream();
      stopCapture = false;
      try {// Loop until stopCapture is set
           // by another thread that
           // services the Stop button.
        while (!stopCapture) {
          // Read data from the internal
          // buffer of the data line.
          int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
          if (cnt > 0) {
            // Save data in output stream
            // object.
            byteArrayOutputStream.write(tempBuffer, 0, cnt);
          } // end if
        } // end while
        targetDataLine.close();
        byteArrayOutputStream.close();
      } catch (Exception e) {
        error(e);
      } // end catch
    }// end run
  }// end inner class CaptureThread

  public class PlayThread extends Thread {
    byte tempBuffer[] = new byte[10000];

    @Override
    public void run() {
      try {
        int cnt;
        // Keep looping until the input
        // read method returns -1 for
        // empty stream.
        while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
          if (cnt > 0) {
            // Write data to the internal
            // buffer of the data line
            // where it will be delivered
            // to the speaker.
            sourceDataLine.write(tempBuffer, 0, cnt);
          } // end if
        } // end while
          // Block and wait for internal
          // buffer of the data line to
          // empty.
        sourceDataLine.drain();
        sourceDataLine.close();
      } catch (Exception e) {
        error(e);
      } // end catch
    }// end run
  }// end inner class PlayThread

  public static void main(String[] args) throws InterruptedException {
    LoggingFactory.init(Level.DEBUG);

    try {

      AudioCapture audioIn = (AudioCapture) Runtime.start("audioIn", "AudioCapture");
      Runtime.start("gui", "SwingGui");
      audioIn.setAudioFormat(16000, 16, 1, true, false);
      audioIn.captureAudio();
      Thread.sleep(3000);
      audioIn.stopAudioCapture();
      audioIn.playAudio();
      audioIn.save("me5.wav");

      audioIn.captureAudio();
      Thread.sleep(3000);
      audioIn.stopAudioCapture();
      audioIn.playAudio();
      audioIn.save("me1.wav");

    } catch (Exception e) {
      Logging.logError(e);
    }

    /*
     * AudioInputStream stream = AudioSystem.getAudioInputStream(new File(
     * "bump.wav")); // stream = AudioSystem.getAudioInputStream(new URL( //
     * "http://hostname/audiofile"));
     * 
     * AudioFormat format = stream.getFormat(); if (format.getEncoding() !=
     * AudioFormat.Encoding.PCM_SIGNED) { format = new
     * AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format .getSampleRate(),
     * format.getSampleSizeInBits() * 2, format .getChannels(),
     * format.getFrameSize() * 2, format.getFrameRate(), true); // big endian
     * stream = AudioSystem.getAudioInputStream(format, stream); }
     * 
     * SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream
     * .getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
     * SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
     * line.open(stream.getFormat()); line.start();
     * 
     * int numRead = 0; byte[] buf = new byte[line.getBufferSize()]; while
     * ((numRead = stream.read(buf, 0, buf.length)) >= 0) { int offset = 0;
     * while (offset < numRead) { offset += line.write(buf, offset, numRead -
     * offset); } } line.drain(); line.stop(); }
     */
  }

  public AudioCapture(String n) {
    super(n);
  }

  public void captureAudio() {
    try {
      // Get everything set up for
      // capture
      audioFormat = getAudioFormat();
      DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
      targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
      targetDataLine.open(audioFormat);
      targetDataLine.start();

      // Create a thread to capture the
      // microphone data and start it
      // running. It will run until
      // the Stop button is clicked.
      Thread captureThread = new Thread(new CaptureThread());
      captureThread.start();
    } catch (Exception e) {
      Logging.logError(e);
    } 
    broadcastState();
    // end catch
  }// end captureAudio method

  // This method creates and returns an
  // AudioFormat object for a given set
  // of format parameters. If these
  // parameters don't work well for
  // you, try some of the other
  // allowable parameter values, which
  // are shown in comments following
  // the declarations.
  private AudioFormat getAudioFormat() {
    // new AudioFormat(Format PCM ULAW UNSIGNED etc ..)
    return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
  }// end getAudioFormat

  public void setAudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
    this.sampleRate = sampleRate;
    this.sampleSizeInBits = sampleSizeInBits;
    this.channels = channels;
    this.signed = signed;
    this.bigEndian = bigEndian;
  }

  // This method plays back the audio
  // data that has been saved in the
  // ByteArrayOutputStream
  public void playAudio() {
    try {
      // Get everything set up for
      // playback.
      // Get the previously-saved data
      // into a byte array object.
      byte audioData[] = byteArrayOutputStream.toByteArray();
      // Get an input stream on the
      // byte array containing the data
      InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
      AudioFormat audioFormat = getAudioFormat();
      audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length / audioFormat.getFrameSize());
      DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
      sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
      sourceDataLine.open(audioFormat);
      sourceDataLine.start();

      // Create a thread to play back
      // the data and start it
      // running. It will run until
      // all the data has been played
      // back.
      Thread playThread = new Thread(new PlayThread());
      playThread.start();
    } catch (Exception e) {
      error(e);
    } // end catch
  }// end playAudio

  public ByteArrayOutputStream publishCapture() {
    return byteArrayOutputStream;
  }

  public void save(String filename) throws IOException {
    byte[] data = byteArrayOutputStream.toByteArray();
    AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), audioFormat, data.length);
    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
  }

  public void stopAudioCapture() {
    targetDataLine.stop();
    stopCapture = true;
    broadcastState();
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(AudioCapture.class.getCanonicalName());
    meta.addDescription("captures and stores audio from microphone");
    meta.addCategory("sound");
    return meta;
  }

}
