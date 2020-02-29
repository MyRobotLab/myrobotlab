package org.myrobotlab.service;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
// import org.myrobotlab.image.DisplayedImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SearchPublisher;
import org.slf4j.Logger;

/**
 * A service used to display images
 * 
 * @author GroG
 *
 */
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

  public class Fader extends Thread {
    public float alpha = 0.5f;
    public boolean fadeIn;
    public String name;
    public int sleepTime = 300;
    public float fadeIncrement = 0.05f;

    public Fader(String name) {
      this.name = name;
    }
    

    public void run() {
      try {
        JFrame frame = frames.get(name);
        ImageIcon icon = getIcon(name);
        JLabel label = getLabel(name);
        
        // get current BufferedImage
        BufferedImage bi = (BufferedImage) icon.getImage();
        Graphics g = bi.createGraphics();
        Graphics2D g2 = (Graphics2D) g.create();
        
        
        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2.setComposite(composite);
        AffineTransform xform = AffineTransform.getTranslateInstance(bi.getWidth(), bi.getHeight());
        g2.drawRenderedImage(bi, xform);
        label.setIcon(new ImageIcon(bi));
        
        
        for (int i = 0; i < 100; ++i) {
          
     /*     
          
          
          icon.setImage();
          g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
          frame.repaint();

          g2d.drawImage(bi, bi.getWidth(), bi.getHeight(), icon);
          // make new Buffered Image with reduced alpha
          
          
          // set Label with new Icon


          BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
          Graphics g = bi.createGraphics();
          Graphics2D g2d = (Graphics2D) g.create();

          // paint the Icon to the BufferedImage.
          icon.paintIcon(null, g, 0, 0);
          g.dispose();

          Graphics2D g2d = (Graphics2D) g.create();
          g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
          int x = (getWidth() - inImage.getWidth()) / 2;
          int y = (getHeight() - inImage.getHeight()) / 2;
          g2d.drawImage(inImage, x, y, this);

          g2d.setComposite(AlphaComposite.SrcOver.derive(1f - alpha));
          x = (getWidth() - outImage.getWidth()) / 2;
          y = (getHeight() - outImage.getHeight()) / 2;
          g2d.drawImage(outImage, x, y, this);
          g2d.dispose();
*/        frame.setVisible(true);
          sleep(sleepTime);
        }
        g.dispose();
        g2.dispose();
      } catch (Exception e) {
      }
      log.info("fader done");
    }
  }
  
  static BufferedImage deepCopy(BufferedImage bi) {
    ColorModel cm = bi.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = bi.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
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

  transient private JFrame currentFrame;

  boolean defaultAlwaysOnTop = false;

  transient Color defaultColor = Color.LIGHT_GRAY;

  boolean defaultFadeIn = false;

  boolean defaultFullScreen = false;

  boolean defaultMultiFrame = true;

  boolean defaultSystemTray = false;

  boolean defaultTransparentBackground = true;

  transient Map<String, JFrame> frames = new HashMap<String, JFrame>();

  transient JLabel fullscreenImageLabel = null;

  int hOffset = 0;

  transient Cursor lastCursor = null;

  transient Timer timer = null;

  int wOffset = 0;

  boolean autoNumber = false;

  public ImageDisplay(String n, String id) {
    super(n, id);
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

  public void display(String src) throws MalformedURLException, AWTException {
    display(null, src, null, null, null, null, null, null, null);
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
  public void display(String inName, String src, Boolean fullscreen, String bgColorStr, Integer width, Integer height, Double scaling, Float alpha, Boolean fadeIn)
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
    if (image == null)

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

    String name = null;
    if (inName != null) {
      name = inName;
    } else if (autoNumber) {
      name = String.format("%d", frames.size() + 1);
    } else {
      name = src;
    }

    if (defaultMultiFrame) {
      frames.put(name, currentFrame);
    }

    if (fade) {
      Fader fader = new Fader(name);
      // reset the icon with a different alpha depending if fading in or fading
      // out

      // TODO - make seperate fadeIn/fadeOut parameters - currently only support
      // fadeIn
      fader.alpha = 0f;
      fader.fadeIn = fade;
      fader.start();
    } else {

    // make it visible
    currentFrame.setVisible(true);
    }

  }

  // Displays a faded image.
  // @param source = path.
  // @param alpha = Value how much the image is faded float from 0.0 to 1.0.
  public void displayAlpha(String src, float alpha) throws MalformedURLException, AWTException {
    display(null, src, null, null, null, null, null, alpha, null);
  }

  /**
   * Display an image by fading its alpha
   * 
   * @param src
   * @throws MalformedURLException
   * @throws AWTException
   */
  public void displayFadeIn(String src) throws MalformedURLException, AWTException {
    display(null, src, null, null, null, null, null, null, true);
  }

  public void displayFullScreen(String src) throws MalformedURLException, AWTException {
    display(null, src, true, null, null, null, null, null, null);
  }

  public void displayScaled(String src, double scaling) throws MalformedURLException, AWTException {
    display(null, src, null, null, null, null, scaling, null, null);
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
    if (SwingUtilities.isRightMouseButton(e)) {
      JFrame frame = (JFrame) e.getSource();
      // TODO options on hiding or disposing or
      // creating a popup menu etc..
      frame.setVisible(false);
      frame.dispose();
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    log.debug("mouseDragged {}", e);
    if (absMouseX == null) {
      absMouseX = e.getXOnScreen();
      offsetX = e.getX();
    }

    if (absMouseY == null) {
      absMouseY = e.getYOnScreen();
      offsetY = e.getY();
    }

    absLastMouseX = absMouseX;
    absLastMouseY = absMouseY;
    absMouseX = e.getXOnScreen();
    absMouseY = e.getYOnScreen();
    // log.info("current x,y ({},{}) - offsets ({},{}) abs last/new X {}, {} ",
    // currentFrame.getX(), currentFrame.getY(), offsetX, offsetY,
    // absLastMouseX, absMouseX);
    // log.info("new pos X {}", currentFrame.getX() - offsetX - (absLastMouseX -
    // absMouseX));
    // currentFrame.setLocation(currentFrame.getX() - offsetX - (absLastMouseX -
    // absMouseX), currentFrame.getY() - offsetY - (absLastMouseY - absMouseY));
    currentFrame.setLocation(absMouseX - offsetX, absMouseY - offsetY);
    currentFrame.repaint();
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    log.debug("mouseEntered {}", e);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    log.debug("mouseExited {}", e);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    log.debug("mouseMoved {}", e);
  }

  Integer offsetX = null;
  Integer offsetY = null;
  Integer absMouseX = null;
  Integer absMouseY = null;
  Integer absLastMouseX = null;
  Integer absLastMouseY = null;

  @Override
  public void mousePressed(MouseEvent e) {
    log.debug("mousePressed {}", e);
    currentFrame = (JFrame) e.getSource();
    // relative to jframe
    absMouseX = e.getXOnScreen();
    offsetX = e.getX();
    absMouseY = e.getYOnScreen();
    offsetY = e.getY();

    lastCursor = currentFrame.getCursor();
    currentFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));

  }

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
  
  public JLabel getLabel(String name) {
    JFrame frame = frames.get(name);
    JLabel label = (JLabel) frame.getContentPane().getComponent(0);
    return label;
  }
  
  public ImageIcon getIcon(String name) {
    JLabel label =  getLabel(name);
    ImageIcon icon = (ImageIcon) label.getIcon();
    return icon;
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
  
  public void attachSearchPublisher(SearchPublisher search) {
    subscribe(search.getName(), "publishImage");
  }
  
  public String onImage(String urlRef) throws MalformedURLException, AWTException {
    //display(urlRef);
    setAlwaysOnTop(true);
    displayFadeIn(urlRef);
    return urlRef;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");
      // FIXME - get gifs working
      display.setAlwaysOnTop(true);
      // display.displayFadeIn("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/256px-Noto_Emoji_Pie_1f62c.svg.png");
      
      display.displayFadeIn("https://www.ntchosting.com/images/png-compression-example.png");
      
      GoogleSearch search = (GoogleSearch)Runtime.start("google","GoogleSearch");
      List<String> images = search.imageSearch("dogs");
      for (String img : images) {
        display.displayFadeIn(img);
      }
     
      boolean done = true;
      if (done) {
        return;
      }
      
      // display.display("https://media.giphy.com/media/snA2OVsg9sMRW/giphy.gif");
      // display.display("http://www.pngmart.com/files/7/SSL-Download-PNG-Image.png");
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/256px-Noto_Emoji_Pie_1f62c.svg.png");
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/32px-Noto_Emoji_Pie_1f62c.svg.png");
      // display.displayScaled("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/1024px-Noto_Emoji_Pie_1f62c.svg.png",
      // 0.0278f);
      // display.displayFullScreen("http://r.ddmcdn.com/w_830/s_f/o_1/cx_0/cy_220/cw_1255/ch_1255/APL/uploads/2014/11/dog-breed-selector-australian-shepherd.jpg");
      // display.display2("C:\\Users\\grperry\\Desktop\\tenor.gif");
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif");
      display.display("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRl_1J1bmqyQCzmm5rJxQIManVbQJ1xu1emnJHbRmEqOFlv2OteTA");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
