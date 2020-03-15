package org.myrobotlab.service;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.AudioData;

public class MarySpeechTest extends AbstractServiceTest {

  @Override
  public Service createService() {
    MarySpeech speech = (MarySpeech) Runtime.createAndStart("speech", "MarySpeech");
    // disable the actual mp3 playback only.  (mary should still genereate a valid wav of the utterance.
    speech.getAudioFile().setMute(true);
    return speech;
  }

  @Override
  public void testService() throws Exception {
    // TODO Auto-generated method stub
    MarySpeech speech = (MarySpeech)service;
    List<AudioData> result = speech.speakBlocking("hello world");
    log.info("Speaking result : {}", result);
    File f = new File(result.get(0).getFileName());
    Assert.assertTrue("Cached file doesn't exist. {}" , f.exists());
    Assert.assertTrue("Cached file was zero length. {}", f.length() > 0);
    
  }
}
