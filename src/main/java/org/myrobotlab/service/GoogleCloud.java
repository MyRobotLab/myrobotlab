package org.myrobotlab.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.CropHint;
import com.google.cloud.vision.v1.CropHintsAnnotation;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.LocationInfo;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.WebDetection.WebEntity;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.cloud.vision.v1.WebDetection.WebPage;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;

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
    // meta.addDependency("com.google.api-client", "google-api-client",
    // "1.23.0");
    meta.addDependency("com.google.cloud", "google-cloud-vision", "1.14.0");
    meta.addCategory("cloud", "vision");
    meta.setCloudService(true);
    return meta;
  }

  /**
   * Detects entities,sentiment and syntax in a document using the Vision API.
   *
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void main(String[] args) throws Exception, IOException {
    argsHelper(args, System.out);
  }

  /**
   * Helper that handles the input passed to the program.
   *
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void argsHelper(String[] args, PrintStream out) throws Exception, IOException {
    if (args.length < 1) {
      out.println("Usage:");
      out.printf("\tmvn exec:java -DDetect -Dexec.args=\"<command> <path-to-image>\"\n" + "Commands:\n" + "\tfaces | labels | landmarks | logos | text | safe-search | properties"
          + "| web | crop \n" + "Path:\n\tA file path (ex: ./resources/wakeupcat.jpg) or a URI for a Cloud Storage " + "resource (gs://...)\n");
      return;
    }
    String command = args[0];
    String path = args.length > 1 ? args[1] : "";

    GoogleCloud app = new GoogleCloud("googleVision");
    if (command.equals("faces")) {
      if (path.startsWith("gs://")) {
        detectFacesGcs(path, out);
      } else {
        detectFaces(path, out);
      }
    } else if (command.equals("labels")) {
      if (path.startsWith("gs://")) {
        detectLabelsGcs(path, out);
      } else {
        detectLabels(path, out);
      }
    } else if (command.equals("landmarks")) {
      if (path.startsWith("http")) {
        detectLandmarksUrl(path, out);
      } else if (path.startsWith("gs://")) {
        detectLandmarksGcs(path, out);
      } else {
        detectLandmarks(path, out);
      }
    } else if (command.equals("logos")) {
      if (path.startsWith("gs://")) {
        detectLogosGcs(path, out);
      } else {
        detectLogos(path, out);
      }
    } else if (command.equals("text")) {
      if (path.startsWith("gs://")) {
        detectTextGcs(path, out);
      } else {
        detectText(path, out);
      }
    } else if (command.equals("properties")) {
      if (path.startsWith("gs://")) {
        detectPropertiesGcs(path, out);
      } else {
        detectProperties(path, out);
      }
    } else if (command.equals("safe-search")) {
      if (path.startsWith("gs://")) {
        detectSafeSearchGcs(path, out);
      } else {
        detectSafeSearch(path, out);
      }
    } else if (command.equals("web")) {
      if (path.startsWith("gs://")) {
        detectWebDetectionsGcs(path, out);
      } else {
        detectWebDetections(path, out);
      }
    } else if (command.equals("crop")) {
      if (path.startsWith("gs://")) {
        detectCropHintsGcs(path, out);
      } else {
        detectCropHints(path, out);
      }
    } else if (command.equals("fulltext")) {
      if (path.startsWith("gs://")) {
        detectDocumentTextGcs(path, out);
      } else {
        detectDocumentText(path, out);
      }
    }
  }

  /**
   * Detects faces in the specified local image.
   *
   * @param filePath
   *          The path to the file to perform face detection on.
   * @param out
   *          A {@link PrintStream} to write detected features to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectFaces(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.FACE_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
          out.printf("anger: %s\njoy: %s\nsurprise: %s\nposition: %s", annotation.getAngerLikelihood(), annotation.getJoyLikelihood(), annotation.getSurpriseLikelihood(),
              annotation.getBoundingPoly());
        }
      }
    }
  }

  /**
   * Detects faces in the specified remote image on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to perform
   *          face detection on.
   * @param out
   *          A {@link PrintStream} to write detected features to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectFacesGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.FACE_DETECTION).build();

    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
          out.printf("anger: %s\njoy: %s\nsurprise: %s\nposition: %s", annotation.getAngerLikelihood(), annotation.getJoyLikelihood(), annotation.getSurpriseLikelihood(),
              annotation.getBoundingPoly());
        }
      }
    }
  }

  /**
   * Detects labels in the specified local image.
   *
   * @param filePath
   *          The path to the file to perform label detection on.
   * @param out
   *          A {@link PrintStream} to write detected labels to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLabels(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
          annotation.getAllFields().forEach((k, v) -> out.printf("%s : %s\n", k, v.toString()));
        }
      }
    }
  }

  /**
   * Detects labels in the specified remote image on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to perform
   *          label detection on.
   * @param out
   *          A {@link PrintStream} to write detected features to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLabelsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
          annotation.getAllFields().forEach((k, v) -> out.printf("%s : %s\n", k, v.toString()));
        }
      }
    }
  }

  /**
   * Detects landmarks in the specified local image.
   *
   * @param filePath
   *          The path to the file to perform landmark detection on.
   * @param out
   *          A {@link PrintStream} to write detected landmarks to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLandmarks(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();
    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
          LocationInfo info = annotation.getLocationsList().listIterator().next();
          out.printf("Landmark: %s\n %s\n", annotation.getDescription(), info.getLatLng());
        }
      }
    }
  }

  /**
   * Detects landmarks in the specified URI.
   *
   * @param uri
   *          The path to the file to perform landmark detection on.
   * @param out
   *          A {@link PrintStream} to write detected landmarks to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLandmarksUrl(String uri, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setImageUri(uri).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
          LocationInfo info = annotation.getLocationsList().listIterator().next();
          out.printf("Landmark: %s\n %s\n", annotation.getDescription(), info.getLatLng());
        }
      }
    }
  }

  /**
   * Detects landmarks in the specified remote image on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to perform
   *          landmark detection on.
   * @param out
   *          A {@link PrintStream} to write detected landmarks to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLandmarksGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
          LocationInfo info = annotation.getLocationsList().listIterator().next();
          out.printf("Landmark: %s\n %s\n", annotation.getDescription(), info.getLatLng());
        }
      }
    }
  }

  /**
   * Detects logos in the specified local image.
   *
   * @param filePath
   *          The path to the local file to perform logo detection on.
   * @param out
   *          A {@link PrintStream} to write detected logos to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLogos(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
          out.println(annotation.getDescription());
        }
      }
    }
  }

  /**
   * Detects logos in the specified remote image on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to perform
   *          logo detection on.
   * @param out
   *          A {@link PrintStream} to write detected logos to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectLogosGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
          out.println(annotation.getDescription());
        }
      }
    }
  }

  /**
   * Detects text in the specified image.
   *
   * @param filePath
   *          The path to the file to detect text in.
   * @param out
   *          A {@link PrintStream} to write the detected text to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectText(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
          out.printf("Text: %s\n", annotation.getDescription());
          out.printf("Position : %s\n", annotation.getBoundingPoly());
        }
      }
    }
  }

  /**
   * Detects text in the specified remote image on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to detect text
   *          in.
   * @param out
   *          A {@link PrintStream} to write the detected text to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectTextGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
          out.printf("Text: %s\n", annotation.getDescription());
          out.printf("Position : %s\n", annotation.getBoundingPoly());
        }
      }
    }
  }

  /**
   * Detects image properties such as color frequency from the specified local
   * image.
   *
   * @param filePath
   *          The path to the file to detect properties.
   * @param out
   *          A {@link PrintStream} to write
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectProperties(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.IMAGE_PROPERTIES).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
        for (ColorInfo color : colors.getColorsList()) {
          out.printf("fraction: %f\nr: %f, g: %f, b: %f\n", color.getPixelFraction(), color.getColor().getRed(), color.getColor().getGreen(), color.getColor().getBlue());
        }
      }
    }
  }

  /**
   * Detects image properties such as color frequency from the specified remote
   * image on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to detect
   *          properties on.
   * @param out
   *          A {@link PrintStream} to write
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectPropertiesGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.IMAGE_PROPERTIES).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
        for (ColorInfo color : colors.getColorsList()) {
          out.printf("fraction: %f\nr: %f, g: %f, b: %f\n", color.getPixelFraction(), color.getColor().getRed(), color.getColor().getGreen(), color.getColor().getBlue());
        }
      }
    }
  }

  /**
   * Detects whether the specified image has features you would want to
   * moderate.
   *
   * @param filePath
   *          The path to the local file used for safe search detection.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectSafeSearch(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();
        out.printf("adult: %s\nmedical: %s\nspoofed: %s\nviolence: %s\n", annotation.getAdult(), annotation.getMedical(), annotation.getSpoof(), annotation.getViolence());
      }
    }
  }

  /**
   * Detects whether the specified remote image on Google Cloud Storage has
   * features you would want to moderate.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to detect
   *          safe-search on.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectSafeSearchGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();
        out.printf("adult: %s\nmedical: %s\nspoofed: %s\nviolence: %s\n", annotation.getAdult(), annotation.getMedical(), annotation.getSpoof(), annotation.getViolence());
      }
    }
  }

  /**
   * Finds references to the specified image on the web.
   *
   * @param filePath
   *          The path to the local file used for web annotation detection.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectWebDetections(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.WEB_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // Search the web for usages of the image. You could use these signals
        // later
        // for user input moderation or linking external references.
        // For a full list of available annotations, see
        // http://g.co/cloud/vision/docs
        WebDetection annotation = res.getWebDetection();
        out.println("Entity:Id:Score");
        out.println("===============");
        for (WebEntity entity : annotation.getWebEntitiesList()) {
          out.println(entity.getDescription() + " : " + entity.getEntityId() + " : " + entity.getScore());
        }
        out.println("\nPages with matching images: Score\n==");
        for (WebPage page : annotation.getPagesWithMatchingImagesList()) {
          out.println(page.getUrl() + " : " + page.getScore());
        }
        out.println("\nPages with partially matching images: Score\n==");
        for (WebImage image : annotation.getPartialMatchingImagesList()) {
          out.println(image.getUrl() + " : " + image.getScore());
        }
        out.println("\nPages with fully matching images: Score\n==");
        for (WebImage image : annotation.getFullMatchingImagesList()) {
          out.println(image.getUrl() + " : " + image.getScore());
        }
      }
    }
  }

  /**
   * Detects whether the specified remote image on Google Cloud Storage has
   * features you would want to moderate.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to detect
   *          safe-search on.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectWebDetectionsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.WEB_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // Search the web for usages of the image. You could use these signals
        // later
        // for user input moderation or linking external references.
        // For a full list of available annotations, see
        // http://g.co/cloud/vision/docs
        WebDetection annotation = res.getWebDetection();
        out.println("Entity:Id:Score");
        out.println("===============");
        for (WebEntity entity : annotation.getWebEntitiesList()) {
          out.println(entity.getDescription() + " : " + entity.getEntityId() + " : " + entity.getScore());
        }
        out.println("\nPages with matching images: Score\n==");
        for (WebPage page : annotation.getPagesWithMatchingImagesList()) {
          out.println(page.getUrl() + " : " + page.getScore());
        }
        out.println("\nPages with partially matching images: Score\n==");
        for (WebImage image : annotation.getPartialMatchingImagesList()) {
          out.println(image.getUrl() + " : " + image.getScore());
        }
        out.println("\nPages with fully matching images: Score\n==");
        for (WebImage image : annotation.getFullMatchingImagesList()) {
          out.println(image.getUrl() + " : " + image.getScore());
        }
      }
    }
  }

  /**
   * Suggests a region to crop to for a local file.
   *
   * @param filePath
   *          The path to the local file used for web annotation detection.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectCropHints(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.CROP_HINTS).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        CropHintsAnnotation annotation = res.getCropHintsAnnotation();
        for (CropHint hint : annotation.getCropHintsList()) {
          out.println(hint.getBoundingPoly());
        }
      }
    }
  }

  /**
   * Suggests a region to crop to for a remote file on Google Cloud Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to detect
   *          safe-search on.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectCropHintsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.CROP_HINTS).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        CropHintsAnnotation annotation = res.getCropHintsAnnotation();
        for (CropHint hint : annotation.getCropHintsList()) {
          out.println(hint.getBoundingPoly());
        }
      }
    }
  }

  /**
   * Performs document text detection on a local image file.
   *
   * @param filePath
   *          The path to the local file to detect document text on.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectDocumentText(String filePath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();
      client.close();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        TextAnnotation annotation = res.getFullTextAnnotation();
        for (Page page : annotation.getPagesList()) {
          String pageText = "";
          for (Block block : page.getBlocksList()) {
            String blockText = "";
            for (Paragraph para : block.getParagraphsList()) {
              String paraText = "";
              for (Word word : para.getWordsList()) {
                String wordText = "";
                for (Symbol symbol : word.getSymbolsList()) {
                  wordText = wordText + symbol.getText();
                }
                paraText = paraText + wordText;
              }
              // Output Example using Paragraph:
              out.println("Paragraph: \n" + paraText);
              out.println("Bounds: \n" + para.getBoundingBox() + "\n");
              blockText = blockText + paraText;
            }
            pageText = pageText + blockText;
          }
        }
        out.println(annotation.getText());
      }
    }
  }

  /**
   * Performs document text detection on a local image file on Google Cloud
   * Storage.
   *
   * @param gcsPath
   *          The path to the remote file on Google Cloud Storage to detect
   *          document text on.
   * @param out
   *          A {@link PrintStream} to write the results to.
   * @throws Exception
   *           on errors while closing the client.
   * @throws IOException
   *           on Input/Output errors.
   */
  public static void detectDocumentTextGcs(String gcsPath, PrintStream out) throws Exception, IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
    AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();
      client.close();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          out.printf("Error: %s\n", res.getError().getMessage());
          return;
        }
        // For full list of available annotations, see
        // http://g.co/cloud/vision/docs
        TextAnnotation annotation = res.getFullTextAnnotation();
        for (Page page : annotation.getPagesList()) {
          String pageText = "";
          for (Block block : page.getBlocksList()) {
            String blockText = "";
            for (Paragraph para : block.getParagraphsList()) {
              String paraText = "";
              for (Word word : para.getWordsList()) {
                String wordText = "";
                for (Symbol symbol : word.getSymbolsList()) {
                  wordText = wordText + symbol.getText();
                }
                paraText = paraText + wordText;
              }
              // Output Example using Paragraph:
              out.println("Paragraph: \n" + paraText);
              out.println("Bounds: \n" + para.getBoundingBox() + "\n");
              blockText = blockText + paraText;
            }
            pageText = pageText + blockText;
          }
        }
        out.println(annotation.getText());
      }
    }
  }

  /*
   * 
   * public static void main(String[] args) { try {
   * 
   * LoggingFactory.init("info");
   * 
   * 
   * GoogleCloud google = (GoogleCloud) Runtime.start("google", "GoogleCloud");
   * // Runtime.start("gui", "SwingGui");
   * 
   * if (args.length != 2) { System.err.println("Usage:");
   * System.err.printf("\tjava %s inputImagePath outputImagePath\n",
   * GoogleCloud.class.getCanonicalName()); System.exit(1); }
   * 
   * Path inputPath = Paths.get(args[0]); Path outputPath = Paths.get(args[1]);
   * 
   * if (!outputPath.toString().toLowerCase().endsWith(".jpg")) {
   * System.err.println("outputImagePath must have the file extension 'jpg'.");
   * System.exit(1); }
   * 
   * // "API Project-c90c3d12e7d3.json"
   * 
   * // GoogleCloudService app = new // GoogleCloudService(getVisionService());
   * google.connect("../API Project-c90c3d12e7d3.json");
   * 
   * long ts = System.currentTimeMillis();
   * 
   * List<FaceAnnotation> faces = google.detectFaces(inputPath);
   * System.out.printf("Found %d face%s\n", faces.size(), faces.size() == 1 ? ""
   * : "s"); System.out.printf("Writing to file %s\n", outputPath);
   * google.writeWithFaces(inputPath, outputPath, faces);
   * 
   * google.getLabels("kitchen.jpg"); google.getLabels("plumbing.jpg");
   * google.getLabels("ship.jpg"); google.getLabels("greenball.jpg");
   * 
   * log.info("{} total ms", System.currentTimeMillis() - ts);
   * 
   * } catch (Exception e) { Logging.logError(e); } }
   * 
   */

}
