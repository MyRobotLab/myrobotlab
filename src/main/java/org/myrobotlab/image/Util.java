/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.image;

import static org.bytedeco.opencv.global.opencv_dnn.NMSBoxes;
import static org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform;
import static org.bytedeco.opencv.global.opencv_imgproc.warpPerspective;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.RotatedRect;
import org.bytedeco.opencv.opencv_core.Size;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.CloseableFrameConverter;
import org.myrobotlab.opencv.DetectedText;
import org.slf4j.Logger;

/**
 * utilities with a swing dependency
 * 
 * references: http://www.colblindor.com/color-name-hue/ - excellent resource
 * 
 */

public class Util {

  /*
   * Integer.toHexString( color.getRGB() & 0x00ffffff ) public String
   * printPixelARGB(int pixel) { int alpha = (pixel >> 24) & 0xff; int red =
   * (pixel >> 16) & 0xff; int green = (pixel >> 8) & 0xff; int blue = (pixel) &
   * 0xff; System.out.println("argb: " + alpha + ", " + red + ", " + green +
   * ", " + blue); }
   */
  public final static Logger log = LoggerFactory.getLogger(Util.class.getCanonicalName());

  // static HashMap <int,>
  // array [r][g][b]
  // TODO - fix arrggh head hurts
  final static String[][][] colorNameCube = {
      { { "black", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "navy", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
          { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
          { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" } },

      { { "maroon", "xxx", "xxx" }, { "green", "xxx", "xxx" }, { "blue", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
          { "xxx", "gray", "xxx" }, { "xxx", "xxx", "xxx" },
          { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" } },

      { { "red", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "lime", "y0", "z0" }, { "xxx", "xxx", "xxx" },
          { "xxx", "xxx", "xxx" }, { "x0", "y0", "z0" }, { "xxx", "xxx", "xxx" },
          { "xxx", "xxx", "xxx" }, { "x0", "y0", "white" } } };

  public static BufferedImage brighten(BufferedImage bufferedImage, float amount) {
    // brighten 30% = 1.3f darken by 10% = .9f
    RescaleOp op = new RescaleOp(amount, 0, null);
    bufferedImage = op.filter(bufferedImage, null);
    return bufferedImage;
  }

  /**
   * Produces a copy of the supplied image
   * 
   * @param image
   *              The original image
   * @return The new BufferedImage
   */
  public static BufferedImage copyImage(BufferedImage image) {
    return scaledImage(image, image.getWidth(), image.getHeight());
  }

  /**
   * Creates an image compatible with the current display
   * 
   * @param width
   *               int
   * @param height
   *               int
   * 
   * @return A BufferedImage with the appropriate color model
   */
  public static BufferedImage createCompatibleImage(int width, int height) {
    GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
        .getDefaultConfiguration();
    return configuration.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
  }

  public static ImageIcon getBrightenedIcon(final String path, final float amount) {
    ImageIcon imageIcon = new ImageIcon(brighten(loadBufferedImage(path), amount));
    return imageIcon;
  }

  static public String getColorString(Color c) {
    // TODO - static this
    String[][][] colorDictionary = new String[3][3][3];

    colorDictionary[0][0][0] = "black";
    colorDictionary[1][1][1] = "gray";
    colorDictionary[2][2][2] = "white";

    colorDictionary[2][0][0] = "red";
    colorDictionary[0][2][0] = "lime";
    colorDictionary[0][0][2] = "blue";

    colorDictionary[0][2][2] = "aqua";
    colorDictionary[2][0][2] = "fushia";
    colorDictionary[2][2][0] = "yellow";

    colorDictionary[1][0][0] = "maroon";
    colorDictionary[0][1][0] = "green";
    colorDictionary[0][0][1] = "navy";

    colorDictionary[0][1][1] = "teal";
    colorDictionary[1][0][1] = "purple";
    colorDictionary[1][1][0] = "olive";

    colorDictionary[2][1][1] = "pink";
    colorDictionary[1][2][1] = "auquamarine";
    colorDictionary[1][1][2] = "sky blue";

    colorDictionary[1][2][2] = "pale blue";
    colorDictionary[2][1][2] = "plum";
    colorDictionary[2][2][1] = "apricot";

    colorDictionary[0][1][2] = "bondi blue";
    colorDictionary[1][0][2] = "amethyst";
    colorDictionary[1][2][0] = "brown";

    colorDictionary[2][1][0] = "persimmon";
    colorDictionary[2][0][1] = "rose";
    colorDictionary[0][2][1] = "persian green";

    // colorDictionary [1][2][0] = "lawn green";
    // colorDictionary [2][1][1] = "salmon";

    String ret = "";
    int red = c.getRed();
    int green = c.getGreen();
    int blue = c.getBlue();

    // 63 < divisor < 85
    red = red / 64 - 1;
    green = green / 64 - 1;
    blue = blue / 64 - 1;

    if (red < 1)
      red = 0;
    if (green < 1)
      green = 0;
    if (blue < 1)
      blue = 0;

    ret = colorDictionary[red][green][blue];

    return ret;
  }

  public static Color getGradient(int pos, int total) {
    float gradient = 1.0f / total;
    return new Color(Color.HSBtoRGB((pos * (gradient)), 0.8f, 0.7f));
  }

  // get images & image icons - with defaults
  public static Image getImage(String path) {
    return getImage(path, "Unknown.png");
  }

  /**
   * this method should be avoided - it uses getResourceDir while is should
   * expect full path - the calling Service should be using getResource or if
   * its not a Service it should be using Service.getResource(class,
   * resourceName)
   * 
   * @param path
   *                     the path to the image
   * @param defaultImage
   *                     a default image to use
   * @return an image
   * 
   */
  @Deprecated
  public static Image getImage(String path, String defaultImage) {
    Image icon = null;
    File imgURL = new File(getResourceDir() + File.separator + path);
    if (isExistRessourceElement(path)) {
      try {
        icon = ImageIO.read(imgURL);
        // log.info("getImage({})", imgURL.getPath());
        return icon;
      } catch (IOException e) {
        log.error("getImage threw", e);
      }
    }

    // trying default image
    imgURL = new File(getResourceDir() + File.separator + defaultImage);

    if (imgURL != null) {
      try {
        icon = ImageIO.read(imgURL);
        return icon;
      } catch (IOException e) {
        log.error("read image threw trying to read file {}", imgURL, e);
      }
    }

    log.error("Get Image : Couldn't find file: " + path + " or default " + defaultImage);
    return null;

  }

  public static ImageIcon getImageIcon(String path) {
    return getImageIcon(path, null);
  }

  /**
   * this method should be avoided - it uses getResourceDir while is should
   * expect full path - the calling Service should be using getResource or if
   * its not a Service it should be using Service.getResource(class,
   * resourceName) by default will take the resource.dir property if set.
   * 
   * If mrl is running inside of a jar it will use the user.dir + "resource" as
   * the directory. If mrl is not in a jar, it will use
   * src/main/resources/resource
   * 
   * @return current resource directory
   */
  @Deprecated /*
               * Resource references do not belong here - the ServiceType and
               * perhaps even the ServiceName are needed in order to provide context. This
               * method should be removed, or parameters provided for ServiceType or
               * ServiceName
               */
  public static String getResourceDir() {
    return Service.getResourceRoot();
  }

  /**
   * Check if file exist from current resource directory
   * 
   * @param element
   *                - element to be tested
   * @return boolean
   */
  @Deprecated /* expect full path - don't use getResourceDir */
  private static Boolean isExistRessourceElement(String element) {
    File f = new File(getResourceDir() + File.separator + element);
    if (!f.exists()) {
      return false;
    }
    return true;
  }

  @Deprecated /* expect full path - don't use getResourceDir */
  public static final ImageIcon getResourceIcon(String path) {
    ImageIcon icon = null;

    String imgURL = path;
    if (isExistRessourceElement(path)) {
      icon = new ImageIcon(Util.getResourceDir() + File.separator + imgURL);
      return icon;
    } else {
      log.error("Get Resource Icon - Couldn't find file: {}", path);
      return null;
    }
  }

  public static ImageIcon getScaledIcon(final String name, final int x, final int y) {
    return new ImageIcon(getImage(name).getScaledInstance(x, y, java.awt.Image.SCALE_SMOOTH));
  }

  public static ImageIcon getScaledIcon(final Image image, final double scale) {
    ImageIcon scaledIcon = new ImageIcon(image) {
      private static final long serialVersionUID = 1L;

      @Override
      public int getIconHeight() {
        return (int) (image.getHeight(null) * scale);
      }

      @Override
      public int getIconWidth() {
        return (int) (image.getWidth(null) * scale);
      }

      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(image, x, y, getIconWidth(), getIconHeight(), c);
      }
    };
    return scaledIcon;
  }

  // This method returns true if the specified image has transparent pixels
  public static boolean hasAlpha(Image image) {
    // If buffered image, the color model is readily available
    if (image instanceof BufferedImage) {
      BufferedImage bimage = (BufferedImage) image;
      return bimage.getColorModel().hasAlpha();
    }

    // Use a pixel grabber to retrieve the image's color model;
    // grabbing a single pixel is usually sufficient
    PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
    try {
      pg.grabPixels();
    } catch (InterruptedException e) {
    }

    // Get the image's color model
    ColorModel cm = pg.getColorModel();
    return cm.hasAlpha();
  }

  // graciously lifted from
  // http://www.exampledepot.com/egs/java.awt.image/image2buf.html
  public static BufferedImage ImageToBufferedImage(Image image) {
    // This method returns a buffered image with the contents of an image
    if (image instanceof BufferedImage) {
      return (BufferedImage) image;
    }

    // This code ensures that all the pixels in the image are loaded
    image = new ImageIcon(image).getImage();

    // Determine if the image has transparent pixels; for this method's
    // implementation, see Determining If an Image Has Transparent Pixels
    boolean hasAlpha = hasAlpha(image);

    // Create a buffered image with a format that's compatible with the
    // screen
    BufferedImage bimage = null;
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    try {
      // Determine the type of transparency of the new buffered image
      int transparency = Transparency.OPAQUE;
      if (hasAlpha) {
        transparency = Transparency.BITMASK;
      }

      // Create the buffered image
      GraphicsDevice gs = ge.getDefaultScreenDevice();
      GraphicsConfiguration gc = gs.getDefaultConfiguration();
      bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
    } catch (HeadlessException e) {
      // The system does not have a screen
    }

    if (bimage == null) {
      // Create a buffered image using the default color model
      int type = BufferedImage.TYPE_INT_RGB;
      if (hasAlpha) {
        type = BufferedImage.TYPE_INT_ARGB;
      }
      bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
    }

    // Copy image to buffered image
    Graphics g = bimage.createGraphics();

    // Paint the image onto the buffered image
    g.drawImage(image, 0, 0, null);
    g.dispose();

    return bimage;

  }

  public static BufferedImage loadBufferedImage(String path)

  {
    BufferedImage bi;
    try {
      bi = ImageIO.read(new File(Util.getResourceDir() + File.separator + path));
    } catch (IOException e) {
      log.error("could not find image " + path);
      return null;
    }
    return bi;
  }

  public final static BufferedImage readBufferedImage(String filename) {
    try {
      File file = new File(filename);
      BufferedImage img = ImageIO.read(file);
      return img;
    } catch (IOException e) {
      return null;
    }
  }

  public final static String getImageAsBase64(String filename) {
    return getImageAsString(filename, "png");
  }

  public final static String getImageAsString(String filename, String type) {
    try {
      File file = new File(filename);
      BufferedImage img = ImageIO.read(file);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ImageIO.write(img, "png", bos);
      return String.format("data:image/%s;base64,%s", type, CodecUtils.toBase64(bos.toByteArray()));
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Produces a resized image that is of the given dimensions
   * 
   * @param image
   *               The original image
   * @param width
   *               The desired width
   * @param height
   *               The desired height
   * @return The new BufferedImage
   */
  public static BufferedImage scaledImage(BufferedImage image, int width, int height) {
    BufferedImage newImage = createCompatibleImage(width, height);
    Graphics graphics = newImage.createGraphics();

    graphics.drawImage(image, 0, 0, width, height, null);

    graphics.dispose();
    return newImage;
  }

  public static BufferedImage toGray(BufferedImage bufferedImage) {
    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    ColorConvertOp op = new ColorConvertOp(cs, null);
    bufferedImage = op.filter(bufferedImage, null);

    return bufferedImage;
  }

  public final static void writeBufferedImage(BufferedImage newImg, String filename) {
    writeBufferedImage(newImg, filename, null);
  }

  public final static void writeBufferedImage(BufferedImage newImg, String filename, String format) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(newImg, "jpg", baos);
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(baos.toByteArray());
      fos.close();
    } catch (IOException e) {
      log.error("writeBufferedImage threw", e);
    }

  }

  public Color getColor(String colorName) {
    try {
      // Find the field and value of colorName
      Field field = Class.forName("java.awt.Color").getField(colorName);
      return (Color) field.get(null);
    } catch (Exception e) {
      return null;
    }
  }

  @Deprecated /* expect full path - don't use getResourceDir */
  public static ImageIcon getImageIcon(String path, String description) {
    ImageIcon icon = null;
    String resourcePath = Util.getResourceDir() + File.separator + path;
    // ImageIcon requires forward slash in the filename (unix/internet style
    // convention)
    resourcePath = resourcePath.replaceAll("\\\\", "/");
    icon = new ImageIcon(resourcePath, description);
    return icon;
  }

  //
  public static void fourPointsTransform(Mat frame, Point2f vertices, Mat result, Size outputSize) {
    Point2f targetVertices = new Point2f(4);
    // write the data into the array
    targetVertices.position(0);
    targetVertices.put(new Point2f(0, outputSize.height() - 1));
    targetVertices.position(1);
    targetVertices.put(new Point2f(0, 0));
    targetVertices.position(2);
    targetVertices.put(new Point2f(outputSize.width() - 1, 0));
    targetVertices.position(3);
    targetVertices.put(new Point2f(outputSize.width() - 1, outputSize.height() - 1));
    // reset the pointer to the beginning of the array.
    targetVertices.position(0);
    Mat rotationMatrix = getPerspectiveTransform(vertices, targetVertices);
    warpPerspective(frame, result, rotationMatrix, outputSize);
    // Ok.. now the result should have the cropped image?
    // show(OpenCV.toImage(result), "four points...");
  }

  public static Point2f scaleVertices(RotatedRect box, Point2f ratio) {
    Point2f vertices = new Point2f(4);
    box.points(vertices);
    for (int i = 0; i < 4; i++) {
      vertices.position(i);
      vertices.x(vertices.x() * ratio.x());
      vertices.y(vertices.y() * ratio.y());
    }
    vertices.position(0);
    return vertices;
  }

  public static Mat cropAndRotate(Mat frame, RotatedRect largerBox, Size outputSize, Point2f ratio) {
    // Input rotatedRect is on the neural network scaled image
    // this needs to be scaled up to the original resolution by the ratio.
    Point2f vertices = Util.scaleVertices(largerBox, ratio);
    // a target for the cropped image
    Mat cropped = new Mat();
    // do the cropping of the original image, scaled vertices and a target
    // output size
    Util.fourPointsTransform(frame, vertices, cropped, outputSize);
    // return the cropped mat that is populated with the cropped image from the
    // original input image.
    return cropped;
  }

  public static ArrayList<DetectedText> applyNMSBoxes(float threshold, ArrayList<RotatedRect> boxes,
      ArrayList<Float> confidences, float nmsThreshold) {
    RectVector boxesRV = new RectVector();
    for (RotatedRect rr : boxes) {
      boxesRV.push_back(rr.boundingRect());
    }
    FloatPointer confidencesFV = arrayListToFloatPointer(confidences);
    IntPointer indicesIp = new IntPointer();
    NMSBoxes(boxesRV, confidencesFV, threshold, nmsThreshold, indicesIp);
    ArrayList<DetectedText> goodOnes = new ArrayList<DetectedText>();
    for (int m = 0; m < indicesIp.limit(); m++) {
      int i = indicesIp.get(m);
      RotatedRect box = boxes.get(i);
      confidencesFV.position(i);
      // we don't have text yet, that will be filled in later by the ocr step.
      DetectedText dt = new DetectedText(box, confidencesFV.get(), null);
      goodOnes.add(dt);
    }
    return goodOnes;
  }

  // utilty helper function to put an array of floats into a javacpp float
  // pointer
  public static FloatPointer arrayListToFloatPointer(ArrayList<Float> confidences) {
    // create a float pointer of the correct size
    FloatPointer confidencesFV = new FloatPointer(confidences.size());
    for (int i = 0; i < confidences.size(); i++) {
      // update the pointer and put the float in
      confidencesFV.position(i);
      confidencesFV.put(confidences.get(i));
    }
    // reset the pointer position back to the head.
    confidencesFV.position(0);
    return confidencesFV;
  }

  /**
   * deserialize from a png byte array to a base64 encoded string
   * for display inline in html.
   * 
   * @param bytes
   * @return
   */
  public static String bytesToBase64Jpg(byte[] bytes) {
    //
    // let's assume we're a buffered image .. those are serializable :)
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      BufferedImage bufImage = ImageIO.read(new ByteArrayInputStream(bytes));
      ImageIO.write(bufImage, "jpg", os);
      os.close();
    } catch (IOException e) {
      // TODO: we should probably just return null and let the caller figure this out.
      return "ERROR converting image to jpg base64.";
    }

    String data = String.format("data:image/%s;base64,%s", "jpg", CodecUtils.toBase64(os.toByteArray()));
    return data;
  }

  /**
   * Helper method to serialize an IplImage into a byte array. returns the bytes
   * of an image in for format specified, png, jpg, bmp,.etc...
   * 
   * @param image
   *               input iage
   * @param format
   *               defaults to jpg..
   * @return byte array of image
   * @throws IOException
   *                     boom
   * 
   */
  public static byte[] imageToBytes(IplImage image, String format) throws IOException {

    // lets make a buffered image
    CloseableFrameConverter converter = new CloseableFrameConverter();
    BufferedImage buffImage = converter.toBufferedImage(image);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      ImageIO.write(buffImage, format, stream);
    } catch (IOException e) {
      // This *shouldn't* happen with a ByteArrayOutputStream, but if it
      // somehow does happen, then we don't want to just ignore it
      throw new RuntimeException(e);
    }
    converter.close();
    return stream.toByteArray();
  }

  /**
   * Helper method to serialize an IplImage into a byte array. returns a jpg
   * version of the original image
   * 
   * @param image
   *              input iage
   * @return byte array of image
   * @throws IOException
   *                     boom
   * 
   */
  public static byte[] imageToBytes(IplImage image) throws IOException {
    return Util.imageToBytes(image, "jpg");
  }

  /**
   * Uses ImageIO to read the byte array into a buffered image.
   * It then converts it to an IplImage
   * 
   * @param bytes
   *              input bytes
   * @return an iplimage
   * @throws IOException
   *                     boom
   * 
   */
  public static IplImage bytesToImage(byte[] bytes) throws IOException {
    //
    // let's assume we're a buffered image .. those are serializable :)
    BufferedImage bufImage = ImageIO.read(new ByteArrayInputStream(bytes));
    ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
    IplImage iplImage = iplConverter.convert(java2dConverter.convert(bufImage));
    // now convert the buffered image to ipl image
    return iplImage;
    // Again this could be try with resources but the original example was in
    // Scala
  }

}
