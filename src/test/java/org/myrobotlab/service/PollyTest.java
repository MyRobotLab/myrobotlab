package org.myrobotlab.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;

@RunWith(MockitoJUnitRunner.class)
public class PollyTest {

  public final static Logger log = LoggerFactory.getLogger(PollyTest.class);

  private Polly polly;

  @Mock
  private AmazonPolly pollyClient;

  @Before
  public void setUp() throws Exception {
    // generates an error starting up because no valid keys
    polly = (Polly) Runtime.start("testPolly", "Polly");
    // polly.apply(new PollyConfig()); - want new config ?
    // polly.setSpeechSynthesisConfig(new SpeechSynthesisConfig("test"));
  }

  @Test
  public void testLoadVoices() throws IOException {
    // Arrange
    DescribeVoicesResult describeVoicesResult = mock(DescribeVoicesResult.class);
    Voice voice1 = mock(Voice.class);
    Voice voice2 = mock(Voice.class);
    List<Voice> voices = new ArrayList<>();
    voices.add(voice1);
    voices.add(voice2);

    when(pollyClient.describeVoices(any(DescribeVoicesRequest.class))).thenReturn(describeVoicesResult);
    when(describeVoicesResult.getVoices()).thenReturn(voices);
    when(voice1.getName()).thenReturn("voice1");
    when(voice2.getName()).thenReturn("voice2");
    when(voice1.getGender()).thenReturn("Female");
    when(voice2.getGender()).thenReturn("Male");
    when(voice1.getLanguageCode()).thenReturn("en-US");
    when(voice2.getLanguageCode()).thenReturn("en-US");

    polly.setPollyClient(pollyClient);

    // Act
    polly.loadVoices();

    // Assert
    List<AbstractSpeechSynthesis.Voice> result = polly.getVoices();
    assert (result.size() == 2);
    assert (result.get(0).getName().equals("voice1"));
    assert (result.get(0).getGender().equals("female"));
    assert (result.get(0).getLanguage().equals("en"));
    assert (result.get(1).getName().equals("voice2"));
    assert (result.get(1).getGender().equals("male"));
    assert (result.get(1).getLanguage().equals("en"));
  }

  @Test
  public void testSpeak() throws Exception {
    // Arrange
    DescribeVoicesResult describeVoicesResult = mock(DescribeVoicesResult.class);
    Voice voice1 = mock(Voice.class);
    Voice voice2 = mock(Voice.class);
    List<Voice> voices = new ArrayList<>();
    voices.add(voice1);
    voices.add(voice2);

    when(pollyClient.describeVoices(any(DescribeVoicesRequest.class))).thenReturn(describeVoicesResult);
    when(describeVoicesResult.getVoices()).thenReturn(voices);
    when(voice1.getName()).thenReturn("voice1");
    when(voice2.getName()).thenReturn("voice2");
    when(voice1.getGender()).thenReturn("Female");
    when(voice2.getGender()).thenReturn("Male");
    when(voice1.getLanguageCode()).thenReturn("en-US");
    when(voice2.getLanguageCode()).thenReturn("en-US");

    polly.setPollyClient(pollyClient);

    SynthesizeSpeechResult synthesizeSpeechResult = mock(SynthesizeSpeechResult.class);
    InputStream audioStream = new ByteArrayInputStream(new byte[0]);

    when(pollyClient.synthesizeSpeech(any(SynthesizeSpeechRequest.class))).thenReturn(synthesizeSpeechResult);
    when(synthesizeSpeechResult.getAudioStream()).thenReturn(audioStream);

    polly.loadVoices();

    polly.setPollyClient(pollyClient);
    polly.setVoice("voice1");
    polly.setVolume(50);
    List<AudioData> audioData = polly.speak("hello world2");

    // Assert
    assert (audioData.size() > 0);
    assert (polly.getVolume() == 50);
    assert (polly.getVoice().getName().equals("voice1"));

    log.info("here");
    // polly.setPitch(50);
    // polly not finished
  }
}
