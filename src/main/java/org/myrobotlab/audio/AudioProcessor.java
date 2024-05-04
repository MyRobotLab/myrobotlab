package org.myrobotlab.audio;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.config.AudioFileConfig;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

// FIXME - make runnable
public class AudioProcessor extends Thread {

  static transient Logger log = LoggerFactory.getLogger(AudioProcessor.class);

  // REFERENCES -
  // http://www.javalobby.org/java/forums/t18465.html
  // http://sourcecodebrowser.com/libjlayer-java/1.0/classjavazoom_1_1jl_1_1player_1_1_audio_device_base__coll__graph.png
  // - does JavaZoom spi decoder just need to be in the classpath ? - because
  // there is not any direct reference to it
  // it seems to make sense - some how the file gets decoded enough - so that
  // a audio decoder can be slected from some
  // internal registry ... i think

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  transient private final ScheduledExecutorService delayScheduler = Executors.newScheduledThreadPool(1);

  protected int currentTrackCount = 0;

  protected int samplesAdded = 0;

  /**
   * unless explicitly set - uses master volue of AudioFile service
   */
  protected Double volume = null;

  protected float balance = 0.0f;

  protected float targetBalance = balance;

  protected AudioFile audioFile = null;

  protected boolean isPlaying = false;

  protected boolean isRunning = false;

  protected String track;

  protected transient BlockingQueue<AudioData> queue = new LinkedBlockingQueue<AudioData>();

  protected AudioData currentAudioData = null;

  protected int repeatCount;

  /**
   * loop counter
   */
  private int cnt = 0;

  public AudioProcessor(AudioFile audioFile, String track) {
    super(String.format("%s:track", track));
    this.audioFile = audioFile;
    this.track = track;
  }

  /**
   * Pause the current playing file - if it is paused - it is "still considered"
   * to be playing so isPlaying needs to remain true (otherwise the file/audio
   * processor will completely stop)
   * 
   * @param b
   *          - to pause or not
   * @return
   */
  public AudioData pause(boolean b) {
    // isPlaying = b; <- DO NOT DO THIS !
    // someone put this bug in - when a song is 'paused' its still playing
    // ie - this needs to remain true otherwise it will not resume when
    // requested !!
    if (currentAudioData != null) {
      if (b) {
        currentAudioData.waitForLock = new Object();
      } else {
        if (currentAudioData.waitForLock != null) {
          synchronized (currentAudioData.waitForLock) {
            currentAudioData.waitForLock.notifyAll();
            currentAudioData.waitForLock = null; // removing reference
            isPlaying = true;
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
        AudioFileConfig config = (AudioFileConfig) audioFile.getConfig();

        while (isPlaying && (nBytesRead = din.read(buffer, 0, buffer.length)) != -1) {
          ++cnt;
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

          // determining volume through precedence
          // first master volume
          Double volume = audioFile.getVolume();
          // unless track is explicitly set
          if (this.volume != null) {
            volume = this.volume;
          }
          // unless file itself is explicitly set
          if (data.volume != null) {
            volume = data.volume;
          }

          if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {

            FloatControl ctrl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            // float scaled = (float) (Math.log(data.volume) / Math.log(10.0)
            // * 20.0);

            if (MathUtils.round(ctrl.getValue(), 3) != MathUtils.round((float) (ctrl.getMinimum() + ((ctrl.getMaximum() - ctrl.getMinimum()) * volume)), 3)) {
              if (volume <= 1.0f && volume >= 0) {

                ctrl.setValue((float) (ctrl.getMinimum() + ((ctrl.getMaximum() - ctrl.getMinimum()) * volume)));
                log.debug("Audioprocessor set volume to : " + ctrl.getValue());
              } else {
                log.error("Requested volume value " + volume.toString() + " not allowed");
                volume = 1.0;
              }
            }
            // volume.setValue(scaled);
          }

          if (balance != targetBalance) {
            try {
              FloatControl control = (FloatControl) line.getControl(FloatControl.Type.BALANCE);
              control.setValue(balance);
              balance = targetBalance;
            } catch (Exception e) {
              log.error("floatcontrol threw", e);
            }
          }

          if (!audioFile.isMute()) {
            line.write(buffer, 0, nBytesRead);
          } else {
            // rough estimate of play time - the above blocks the thread
            // if we are not computing and blocking the loops is much too fast
            // by definition "mute" should still read/process the file, just not
            // make any sound
            sleep(100);
          }
          // Compute the peak value and publish it.
          if (cnt % config.peakSampleInterval == 0) {
            float peak = 0f;
            int b = buffer.length;
            // convert bytes to samples here
            for (int i = 0; i < b;) {
              int sample = 0;
              sample |= buffer[i++] & 0xFF; // (reverse these two lines
              sample |= buffer[i++] << 8; // if the format is big endian)
              float abs = Math.abs(sample / 32768f);
              if (abs > peak) {
                peak = abs;
              }
            }

            final double value = peak * (double) audioFile.getPeakMultiplier();

            // skew publish forwards in time
            if (audioFile.getConfig().peakDelayMs == null) {
              audioFile.invoke("publishPeak", value);
            } else {
              delayScheduler.schedule(() -> audioFile.invoke("publishPeak", value), audioFile.getConfig().peakDelayMs, TimeUnit.MILLISECONDS);
            }

            // reset to 0 after millis
            if (audioFile.getConfig().publishPeakResetDelayMs != null) {
              delayScheduler.schedule(() -> audioFile.invoke("publishPeak", 0), audioFile.getConfig().peakDelayMs + audioFile.getConfig().publishPeakResetDelayMs,
                  TimeUnit.MILLISECONDS);
            }
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

        if (audioFile.getConfig().peakDelayMs == null) {
          audioFile.invoke("publishPeak", 0);
        } else {
          delayScheduler.schedule(() -> audioFile.invoke("publishPeak", 0), audioFile.getConfig().peakDelayMs, TimeUnit.MILLISECONDS);
        }

        audioFile.invoke("publishPeak", 0);
        audioFile.invoke("publishAudioEnd", data);

        synchronized (data) {
          log.debug("notifying others");
          data.notifyAll();
        }

      } else {
        log.error("line is null !");
      }
    } catch (Exception e) {
      if (data != null) {
        synchronized (data) {
          log.debug("notifying others");
          data.notifyAll();
        }
      }
      if (audioFile != null) {
        audioFile.error(e);
        audioFile.warn("%s - %s output audio line was not found - is audio enabled?", e.getClass().getSimpleName(), e.getMessage());
        if (data != null) {
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

        play(data);
        ++repeatCount;

        data.stopTs = System.currentTimeMillis();

      }
    } catch (Exception e) {
      isRunning = false;
    }
    // default waits on queued audio requests
    log.info("audio processor {} exiting", getName());
    delayScheduler.shutdown();
  }

  public void setVolume(Double volume) {
    this.volume = volume;
  }

  public Double getVolume() {
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
