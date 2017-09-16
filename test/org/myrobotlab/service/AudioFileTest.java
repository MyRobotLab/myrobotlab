package org.myrobotlab.service;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class AudioFileTest {

  @Test
  public final void testPlay() {

    AudioFile af1 = (AudioFile) Runtime.createAndStart("af1", "AudioFile");
    String filename = "Wreck.mp3";
    af1.playFile(filename, false);
    assertTrue(true);
    System.out.println("Started playing!");
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("set volume to zero");
    af1.setVolume(0);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("set volume to 1");
    af1.setVolume(1.0);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
