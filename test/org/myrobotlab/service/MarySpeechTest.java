package org.myrobotlab.service;

import org.junit.Ignore;
import org.junit.Test;

import marytts.exceptions.SynthesisException;

// KW_ TODO: this is ignored now, for some reason this locks up on me...
@Ignore
public class MarySpeechTest {

  @Test
  public void testMarySpeech() throws SynthesisException, InterruptedException {
    MarySpeech speech = (MarySpeech)Runtime.createAndStart("speech", "MarySpeech");
    speech.speakBlocking("hello world");
    
  }
}
