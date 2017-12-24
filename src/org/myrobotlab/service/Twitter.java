package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * 
 * Twitter - a service that allows you to send a tweet from MRL assuming you
 * have a consumerKey and accessToken from a twitter account.
 *
 */
public class Twitter extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Twitter.class.getCanonicalName());

  public String consumerKey;
  public String consumerSecret;
  public String accessToken;
  public String accessTokenSecret;

  twitter4j.Twitter twitter = null;

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {
      Twitter twitter = new Twitter("twitter");

      twitter.startService();

      Runtime.createAndStart("gui", "SwingGui");

      twitter.setSecurity("xxx", "xxx", "xxx", "xxx");
      twitter.configure();
      twitter.tweet("Ciao from MyRobotLab");

      // twitter.uploadPic("C:/Users/ALESSANDRO/Desktop/myrobotlab/opencv.jpg"
      // , "here is the pic");

      /*
       * OpenCV opencv = new OpenCV("opencv"); opencv.startService();
       * opencv.capture(); Service.sleep(4000);// wait for an image
       * SerializableImage img = opencv.getDisplay(); twitter.uploadImage(img,
       * "ME TOO!");
       */
      // twitter.subscribe("publishDisplay", opencv.getName(),
      // "uploadImage",
      // SerializableImage.class);

      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /*
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   */

  public Twitter(String n) {
    super(n);
  }

  public void configure() {
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret);
    TwitterFactory tf = new TwitterFactory(cb.build());
    twitter = tf.getInstance();
  }

  @Override
  public void releaseService() {
    super.releaseService();
  }

  public void setSecurity(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {

    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
    this.accessToken = accessToken;
    this.accessTokenSecret = accessTokenSecret;

    configure();
  }

  @Override
  public void stopService() {
    super.stopService();
  }

  public void tweet(String msg) {
    try {
      Status status = twitter.updateStatus(msg);
      // TODO: invoke or publish this maybe?
      log.info("Tweet Status Response: {}", status);
    } catch (TwitterException e) {
      error(e.getMessage());
      Logging.logError(e);
    }
  }

  public void uploadImage(final SerializableImage image, final String message) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          StatusUpdate status = new StatusUpdate(message);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ImageIO.write(image.getImage(), "png", baos);
          baos.flush();
          byte[] buffer = baos.toByteArray();
          status.media("image", new ByteArrayInputStream(buffer));
          twitter.updateStatus(status);
        } catch (Exception e) {
          Logging.logError(e);
        }
      }
    }).start();
  }

  public void uploadImageFile(final String filePath, final String message) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          File file = new File(filePath);
          StatusUpdate status = new StatusUpdate(message);
          status.setMedia(file);
          twitter.updateStatus(status);
        } catch (TwitterException e) {
          Logging.logError(e);
        }
      }
    }).start();
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

    ServiceType meta = new ServiceType(Twitter.class.getCanonicalName());
    meta.addDescription("Service which can relay tweets");
    meta.addCategory("cloud", "connectivity");
    meta.addDependency("org.twitter4j.twitter", "3.0.5");
    meta.setCloudService(true);
    return meta;
  }

}
