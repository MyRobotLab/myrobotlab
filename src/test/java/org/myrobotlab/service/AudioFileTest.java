package org.myrobotlab.service;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Assert;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.data.AudioData;


public class AudioFileTest extends AbstractServiceTest {

  @Override
  public Service createService() {
    AudioFile af1 = (AudioFile) Runtime.createAndStart("af1", "AudioFile");
    return af1;
  }

  @Override
  public void testService() throws Exception {
    // TODO Auto-generated method stub
    AudioFile af1 = (AudioFile)service;
    
    af1.setVolume(0);
    Assert.assertEquals(0.0, af1.getVolume(), 0.0);
    af1.setVolume(1.0);
    Assert.assertEquals(1.0, af1.getVolume(), 0.0);
    
    String filename = Util.getResourceDir() + File.separator + "Clock" + File.separator + "tick.mp3";
    // unit tests will set this to mute ... (build servers shouldn't start playing sounds when a build runs.) 
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
    
    
    
    
  }
}
