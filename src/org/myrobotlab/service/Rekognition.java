package org.myrobotlab.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
//import java.nio.ByteBuffer;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
//import com.amazonaws.services.appstream.model.Image;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;

public class Rekognition extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Rekognition.class);

  public Rekognition(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(Rekognition.class.getCanonicalName());
    meta.addDescription("Amazon visual recognition cloud service");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.setCloudService(true);
    meta.addCategory("general");
    return meta;
  }
  
  public static void main(String[] args) throws Exception {
    String photo="/path/inputimage.jpg";

    /* WTF is this way now ?
      AWSCredentials credentials;
      try {
          credentials = new ProfileCredentialsProvider("AdminUser").getCredentials();
      } catch (Exception e) {
          throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                  + "Please make sure that your credentials file is at the correct "
                  + "location (/Usersuserid.aws/credentials), and is in a valid format.", e);
      }
      */
    
    AWSCredentials credentials = new BasicAWSCredentials("xxx", "xxxxxxxx");
    
      ByteBuffer imageBytes;
      try (InputStream inputStream = new FileInputStream(new File(photo))) {
          imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
      }


      AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder
            .standard()
            .withRegion(Regions.US_WEST_2)
          .withCredentials(new AWSStaticCredentialsProvider(credentials))
          .build();

      DetectLabelsRequest request = new DetectLabelsRequest()
              .withImage(new Image()
                      .withBytes(imageBytes))
              .withMaxLabels(10)
              .withMinConfidence(77F);

      try {

          DetectLabelsResult result = rekognitionClient.detectLabels(request);
          List <Label> labels = result.getLabels();

          System.out.println("Detected labels for " + photo);
          for (Label label: labels) {
             System.out.println(label.getName() + ": " + label.getConfidence().toString());
          }

      } catch (AmazonRekognitionException e) {
          e.printStackTrace();
      }

  }
/*
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
    
  }
  */

}
