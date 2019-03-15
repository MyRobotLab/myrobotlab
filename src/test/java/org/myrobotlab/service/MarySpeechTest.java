package org.myrobotlab.service;

import java.io.IOException;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

import marytts.exceptions.SynthesisException;

public class MarySpeechTest extends AbstractTest {

  @Test
  public void testMarySpeech() throws IOException, SynthesisException, InterruptedException {
    if (printMethods)System.out.println(String.format("Running %s.%s", getSimpleName(), getName()));
    MarySpeech speech = (MarySpeech) Runtime.createAndStart("speech", "MarySpeech");
    speech.speakBlocking("hello world");
  }

}
