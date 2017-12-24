package org.myrobotlab.service;

// FIXME ! loose the awt (can't work on Android !)
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

//[BEGIN import_libraries]
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Vertex;
import com.google.common.collect.ImmutableList;

//[END import_libraries]

public class GoogleCloud extends Service {

  private static final long serialVersionUID = 1L;
  final static Logger log = LoggerFactory.getLogger(GoogleCloud.class);
  /**
   * Be sure to specify the name of your application. If the application name is
   * {@code null} or blank, the application will log a warning. Suggested format
   * is "MyCompany-ProductName/1.0".
   */
  private static final String APPLICATION_NAME = "Google-VisionFaceDetectSample/1.0";
  transient Vision vision;
  int maxResults = 32;
  boolean connected = false;
  
  public GoogleCloud(String n) {
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

    ServiceType meta = new ServiceType(GoogleCloud.class.getCanonicalName());
    meta.addDescription("google api client service");
    meta.setAvailable(true);
    // add dependency if necessary
    meta.addDependency("com.google.client", "1.22.0");
    meta.addDependency("com.google.vision", "1.22.0");
    meta.addCategory("cloud", "vision");
    meta.setCloudService(true);
    return meta;
  }

  // [START main]
  /**
   * Annotates an image using the Vision API.
   */

  // [END main]

  // [START get_vision_service]
  /**
   * Connects to the Vision API using Application Default Credentials.
   * @param credJsonFile c
   * @return v
   * @throws IOException e 
   * @throws GeneralSecurityException e 
   */
  public Vision getVisionService(String credJsonFile) throws IOException, GeneralSecurityException {

    /*
     * GoogleCredential credential = new
     * GoogleCredential().setAccessToken(accessToken); Plus plus = new
     * Plus.builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(),
     * credential) .setApplicationName("Google-PlusSample/1.0") .build();
     */

    GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(credJsonFile)).createScoped(VisionScopes.all());

    /*
     * JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
     * 
     * HttpTransport httpTransport =
     * GoogleNetHttpTransport.newTrustedTransport();
     * 
     * InputStream inputStream = IOUtils.toInputStream(serviceAccountJson);
     * 
     * GoogleCredential credential = GoogleCredential.fromStream(inputStream,
     * httpTransport, jsonFactory);
     * 
     * credential =
     * credential.createScoped(Collections.singleton(AndroidPublisherScopes.
     * ANDROIDPUBLISHER));
     * 
     * AndroidPublisher androidPublisher = new AndroidPublisher(httpTransport,
     * jsonFactory, credential);
     */

    // GoogleCredential credential =
    // GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential).setApplicationName(APPLICATION_NAME).build();
  }
  // [END get_vision_service]

  public boolean connect(String credJsonFile) throws IOException, GeneralSecurityException {
    File f = new File(credJsonFile);
    if (f.exists())
    {
    connect(getVisionService(credJsonFile));
    connected=true;
    }
    else
    {
      connected=false;
      error("getVisionService : File credJsonFile not exist");
    }
    return connected;
  }

  public void connect(Vision vision) {
    this.vision = vision;
  }

  // [START detect_face]
  public List<FaceAnnotation> detectFaces(String path) throws IOException {
    return detectFaces(Paths.get(path), maxResults);
  }

  public List<FaceAnnotation> detectFaces(Path path) throws IOException {
    return detectFaces(path, maxResults);
  }

  /**
   * Gets up to {@code maxResults} faces for an image stored at {@code path}.
   * @param path p
   * @param maxResults m 
   * @return list of face annotations
   * @throws IOException e
   */
  public List<FaceAnnotation> detectFaces(Path path, int maxResults) throws IOException {
    byte[] data = Files.readAllBytes(path);

    AnnotateImageRequest request = new AnnotateImageRequest().setImage(new Image().encodeContent(data))
        .setFeatures(ImmutableList.of(new Feature().setType("FACE_DETECTION").setMaxResults(maxResults)));
    Vision.Images.Annotate annotate = vision.images().annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
    // Due to a bug: requests to Vision API containing large images fail
    // when GZipped.
    annotate.setDisableGZipContent(true);

    BatchAnnotateImagesResponse batchResponse = annotate.execute();
    assert batchResponse.getResponses().size() == 1;
    AnnotateImageResponse response = batchResponse.getResponses().get(0);
    if (response.getFaceAnnotations() == null) {
      throw new IOException(response.getError() != null ? response.getError().getMessage() : "Unknown error getting image annotations");
    }
    return response.getFaceAnnotations();  
  }
  // [END detect_face]

  // [START highlight_faces]

  public void writeWithFaces(String inputPath, String outputPath, List<FaceAnnotation> faces) throws IOException {
    writeWithFaces(Paths.get(inputPath), Paths.get(outputPath), faces);
  }

  /**
   * Reads image {@code inputPath} and writes {@code outputPath} with
   * {@code faces} outlined.
   * @param inputPath i
   * @param outputPath o
   * @param faces f
   * @throws IOException e 
   */
  public void writeWithFaces(Path inputPath, Path outputPath, List<FaceAnnotation> faces) throws IOException {
    BufferedImage img = ImageIO.read(inputPath.toFile());
    annotateWithFaces(img, faces);
    ImageIO.write(img, "jpg", outputPath.toFile());
  }

  /**
   * Annotates an image {@code img} with a polygon around each face in
   * {@code faces}.
   * @param img i 
   * @param faces f
   */
  public void annotateWithFaces(BufferedImage img, List<FaceAnnotation> faces) {
    for (FaceAnnotation face : faces) {
      annotateWithFace(img, face);
    }
  }

  /**
   * Annotates an image {@code img} with a polygon defined by {@code face}.
   */
  private void annotateWithFace(BufferedImage img, FaceAnnotation face) {
    Graphics2D gfx = img.createGraphics();
    Polygon poly = new Polygon();
    for (Vertex vertex : face.getFdBoundingPoly().getVertices()) {
      poly.addPoint(vertex.getX(), vertex.getY());
    }
    gfx.setStroke(new BasicStroke(5));
    gfx.setColor(new Color(0x00ff00));
    gfx.draw(poly);
  }
  // [END highlight_faces]

  /**
   * Prints the labels received from the Vision API.
   * @param out o
   * @param imagePath i 
   * @param labels l
   */
  public void printLabels(PrintStream out, Path imagePath, List<EntityAnnotation> labels) {
    out.printf("Labels for image %s:\n", imagePath);
    for (EntityAnnotation label : labels) {
      out.printf("\t%s (score: %.3f)\n", label.getDescription(), label.getScore());
    }
    if (labels.isEmpty()) {
      out.println("\tNo labels found.");
    }
  }

  public List<EntityAnnotation> labelImage(Path path) throws IOException {
    return labelImage(path, maxResults);
  }

  /**
   * Gets up to {@code maxResults} labels for an image stored at {@code path}.
   * @param path p
   * @param maxResults m 
   * @return list of entity annotations
   * @throws IOException e
   */
  public List<EntityAnnotation> labelImage(Path path, int maxResults) throws IOException {
    // [START construct_request]
    byte[] data = Files.readAllBytes(path);

    AnnotateImageRequest request = new AnnotateImageRequest().setImage(new Image().encodeContent(data))
        .setFeatures(ImmutableList.of(new Feature().setType("LABEL_DETECTION").setMaxResults(maxResults)));
    Vision.Images.Annotate annotate = vision.images().annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
    // Due to a bug: requests to Vision API containing large images fail
    // when GZipped.
    // annotate.setDisableGZipContent(true);
    // [END construct_request]

    // [START parse_response]
    BatchAnnotateImagesResponse batchResponse = annotate.execute();
    assert batchResponse.getResponses().size() == 1;
    AnnotateImageResponse response = batchResponse.getResponses().get(0);
    if (response.getLabelAnnotations() == null) {
      throw new IOException(response.getError() != null ? response.getError().getMessage() : "Unknown error getting image annotations");
    }
    return response.getLabelAnnotations();
    // [END parse_response]
  }

  public Map<String, Float> getLabels(String filename) throws IOException {
    LinkedHashMap<String, Float> ret = new LinkedHashMap<String, Float>();
    byte[] data = Files.readAllBytes(Paths.get(filename));

    AnnotateImageRequest request = new AnnotateImageRequest().setImage(new Image().encodeContent(data))
        .setFeatures(ImmutableList.of(new Feature().setType("LABEL_DETECTION").setMaxResults(maxResults)));
    Vision.Images.Annotate annotate = vision.images().annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
    // Due to a bug: requests to Vision API containing large images fail
    // when GZipped.
    // annotate.setDisableGZipContent(true);
    // [END construct_request]

    // [START parse_response]
    BatchAnnotateImagesResponse batchResponse = annotate.execute();
    assert batchResponse.getResponses().size() == 1;
    AnnotateImageResponse response = batchResponse.getResponses().get(0);
    if (response.getLabelAnnotations() == null) {
      throw new IOException(response.getError() != null ? response.getError().getMessage() : "Unknown error getting image annotations");
    }

    List<EntityAnnotation> labels = response.getLabelAnnotations();

    log.info("Labels for image {}:", filename);
    for (EntityAnnotation label : labels) {
      String desc = label.getDescription();
      Float score = label.getScore();
      log.info("\t{} (score: {})", desc, score);
      ret.put(desc, score);
    }

    if (labels.isEmpty()) {
      log.info("\tNo labels found.");
    }

    return ret;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.getInstance().configure();
      // LoggingFactory.getInstance().setLevel(Level.INFO);

      GoogleCloud google = (GoogleCloud) Runtime.start("google", "GoogleCloud");
      // Runtime.start("gui", "SwingGui");

      if (args.length != 2) {
        System.err.println("Usage:");
        System.err.printf("\tjava %s inputImagePath outputImagePath\n", GoogleCloud.class.getCanonicalName());
        System.exit(1);
      }

      Path inputPath = Paths.get(args[0]);
      Path outputPath = Paths.get(args[1]);

      if (!outputPath.toString().toLowerCase().endsWith(".jpg")) {
        System.err.println("outputImagePath must have the file extension 'jpg'.");
        System.exit(1);
      }

      // "API Project-c90c3d12e7d3.json"

      // GoogleCloudService app = new
      // GoogleCloudService(getVisionService());
      google.connect("../API Project-c90c3d12e7d3.json");

      long ts = System.currentTimeMillis();

      List<FaceAnnotation> faces = google.detectFaces(inputPath);
      System.out.printf("Found %d face%s\n", faces.size(), faces.size() == 1 ? "" : "s");
      System.out.printf("Writing to file %s\n", outputPath);
      google.writeWithFaces(inputPath, outputPath, faces);

      google.getLabels("kitchen.jpg");
      google.getLabels("plumbing.jpg");
      google.getLabels("ship.jpg");
      google.getLabels("greenball.jpg");

      log.info("{} total ms", System.currentTimeMillis() - ts);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
