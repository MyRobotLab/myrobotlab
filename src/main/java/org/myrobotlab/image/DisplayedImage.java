package org.myrobotlab.image;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ImageDisplay;
import org.slf4j.Logger;

public class DisplayedImage extends JPanel implements ActionListener {

  public final static Logger log = LoggerFactory.getLogger(DisplayedImage.class);

  private static final long serialVersionUID = 1L;
  transient BufferedImage image; // Stores the actual image.
  Timer timer; // Timer for the FadeIn Effect.
  private float alpha = 0f; // Stores the alpha value.
  private int hImage, wImage; // Stores the height and width of an image.
  private float scaling = 1.0f; // Stores the scaling factor which influences
  // the height and width.
  private float scaleOnH = 1.0f, scaleOnW = 1.0f;
  private int wOffset = 0, hOffset = 0;

  // Displays an image with the FadeIn effect.
  public DisplayedImage(String source) {
    image = loadImage(source);
    setSize();
    timer = new Timer(100, this);
    timer.start();
  }

  // Displays an image to @alpha% and with the @scaling scale-factor.
  public DisplayedImage(String source, float alpha, float scaling) {
    this.scaling = scaling;
    image = loadImage(source);
    setSize();
    this.alpha = alpha;
  }

  // Displays an image to @alpha%.
  public DisplayedImage(String source, float alpha) {
    image = loadImage(source);
    setSize();
    this.alpha = alpha;

  }

  // Displays an image to @alpha% in FullscreenMode.
  public DisplayedImage(String source, float alpha, boolean FS) {
    image = loadImage(source);
    setSize();
    this.scaleOnH = ImageDisplay.getResolutionOfH() / (float) hImage;
    log.info("H: " + scaleOnH);
    this.scaleOnW = ImageDisplay.getResolutionOfW() / (float) wImage;
    log.info("W: " + scaleOnW);
    if (scaleOnH <= scaleOnW) {
      this.scaling = scaleOnH;
      this.wOffset = (int) (ImageDisplay.getResolutionOfW() - wImage * scaling) / 2;
    } else {
      this.scaling = scaleOnW;
      this.hOffset = (int) (ImageDisplay.getResolutionOfH() - hImage * scaling) / 2;
    }
    log.info("S: " + scaling);
    setSize();
    this.alpha = alpha;

  }

  // Scales the image to the right size / defines hImage and wImage.
  private void setSize() {
    // hImage = image.getHeight(null);
    // wImage = image.getWidth(null);
    hImage = (int) (image.getHeight(null) * scaling);
    wImage = (int) (image.getWidth(null) * scaling);
  }

  // Returns the height of the image.
  public int getHeight() {
    return hImage;
  }

  // Returns the width of the image.
  public int getWidth() {
    return wImage;
  }

  // Returns the hOffset of the image.
  public int getwOffset() {
    return wOffset;
  }

  // Returns the wOffset of the image.
  public int gethOffset() {
    return hOffset;
  }

  // Load the image from a source and returns it as Buffered Image;
  private BufferedImage loadImage(String source) {
    try {
      // get the image from web if its an Url
      log.info("Loading image: ");
      if (source.startsWith("http://") || (source.startsWith("https://"))) {
        log.info("from url...");
        URL url = new URL(source);
        image = ImageIO.read(url);
      }
      // else get it from a file
      else {
        log.error("from file...");
        image = ImageIO.read(new File(source));
      }

    } catch (Exception exp) {
      exp.printStackTrace();
    }
    return image;
  }

  // Creates the correct graphic of the image.
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    g2d.drawImage(image, 0, 0, wImage, hImage, 0, 0, image.getWidth(), image.getHeight(), null);
  }

  // Performs the FadeIn effect.
  public void actionPerformed(ActionEvent e) {
    alpha += 0.05f;
    if (alpha > 1) {
      alpha = 1;
      timer.stop();
    }
    repaint();
  }
}