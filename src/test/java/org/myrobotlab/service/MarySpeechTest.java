package org.myrobotlab.service;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import marytts.exceptions.SynthesisException;

// KW_ TODO: this is ignored now, for some reason this locks up on me...
@Ignore
public class MarySpeechTest {

  @Test
  public void testMarySpeech() throws IOException, SynthesisException, InterruptedException {
    MarySpeech speech = (MarySpeech) Runtime.createAndStart("speech", "MarySpeech");
    speech.speakBlocking("hello world");

  }
}
