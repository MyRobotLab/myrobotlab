package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

@Ignore
public class AcapelaSpeechTest {

  public final static Logger log = LoggerFactory.getLogger(AcapelaSpeechTest.class);

  static private SpeechSynthesis speech = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    speech = (SpeechSynthesis) Runtime.start("speech", "AcapelaSpeech");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testGetCategories() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetDescription() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStartService() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testStopService() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetPeers() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testMain() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeech() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetByteArrayFromResponse() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testIsSpeaking() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testListAllVoices() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testOnText() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testQueueSetLanguage() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testRequestConfirmation() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSaying() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetBackendType() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetFrontendType() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetGenderFemale() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetGenderMale() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetGoogleProxy() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetGoogleURI() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetLanguage() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeak() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeakInternal() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeakBlockingString() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeakBlockingStringObjectArray() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeakErrors() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeakFreeTTS() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSpeakGoogle() throws Exception {
    log.info("starting testSpeakGoogle");

    // speech.speak("Light scattering is a form of scattering in which light is
    // the form of propagating energy which is scattered. Light scattering can
    // be thought of as the deflection of a ray from a straight path, for
    // example by irregularities in the propagation medium, particles, or in the
    // interface between two media. Deviations from the law of reflection due to
    // irregularities on a surface are also usually considered to be a form of
    // scattering. When these irregularities are considered to be random and
    // dense enough that their individual effects average out, this kind of
    // scattered reflection is commonly referred to as diffuse reflection.");
    // speech.speak("hello");
    speech.speak("I don't use appostrophes, or other punctuation, do you?");
    speech.speak("I'm done with this test");
    speech.speak("I'm done with this test again");
    // Swedish
    speech.setLanguage("sv");
    speech.speak("Testar Svenska. HÃ¤st, Ã¥sna, fÃ¶l, gÃ¥s");
    // French
    speech.setLanguage("fr");
    speech.speak("Teste le franÃ§ais. Joyeux NoÃ«l.");
    // Japanese
    speech.setLanguage("ja");
    speech.speak("ç§�ã�¯æ—¥æœ¬èªžã‚’è©±ã�—ã�¾ã�™");
    // Hindi
    speech.setLanguage("hi");
    speech.speak("à¤®à¥ˆà¤‚ à¤¹à¤¿à¤‚à¤¦à¥€ à¤¬à¥‹à¤²à¤¤à¥‡ à¤¹à¥ˆà¤‚");
    // TODO non-blocking - blocking google freetts
  }

  @Test
  public final void testSpeakNormal() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testSetVolume() {
    // fail("Not yet implemented"); // TODO
  }

  @Test
  public final void testGetVolume() {
    // fail("Not yet implemented"); // TODO
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.DEBUG);

      JUnitCore junit = new JUnitCore();
      Result result = junit.run(AcapelaSpeechTest.class);
      log.info("Result: {}" + result);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
