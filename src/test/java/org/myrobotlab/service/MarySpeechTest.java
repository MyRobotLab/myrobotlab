package org.myrobotlab.service;

import java.io.IOException;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

import marytts.exceptions.SynthesisException;

public class MarySpeechTest extends AbstractTest {

  @Test
  public void testMarySpeech() throws IOException, SynthesisException, InterruptedException {
    log.warn("this is testing method name {}", getName());
    MarySpeech speech = (MarySpeech) Runtime.createAndStart("speech", "MarySpeech");
    speech.speakBlocking("hello world");

  }
}
