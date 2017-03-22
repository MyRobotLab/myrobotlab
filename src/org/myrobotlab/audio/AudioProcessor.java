package org.myrobotlab.audio;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
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

  Queue<String> commands = new ConcurrentLinkedQueue<String>();

  int currentTrackCount = 0;
  int trackCount = 0;

  float volume = 1.0f;
  // float targetVolume = currentVolume;

  float targetVolume = volume;

  float balance = 0.0f;

  float targetBalance = balance;

  AudioFile audioFile = null;

  public boolean pause = false;

  public boolean isPlaying = false;

  boolean isRunning = false;

  public String queueName;

  public boolean repeat;

  public String waitForKey = null;

  Object lock = new Object();

  BlockingQueue<AudioData> track = new LinkedBlockingQueue<AudioData>();

  public AudioProcessor(AudioFile audioFile, String queueName) {
    super(String.format("%s:track", queueName));
    this.audioFile = audioFile;
    this.queueName = queueName;
  }

  // FIXME play with this thread - but block incoming thread
  public AudioData play(AudioData data) {

    log.info(String.format("playing %s", data.toString()));

    AudioInputStream din = null;
    try {

      File file = new File(data.file);
      if (file.length() == 0){
        // bail ?
        log.error(String.format("audio file %s 0 byte length", file.getName()));
        return data;
      }
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
        volume = 0; // new file being played or repeat - targetVolume should be
                    // old volume - needs setting to target
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
            // Object lock = myService.getLock(queueName); // getLocks my lock
            synchronized (lock) {
              log.info("pausing");
              // notify all waiting for me
              List<Object> locks = audioFile.getLocksWaitingFor(queueName);
              for (int i = 0; i < locks.size(); ++i) {
                locks.get(i).notifyAll();
              }
              lock.wait();
            }
          }
        }
        // Stop
        line.drain();
        line.stop();
        line.close();
        din.close();

        audioFile.invoke("publishAudioEnd", data);

      } else {
        log.error("line is null !");
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

    return data;
  }

  @Override
  public void run() {
    isRunning = true;

    try {
      AudioData data = null;
      int lastTrackPlayed = currentTrackCount;
      while (isRunning) {

        if (pause) {
          // Object lock = myService.getLock(queueName);
          synchronized (lock) {
            lock.wait();
          }
        }

        if (!repeat || data == null) {
          data = track.take();
          ++currentTrackCount;

        }

        // check to see if we should be waiting for another track to finish
        if (waitForKey != null) {
          Object waitForLock = audioFile.getWaitForLock(waitForKey);
          synchronized (waitForLock) {
            waitForLock.wait();
          }
        }

        play(data);
        if (currentTrackCount != lastTrackPlayed) {
          String key = String.format("%s:%s", queueName, currentTrackCount);
          Object waitForLock = audioFile.getWaitForLock(key);
          if (waitForLock != null) {
            synchronized (waitForLock) {
              waitForLock.notify();
            }
          }
        }

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

  public AudioData add(AudioData data) {
    ++trackCount;
    track.add(data); // COOL - should change state to QUEUED'
    return data;
  }

  public Object getLock() {
    return lock;
  }

  public boolean isRunning() {
    return isRunning;
  }

}
