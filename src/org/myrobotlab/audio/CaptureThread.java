package org.myrobotlab.audio;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.AudioFile;
import org.slf4j.Logger;

class CaptureThread extends Thread {
  // An arbitrary-size temporary holding
  // buffer
  static Logger log = LoggerFactory.getLogger(AudioFile.class);

  byte buffer[] = new byte[10000];
  OutputStream out = null;

  TargetDataLine audioLine;
  SourceDataLine sourceDataLine;

  boolean done = false;

  public CaptureThread(TargetDataLine audioLine) {
    this(audioLine, null);
  }

  public CaptureThread(TargetDataLine audioLine, OutputStream out) {
    this.audioLine = audioLine;
    if (out == null) {
      out = new ByteArrayOutputStream();
      ;
    }
    this.out = out;
  }

  @Override
  public void run() {
    out = new ByteArrayOutputStream();
    try {// Loop until stopCapture is set
         // by another thread that
         // services the Stop button.
      while (!done) {
        // Read data from the internal
        // buffer of the data line.
        int cnt = audioLine.read(buffer, 0, buffer.length);
        if (cnt > 0) {
          // Save data in output stream
          // object.
          out.write(buffer, 0, cnt);
        } // end if
      } // end while
      audioLine.close();
      out.close();
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (Exception e) {
        }
      }
    }
  }// end run

  public void setBufferLength(int size) {
    buffer = new byte[size];
  }

  public void stopCapture() {
    this.interrupt();
  }

  public OutputStream getOuputStream() {
    return out;
  }
}