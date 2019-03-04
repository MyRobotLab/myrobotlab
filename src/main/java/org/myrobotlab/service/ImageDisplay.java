package org.myrobotlab.service;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
// import org.myrobotlab.image.DisplayedImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

// FIXME remove DisplayedImage class
public class ImageDisplay extends Service implements MouseListener, ActionListener, MouseMotionListener {

  transient private static GraphicsDevice gd;

  public final static Logger log = LoggerFactory.getLogger(ImageDisplay.class);
  private static final long serialVersionUID = 1L;

  // FIXME - incorporate small neat return (default broken image) can handle
  // file/resource/https
  protected static Image createImage(String path, String description) throws MalformedURLException {
    // URL imageURL = TrayIconDemo.class.getResource(path); FIXME use
    // getResourceDir
    return new ImageIcon(new URL(path), description).getImage();
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

    ServiceType meta = new ServiceType(ImageDisplay.class.getCanonicalName());
    meta.addDescription("Service to Display Images");
    meta.addCategory("display");

    return meta;
  }

  // Returns the Height-factor of the DisplayResolution.
  public static int getResolutionOfH() {
    return gd.getDisplayMode().getHeight();
  }

  // Returns the Width-factor of the DisplayResolution.
  public static int getResolutionOfW() {
    return gd.getDisplayMode().getWidth();
  }

  private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height, Integer type) {
    if (type == null) {
      type = originalImage.getType();
    }
    BufferedImage resizedImage = new BufferedImage(width, height, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, width, height, null);
    g.dispose();

    return resizedImage;
  }

  private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int width, int height, int type) {

    BufferedImage resizedImage = new BufferedImage(width, height, type);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, width, height, null);
    g.dispose();
    g.setComposite(AlphaComposite.Src);

    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    return resizedImage;
  }

  float currentAlpha = 0.0f;

  private JFrame currentFrame;

  boolean defaultAlwaysOnTop = false;

  transient Color defaultColor = Color.LIGHT_GRAY;

  boolean defaultFadeIn = false;

  boolean defaultFullScreen = false;

  boolean defaultMultiFrame = true;

  boolean defaultSystemTray = false;

  boolean defaultTransparentBackground = true;

  transient Map<String, JFrame> frames = new HashMap<String, JFrame>();

  /**
   * one and only full screen
   */
  transient JFrame fullscreen = null;

  // Displays an image in FullScreen mode.
  // @param source = path.
  /*
   * public void displayFullScreen(String source) {
   * 
   * BufferedImage image = loadImage(source);
   * 
   * if (fullscreen == null) { fullscreen = new JFrame();
   * 
   * JPanel imagePanel = new JPanel(new BorderLayout()); ImageIcon imageIcon =
   * new ImageIcon(image); fullscreenImageLabel = new JLabel(imageIcon); //
   * fullscreenImageLabel = new JLabel(); imagePanel.add(fullscreenImageLabel,
   * BorderLayout.CENTER); imagePanel.setBackground(Color.BLACK); // FIXME -
   * variable color
   * 
   * fullscreen.add(imagePanel);
   * 
   * // FIXME - some of these can be "reset" with member values // so they will
   * need to be out of this null checking if statement
   * fullscreen.setUndecorated(true); // TODO - look into the details of this //
   * even for non-fullscreen fullscreen.setBackground(new Color(1.0f, 1.0f,
   * 1.0f, 0.5f)); fullscreen.setBackground(Color.BLACK);
   * fullscreen.getContentPane().setBackground(Color.BLACK); // It sets the size
   * of the Frame to the size of the picture, if not it // will // be build a
   * boarder to the right end of the screen. //
   * fullscreen.setSize(image.getWidth() + wOffset, image.getHeight() + //
   * hOffset); // getResolution(); // fullscreen.setLocation(image.getwOffset(),
   * image.gethOffset()); fullscreen.toFront();
   * gd.setFullScreenWindow(fullscreen); //
   * fullscreen.setLocation(image.getwOffset(), image.gethOffset());
   * fullscreen.setVisible(true);
   * 
   * // Exit program on mouse click fullscreen.addMouseListener(new
   * MouseListener() { public void mouseClicked(MouseEvent e) {
   * gd.setFullScreenWindow(null); fullscreen.dispose(); }
   * 
   * @Override public void mouseEntered(MouseEvent arg0) { // TODO
   * Auto-generated method stub
   * 
   * }
   * 
   * @Override public void mouseExited(MouseEvent arg0) { // TODO Auto-generated
   * method stub
   * 
   * }
   * 
   * @Override public void mousePressed(MouseEvent arg0) { // TODO
   * Auto-generated method stub
   * 
   * }
   * 
   * @Override public void mouseReleased(MouseEvent arg0) { // TODO
   * Auto-generated method stub
   * 
   * } });
   * 
   * }
   * 
   * ImageIcon icon = null;
   * 
   * try { icon = new ImageIcon(image); } catch (Exception e) {
   * log.error("could not set image icon to {}", source); return; }
   * 
   * fullscreenImageLabel.setIcon(icon);
   * 
   * // frames.add(f); }
   */

  transient JLabel fullscreenImageLabel = null;

  int hOffset = 0;

  Timer timer = null;

  int wOffset = 0;

  public ImageDisplay(String n) {
    super(n);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    currentAlpha += 0.05f;
    if (currentAlpha > 1) {
      currentAlpha = 1;
      // timer.stop();
      timer.cancel();
    }
    // repaint();
  }

  public void closeAll() {
    for (JFrame frame : frames.values()) {
      frame.dispose();
    }
    frames.clear();
  }

  public void display2(String src) throws MalformedURLException, AWTException {
    display2(src, null, null, null, null, null, null, null);
  }

  // FIXME - setIcon
  // FIXME - fullscreen
  // FIXME - width=200 ? and scale proportionately
  // FIXME - scale %
  // FIXME - transparency/alpha
  // FIXME - alignment
  // FIXME - fade in ?
  // FIXME - gif animation
  // FIXME - from http/https/localfile OR Resource !!! use getResourceDir
  // FIXME - cache locally /data/DisplayImage/ -> boolean cacheFiles = true;
  public void display2(String src, Boolean fullscreen, String bgColorStr, Integer width, Integer height, Double scaling, Float alpha, Boolean fadeIn)
      throws MalformedURLException, AWTException {
    Color bgColor = defaultColor;
    if (bgColorStr != null) {
      try {
        bgColor = Color.decode(bgColorStr);
      } catch (Exception e) {
      }
    }

    // FYI - need buffered image to do lower level manipulations like sizing,
    // scaling, and alpha changes
    // BUT animated gifs do not successfully convert to BufferedImages
    BufferedImage image = loadImage(src);

    // FIXME - this will explode if image comes back null (which it will for
    // animated gifs)
    // FIXME - vs explicit width height vs explicit proportional width !
    if (scaling != null && scaling != 1.0f) {
      int scaledWidth = Math.round(image.getWidth() * scaling.floatValue());
      int scaledHeight = Math.round(image.getHeight() * scaling.floatValue());
      image = resizeImage(image, scaledWidth, scaledHeight);
    }

    boolean fade = defaultFadeIn;
    if (fadeIn != null) {
      fade = fadeIn;
    }

    if (fade) {
      // FIXME - implement
    }

    ImageIcon icon = null;
    if (image == null) // animated gif
    {
      icon = new ImageIcon(new URL(src));
    } else {
      icon = new ImageIcon(image);
    }
    JLabel label = new JLabel(icon);
    label.setBackground(bgColor);

    if (defaultMultiFrame || currentFrame == null) {
      currentFrame = new JFrame();
      // FIXME - make variable - also must put "default" of window handling ..
      currentFrame.setUndecorated(true);
      // f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      currentFrame.add(label, BorderLayout.CENTER);
    }

    currentFrame.addMouseListener(this);
    currentFrame.addMouseMotionListener(this);

    currentFrame.getContentPane().setBackground(bgColor);// <-- THIS ONE MAKES A
                                                         // DIFFERENCE !!!
    currentFrame.setBackground(bgColor);
    currentFrame.setSize(image.getWidth() + wOffset, image.getHeight() + hOffset);

    currentFrame.setLocation(getDisplayWidth() / 2 - image.getWidth() / 2, getDisplayHeight() / 2 - (image.getHeight() + hOffset) / 2);

    // FIXME - do i have to check the size ???
    currentFrame.setIconImage(image);

    JLabel currentLabel = (JLabel) currentFrame.getContentPane().getComponent(0);
    ImageIcon currentIcon = (ImageIcon) currentLabel.getIcon();
    currentLabel.setIcon(icon);

    currentFrame.toFront();

    if (defaultTransparentBackground) {
      currentFrame.setBackground(new Color(0, 0, 0, 0));
    }

    // currentFrame.setLocation(image.getwOffset(), image.gethOffset());
    // FIXME defaultVisible
    currentFrame.setVisible(true);

    if (defaultAlwaysOnTop) {
      currentFrame.setAlwaysOnTop(true);
    }

    // TODO - make better / don't use setImageAutoSize (very bad algorithm)
    if (defaultSystemTray && SystemTray.isSupported()) {
      log.info("SystemTray is supported");
      SystemTray tray = SystemTray.getSystemTray();
      Dimension trayIconSize = tray.getTrayIconSize();

      TrayIcon trayIcon = new TrayIcon(createImage(src, "tray icon"));
      trayIcon.setImageAutoSize(true);

      tray.add(trayIcon);
    }

    // fullscreen ... begin
    Boolean fs = fullscreen;
    if (fs == null) {
      fs = defaultFullScreen;
    }

    if (fs) {
      gd.setFullScreenWindow(currentFrame);
    }

    if (defaultMultiFrame) {
      frames.put(src, currentFrame);
    }
  }

  // Displays a faded image.
  // @param source = path.
  // @param alpha = Value how much the image is faded float from 0.0 to 1.0.
  public void displayAlpha(String src, float alpha) throws MalformedURLException, AWTException {
    display2(src, null, null, null, null, null, alpha, null);
  }

  // Displays an image by Fading it in.
  // @param source = path.
  public void displayFadeIn(String src) throws MalformedURLException, AWTException {
    display2(src, null, null, null, null, null, null, true);
    /*
     * DisplayedImage image = new DisplayedImage(source);
     * log.info("Loading image done"); buildFrame(image);
     */
  }

  // Displays a faded image in FullScreen mode.
  // @param source = path.
  // @param alpha = Value how much the image is faded float from 0.0 to 1.0.
  public void displayFullScreen(String src) throws MalformedURLException, AWTException {
    display2(src, true, null, null, null, null, null, null);
  }

  // Displays a resized image in FullScreen mode.
  // @param source = path.
  // @param scaling = scale factor to resize the image.
  public void displayScaled(String src, double scaling) throws MalformedURLException, AWTException {
    display2(src, null, null, null, null, scaling, null, null);
  }

  // Exits the Fullscreen mode.
  public void exitFS() {
    gd.setFullScreenWindow(null);
  }

  public int getDisplayHeight() {
    return gd.getDisplayMode().getHeight();
  }

  // necessary ?
  public int getDisplayWidth() {
    return gd.getDisplayMode().getWidth();
  }

  private BufferedImage loadImage(String src) {
    BufferedImage image = null;
    try {
      // get the image from web if its an Url
      log.info("Loading image: ");
      if (src.startsWith("http://") || (src.startsWith("https://"))) {
        log.info("from url...");
        URL url = new URL(src);
        image = ImageIO.read(url);
      } else {
        log.info("from file...");
        image = ImageIO.read(new File(src));
      }

    } catch (Exception exp) {
      exp.printStackTrace();
    }
    return image;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // gd.setFullScreenWindow(null);
    // fullscreen.dispose();
    log.info("mouseClicked {}", e);
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    log.info("mouseEntered {}", e);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    log.info("mouseExited {}", e);
  }

  Cursor lastCursor = null;

  @Override
  public void mouseReleased(MouseEvent e) {
    if (lastCursor != null) {
      currentFrame.setCursor(lastCursor);
    }
  }

  private BufferedImage resizeImage(BufferedImage image, int scaledWidth, int scaledHeight) {
    return resizeImage(image, scaledWidth, scaledHeight, null);
  }

  public void setAlwaysOnTop(boolean b) {
    defaultAlwaysOnTop = b;
  }

  // FIXME - connect !!!
  public void setColor(String string) {
    // TODO Auto-generated method stub

  }

  public void setFullScreen(boolean b) {
    defaultFullScreen = b;
  }

  public void setMultiFrame(boolean b) {
    defaultMultiFrame = b;
  }

  @Override
  public void startService() {
    super.startService();
    if (GraphicsEnvironment.isHeadless()) {
      log.warn("in headless mode - can not start awt components");
      return;
    } else {
      gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }
  }

  public void stopService() {
    super.stopService();
    closeAll();
  }

 
  int mouseXoffset = 0;
  int mouseYoffset = 0;

  private Point initialDragPoint;
  
  @Override
  public void mousePressed(MouseEvent e) {
    log.info("mousePressed {}", e);
    mouseXoffset=e.getX();
    mouseYoffset=e.getY();
    lastCursor = currentFrame.getCursor();
    currentFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
    initialDragPoint = e.getPoint();
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {
    log.info("mouseDragged {}", e);  
    currentFrame.setLocation(currentFrame.getX()+e.getX()-mouseXoffset, currentFrame.getY()+e.getY()-mouseYoffset);
    currentFrame.repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    log.info("mouseMoved {}", e);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");
      // FIXME - get gifs working
      display.setAlwaysOnTop(true);
      // display.display("https://media.giphy.com/media/snA2OVsg9sMRW/giphy.gif");
      // display.display2("http://www.pngmart.com/files/7/SSL-Download-PNG-Image.png");
      // display.display2("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/1024px-Noto_Emoji_Pie_1f62c.svg.png");
      // display.display2("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/32px-Noto_Emoji_Pie_1f62c.svg.png");
      // display.displayScaled("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/1024px-Noto_Emoji_Pie_1f62c.svg.png",
      // 0.0278f);
      // display.displayFullScreen("http://r.ddmcdn.com/w_830/s_f/o_1/cx_0/cy_220/cw_1255/ch_1255/APL/uploads/2014/11/dog-breed-selector-australian-shepherd.jpg");
      // display.display2("C:\\Users\\grperry\\Desktop\\tenor.gif");
      display.display2("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
