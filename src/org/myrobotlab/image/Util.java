/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.image;

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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
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
  final static String[][][] colorNameCube = { { { "black", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "navy", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
      { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" } },

      { { "maroon", "xxx", "xxx" }, { "green", "xxx", "xxx" }, { "blue", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "gray", "xxx" }, { "xxx", "xxx", "xxx" },
          { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" } },

      { { "red", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "lime", "y0", "z0" }, { "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" }, { "x0", "y0", "z0" }, { "xxx", "xxx", "xxx" },
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
   *          The original image
   * @return The new BufferedImage
   */
  public static BufferedImage copyImage(BufferedImage image) {
    return scaledImage(image, image.getWidth(), image.getHeight());
  }

  /**
   * Creates an image compatible with the current display
   * @param width int
   * @param height int
   * 
   * @return A BufferedImage with the appropriate color model
   */
  public static BufferedImage createCompatibleImage(int width, int height) {
    GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
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
    return getImage(path, "unknown.png");
  }

  public static Image getImage(String path, String defaultImage) {
    Image icon = null;
    File imgURL=new File(getRessourceDir()+path);
    if (isExistRessourceElement(path)) {
      try {
        icon = ImageIO.read(imgURL);
        //log.info(imgURL.getPath());
        return icon;
      } catch (IOException e) {
        Logging.logError(e);
      }
    }

    // trying default image
    imgURL=new File(getRessourceDir()+defaultImage);

    if (imgURL != null) {
      try {
        icon = ImageIO.read(imgURL);
        return icon;
      } catch (IOException e) {
        Logging.logError(e);
      }
    }

    log.error("Couldn't find file: " + path + " or default " + defaultImage);
    return null;

  }

  public static ImageIcon getImageIcon(String path) {
    ImageIcon icon = null;
    String resourcePath = String.format("/resource/%s", path);
    java.net.URL imgURL = Util.class.getResource(resourcePath);
    if (imgURL != null) {
      icon = new ImageIcon(imgURL);
      return icon;
    } else {
      log.error(String.format("Couldn't find file: %s", resourcePath));
      return null;
    }
  }

  /**
   * @return current resource directory
   */
  public static String getRessourceDir() {
    String ressourceDir=System.getProperty("user.dir") + File.separator + "resource"+ File.separator;
    if (!FileIO.isJar()) {
      ressourceDir=System.getProperty("user.dir") + File.separator + "src/resource"+ File.separator;
    }
    return ressourceDir; 
  }
  
  /**
   * Check if file exist from current resource directory
   * 
   * @return boolean
   */
  public static Boolean isExistRessourceElement(String element) {
    File f=new File(getRessourceDir()+element);
    if (!f.exists()) {
      return false;
    }
    return true;
  }
  
  public static final ImageIcon getResourceIcon(String path) {
    ImageIcon icon = null;

    String imgURL = getRessourceDir() + path;
    if (isExistRessourceElement(path)) {
      icon = new ImageIcon(imgURL);
      return icon;
    } else {
      log.error("Couldn't find file: " + path);
      return null;
    }
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
      bi = ImageIO.read(Util.class.getResource("/resource/" + path));
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

  /**
   * Produces a resized image that is of the given dimensions
   * 
   * @param image
   *          The original image
   * @param width
   *          The desired width
   * @param height
   *          The desired height
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
      Logging.logError(e);
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

}
