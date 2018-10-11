package org.myrobotlab.service;

import java.io.IOException;

import org.junit.Test;

import marytts.exceptions.SynthesisException;

public class VoiceRssTest {

  @Test
  public void testVoiceRss() throws IOException, SynthesisException, InterruptedException {
    VoiceRss voiceRss = (VoiceRss)Runtime.createAndStart("voiceRss", "VoiceRss");
    voiceRss.speakBlocking("hello world");    
  }
}
