package org.myrobotlab.audio;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

// FIXME - make runnable
public class AudioProcessor extends Thread {

  static Logger log = LoggerFactory.getLogger(AudioProcessor.class);

  // REFERENCES -
  // http://www.javalobby.org/java/forums/t18465.html
  // http://sourcecodebrowser.com/libjlayer-java/1.0/classjavazoom_1_1jl_1_1player_1_1_audio_device_base__coll__graph.png
  // - does JavaZoom spi decoder just need to be in the classpath ? - because
  // there is not any direct reference to it
  // it seems to make sense - some how the file gets decoded enough - so that
  // a audio decoder can be slected from some
  // internal registry ... i think

  int currentTrackCount = 0;
  int samplesAdded = 0;

  double volume = 1.0f;

  float balance = 0.0f;

  float targetBalance = balance;

  AudioFile audioFile = null;

  boolean isPlaying = false;

  boolean isRunning = false;

  public String track;

  BlockingQueue<AudioData> queue = new LinkedBlockingQueue<AudioData>();

  AudioData currentAudioData = null;

  private int repeatCount;
  
  public AudioProcessor(AudioFile audioFile, String track) {
    super(String.format("%s:track", track));
    this.audioFile = audioFile;
    this.track = track;
  }

  public AudioData pause(boolean b) {
    if (b) {
      isPlaying = false;
    }
    if (currentAudioData != null) {
      if (b) {
        currentAudioData.waitForLock = new Object();
      } else {
        if (currentAudioData.waitForLock != null) {
          synchronized (currentAudioData.waitForLock) {
            currentAudioData.waitForLock.notifyAll();
            currentAudioData.waitForLock = null; // removing reference
          }
        }
      }
    }
    return currentAudioData;
  }

  // FIXME play with this thread - but block incoming thread
  // FIXME - AudioData should have InputStream not File !
  public AudioData play(AudioData data) {

    log.debug("playing {}", data.toString());
    // FIXME - won't close filehandles :( .. dunno why
    // FileInputStream fis = null;
    // BufferedInputStream bis = null;
    AudioInputStream din = null;
    try {

      AudioInputStream in = null;

      if (data.getFileName() != null) {
        File file = new File(data.getFileName());
        if (file.length() == 0) {
          audioFile.error(String.format("audio file %s 0 byte length", file.getName()));
          return data;
        }
        /*
         * fis = new FileInputStream(file); bis = new BufferedInputStream(fis);
         * in = AudioSystem.getAudioInputStream(bis);
         */

        in = AudioSystem.getAudioInputStream(file);

      } else if (data.inputStream != null) {
        in = AudioSystem.getAudioInputStream(data.inputStream);
      }

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

        audioFile.invoke("publishAudioStart", data);

        while (isPlaying && (nBytesRead = din.read(buffer, 0, buffer.length)) != -1) {
          // byte[] goofy = new byte[4096];
          /*
           * HEE HEE .. if you want to make something sound "bad" i'm sure its
           * clipping as 130 pushes some of the values over the high range for
           * (int i = 0; i < data.length; ++i){ data[i] = (byte)(data[i] + 130);
           * }
           */

          if (data.waitForLock != null) {
            // Object lock = myService.getLock(queueName); // getLocks my lock
            synchronized (data.waitForLock) {
              log.info("pausing");
              data.waitForLock.wait();
            }
          }

          if (data.volume == null) {
            data.volume = volume;
          }

          if (data.volume != null) {

            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {

              FloatControl ctrl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
              // float scaled = (float) (Math.log(data.volume) / Math.log(10.0)
              // * 20.0);

              if (MathUtils.round(ctrl.getValue(), 3) != MathUtils.round((float) (ctrl.getMinimum() + ((double) (ctrl.getMaximum() - ctrl.getMinimum()) * data.volume)), 3)) {
                if (data.volume <= 1.0f && data.volume >= 0) {

                  ctrl.setValue((float) (ctrl.getMinimum() + ((double) (ctrl.getMaximum() - ctrl.getMinimum()) * data.volume)));
                  log.debug("Audioprocessor set volume to : " + ctrl.getValue());
                } else {
                  log.error("Requested volume value " + data.volume.toString() + " not allowed");
                  data.volume = 1.0;
                }
              }
              // volume.setValue(scaled);
            }
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

          if (audioFile.isMute()) {
            // NoOp for a mute audioFile.
          } else {
            line.write(buffer, 0, nBytesRead);
          }

        }
        // Stop

        line.drain();
        line.stop();
        line.close();
        din.close();
        in.close();
        /*
         * if (bis != null) bis.close(); if (fis != null) fis.close();
         */

        // System.gc();

        audioFile.invoke("publishAudioEnd", data);

        synchronized (data) {
          log.debug("notifying others");
          data.notifyAll();
        }

      } else {
        log.error("line is null !");
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      if (audioFile != null) {
    	if (e instanceof LineUnavailableException) {
    		audioFile.error("line is not available");
    	} else {
    		audioFile.error("%s - %s", e.getMessage(), data.getFileName());
    	}
      }
    } finally {
      if (din != null) {
        try {
          din.close();
        } catch (IOException e) {
        }
      }
    }

    return data;
  }

  @Override
  public void run() {
    isRunning = true;

    try {
      AudioData data = null;
      int lastTrackPlayed = currentTrackCount;
      while (isRunning) {
        // FIXME - timeSinceLastFinishedSample (collect all time)

        // (repeat != null && repeatCount < repeat || repeat == -1)
        // grab new AudioData job ! (new sample on track)
        // data == null first sample we'll play
        // || we are done repeating
        if (data == null || data.repeat == null || (data.repeat != -1 && data.repeat <= repeatCount)) {
          data = queue.take();
          currentAudioData = data;
          ++currentTrackCount;
          data.startTs = System.currentTimeMillis();
          repeatCount = 0;
        }

        // check to see if we should be waiting for another track to finish
        // FIXME - REMOVE ! NO KEYS JUST LOCK ON OBJECT !!!!
        /*
         * if (waitForKey != null) { Object waitForLock =
         * audioFile.getWaitForLock(waitForKey); synchronized (waitForLock) {
         * waitForLock.wait(); } }
         */

        play(data);
        ++repeatCount;

        // FIXME - DONT USE KEYS !! DONT USE AUDIOFILE !! LOCK ON OBJECT IN DATA
        // !!!
        /*
         * if (currentTrackCount != lastTrackPlayed) { String key =
         * String.format("%s:%s", queueName, currentTrackCount); Object
         * waitForLock = audioFile.getWaitForLock(key); if (waitForLock != null)
         * { synchronized (waitForLock) { waitForLock.notify(); } } }
         */

        data.stopTs = System.currentTimeMillis();

      }
    } catch (Exception e) {
      isRunning = false;
    }
    // default waits on queued audio requests
    log.info("audio processor {} exiting", getName());
  }

  public void setVolume(double volume) {
    this.volume = volume;
  }

  /*
   * <pre> public static void main(String[] args) {
   * 
   * AudioPlayer player = new AudioPlayer();
   * 
   * // jlp.play("NeroSoundTrax_test1_PCM_Stereo_CBR_16SS_6000Hz.wav");
   * AudioData data = new AudioData("aaa.mp3"); // data.volume = 120.0f;
   * data.balance = -1;
   * 
   * player.play(data);
   * 
   * } </pre>
   */

  public double getVolume() {
    return volume;
  }

  public AudioData add(AudioData data) {
    ++samplesAdded;
    queue.add(data); // COOL - should change state to QUEUED'
    return data;
  }

  public boolean isRunning() {
    return isRunning;
  }

  /**
   * number of samples currently queued to be played
   * 
   * @return - number of queued samples
   */
  public int getTrackSize() {
    return queue.size();
  }

  public void stopPlaying() {
    isPlaying = false;
    isRunning = false;
  }

}
