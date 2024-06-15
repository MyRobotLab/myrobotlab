package org.myrobotlab.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
//import java.nio.ByteBuffer;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.RekognitionConfig;
import org.slf4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
//import com.amazonaws.services.appstream.model.Image;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;

public class Rekognition extends Service<RekognitionConfig>
{

  private static final long serialVersionUID = 1L;

  transient private AWSCredentials credentials;
  transient private AmazonRekognition rekognitionClient;

  Regions region = Regions.US_WEST_2;

  ByteBuffer lastImage;
  List<Label> lastLabels;

  int maxLabels = 10;
  float minConfidence = 77F;

  public final static Logger log = LoggerFactory.getLogger(Rekognition.class);

  public Rekognition(String n, String id) {
    super(n, id);
  }

  public void setMinConfidence(float confidence) {
    this.minConfidence = confidence;
  }

  public float getMinConfidence() {
    return minConfidence;
  }

  /**
   * This sets the aws credentials, this only needs to be done once! It encrypts
   * and saves the credentials in the .myrobotlab/store file. Once this is run
   * once
   * 
   * @param accessKey
   *                  aws access key
   * @param secretKey
   *                  aws secret key
   */
  public void setCredentials(String accessKey, String secretKey) {
    Security security = Runtime.getSecurity();
    security.setKey(String.format("%s.aws.accessKey", getName()), accessKey);
    security.setKey(String.format("%s.aws.secretKey", getName()), secretKey);
  }

  /**
   * loads pre-saved encrypted credentials into the aws credential provider
   */
  public void loadCredentials() {
    Security security = Runtime.getSecurity();
    String accessKey = security.getKey(String.format("%s.aws.accessKey", getName()));
    String secretKey = security.getKey(String.format("%s.aws.secretKey", getName()));

    credentials = new BasicAWSCredentials(accessKey, secretKey);
  }

  /**
   * set the region - not sure which ones are supported
   * 
   * @param region
   *               r
   * 
   */
  public void setRegion(Regions region) {
    this.region = region;
  }

  /**
   * @return an initialized client or throws with an error
   * 
   * 
   */
  public AmazonRekognition getClient() {
    if (rekognitionClient == null) {
      rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(region)
          .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
    }
    return rekognitionClient;
  }

  /**
   * returns label from file
   * 
   * 
   * @param path
   *             - the path
   * @return - labels
   * @throws FileNotFoundException
   *                               boom
   * @throws IOException
   *                               boom
   * @throws URISyntaxException
   *                               boom
   */
  public List<Label> getLabels(String path) throws FileNotFoundException, IOException, URISyntaxException {
    if (path == null) {
      return getLabels();
    }

    InputStream inputStream = null;
    if (path.contains("://")) {
      inputStream = new URL(path).openStream();
    } else {
      inputStream = new FileInputStream(path);
    }
    return getLabels(inputStream);
  }

  public List<Label> getLabels() {
    return getLabels(lastImage);
  }

  /**
   * get labels
   * 
   * @param inputStream
   *                    - the stream of data
   * @return - labels found
   * @throws FileNotFoundException
   *                               boom
   * @throws IOException
   *                               boom
   */
  public List<Label> getLabels(InputStream inputStream) throws FileNotFoundException, IOException {
    ByteBuffer imageBytes;
    imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
    return getLabels(imageBytes);
  }

  /**
   * Hopefully, Label is serializable, otherwise it needs to return a list of
   * POJOs.
   * 
   * @param imageBytes
   *                   image data
   * @return list of labels extracted
   * 
   */
  public List<Label> getLabels(ByteBuffer imageBytes) {
    AmazonRekognition client = getClient();
    DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withBytes(imageBytes))
        .withMaxLabels(maxLabels).withMinConfidence(minConfidence);
    DetectLabelsResult result = client.detectLabels(request);
    List<Label> labels = result.getLabels();
    lastImage = imageBytes;
    lastLabels = labels;
    return labels;
  }

  // FIXME make BufferedImage translations...
  public List<FaceDetail> getFaces(ByteBuffer imageBytes, Integer width, Integer height) {

    DetectFacesRequest request = new DetectFacesRequest().withImage(new Image().withBytes((imageBytes)))
        .withAttributes(Attribute.ALL);

    DetectFacesResult result = getClient().detectFaces(request);
    System.out.println("Orientation: " + result.getOrientationCorrection() + "\n");
    List<FaceDetail> faceDetails = result.getFaceDetails();

    for (FaceDetail face : faceDetails) {
      System.out.println("Face:");
      ShowBoundingBoxPositions(height, width, face.getBoundingBox(), result.getOrientationCorrection());
      AgeRange ageRange = face.getAgeRange();
      System.out.println("The detected face is estimated to be between " + ageRange.getLow().toString() + " and "
          + ageRange.getHigh().toString() + " years old.");
      System.out.println();
    }

    return faceDetails;

  }

  public static void ShowBoundingBoxPositions(int imageHeight, int imageWidth, BoundingBox box, String rotation) {

    float left = 0;
    float top = 0;

    if (rotation == null) {
      System.out.println("No estimated estimated orientation. Check Exif data.");
      return;
    }
    // Calculate face position based on image orientation.
    switch (rotation) {
      case "ROTATE_0":
        left = imageWidth * box.getLeft();
        top = imageHeight * box.getTop();
        break;
      case "ROTATE_90":
        left = imageHeight * (1 - (box.getTop() + box.getHeight()));
        top = imageWidth * box.getLeft();
        break;
      case "ROTATE_180":
        left = imageWidth - (imageWidth * (box.getLeft() + box.getWidth()));
        top = imageHeight * (1 - (box.getTop() + box.getHeight()));
        break;
      case "ROTATE_270":
        left = imageHeight * box.getTop();
        top = imageWidth * (1 - box.getLeft() - box.getWidth());
        break;
      default:
        System.out.println("No estimated orientation information. Check Exif data.");
        return;
    }

    // Display face location information.
    System.out.println("Left: " + String.valueOf((int) left));
    System.out.println("Top: " + String.valueOf((int) top));
    System.out.println("Face Width: " + String.valueOf((int) (imageWidth * box.getWidth())));
    System.out.println("Face Height: " + String.valueOf((int) (imageHeight * box.getHeight())));

  }

  public static void main(String[] args) throws Exception {

    Rekognition recog = (Rekognition) Runtime.start("recog", "Rekognition");

    /*
     * OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
     * opencv.capture();
     * 
     * sleep(1000); String photo = opencv.recordFrame();
     * 
     * 
     * System.out.println("Detected labels for " + photo);
     */

    // set your credentials once - then comment out this line and remove the
    // sensitive info
    // the credentials will be saved to .myrobotlab/store
    // recog.setCredentials("XXXXXXXXXXXXXXXX",
    // "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    recog.loadCredentials();
    List<Label> labels = null;
    // labels = recog.getLabels("opencv.input.48.jpg");
    labels = recog.getLabels("http://animals.sandiegozoo.org/sites/default/files/2016-08/hero_zebra_animals.jpg");
    for (Label label : labels) {

      System.out.println(label.getName() + ": " + label.getConfidence().toString());
    }

  }

}
