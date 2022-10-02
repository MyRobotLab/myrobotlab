package org.myrobotlab.service;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.test.AbstractTest;

public class AudioFileTest extends AbstractTest {

  @Test
  public void testService() throws Exception {
    AudioFile af1 = (AudioFile) Runtime.start("af1", "AudioFile");    
    Python python = (Python) Runtime.start("python", "Python");

    af1.setVolume(0);
    Assert.assertEquals(0.0, af1.getVolume(), 0.0);
    af1.setVolume(1.0);
    Assert.assertEquals(1.0, af1.getVolume(), 0.0);

    String filename = Service.getResourceDir(AudioFile.class, "tick.mp3");
    // unit tests will set this to mute ... (build servers shouldn't start
    // playing sounds when a build runs.)
    af1.setMute(true);

    AudioData d = af1.playBlocking(filename);
    // what can i assert?!
    Assert.assertNotNull(d);

    af1.pause();
    af1.resume();
    af1.stop();

    d = af1.play(filename);
    Assert.assertNotNull(d);

    // Silence!!
    af1.silence();
    
    af1.releaseService();

  }

}
