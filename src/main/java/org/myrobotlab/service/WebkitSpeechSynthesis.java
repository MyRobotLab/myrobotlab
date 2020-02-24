package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

import com.amazonaws.services.polly.model.DescribeVoicesRequest;

/**
 * Amazon's cloud speech service
 * 
 * Free Tier The Amazon Polly free tier includes 5 million characters per month,
 * for the first 12 months, starting from the first request for speech.
 * 
 * Polly Pricing Pay-as-you-go $4.00 per 1 million characters (when outside the
 * free tier).
 *
 * @author GroG
 *
 */
public class WebkitSpeechSynthesis extends AbstractSpeechSynthesis {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebkitSpeechSynthesis.class);

  public WebkitSpeechSynthesis(String n, String id) {
    super(n, id);
  }

  /**
   * loadVoices - must be loaded by SpeechSynthesis class - contract of
   * AbstractSpeechSynthesis
   */
  protected void loadVoices() {

    // TODO get voices from javascript ????

    // Create describe voices request.
    DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
  }

  /**
   */
  @Override
  public AudioData generateAudioData(AudioData audioData, String toSpeak) throws IOException {

    return null;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = AbstractSpeechSynthesis.getMetaData(WebkitSpeechSynthesis.class.getCanonicalName());

    meta.addDescription("used as a general template");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addCategory("speech", "sound");
    return meta;
  }

}
