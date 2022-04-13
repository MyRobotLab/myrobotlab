package org.myrobotlab.service;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.config.ImageDisplayConfig;
import org.myrobotlab.service.config.ImageDisplayConfig.Display;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.interfaces.ImageListener;
import org.slf4j.Logger;

public class ImageDisplay extends Service implements ImageListener, MouseListener, ActionListener, MouseMotionListener {

  private static final long serialVersionUID = 1L;

  final static Logger log = LoggerFactory.getLogger(ImageDisplay.class);

  String currentFrame = null;

  private transient Map<String, JFrame> displays = new HashMap<>();

  Integer offsetX = null;
  Integer offsetY = null;
  Integer absMouseX = null;
  Integer absMouseY = null;
  Integer absLastMouseX = null;
  Integer absLastMouseY = null;

  String cacheDir = getDataDir() + fs + "cache";

  transient GraphicsEnvironment ge = null;

  transient GraphicsDevice[] gs = null;

  public ImageDisplay(String name, String inId) {
    super(name, inId);
    File file = new File(cacheDir);
    file.mkdirs();
  }

  public void setAlwaysOnTop(boolean b) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.alwaysOnTop = b;
  }

  public void setColor(String color) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.bgColor = color;
  }

  public void setTransparent() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.bgColor = null;
  }

  public void setFullScreen(boolean b) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.fullscreen = b;
  }

  public void enable() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.enabled = true;
  }

  public void disable() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.enabled = false;
  }

  @Deprecated /* opacity not supported this way */
  public void exitFS() throws MalformedURLException, AWTException {
    exitFullScreen(null);
  }

  public void exitFullScreen() {
    exitFullScreen(null);
  }

  public void exitFullScreen(String src) {
    if (src == null) {
      src = currentFrame;
    }
    JFrame frame = displays.get(src);
    if (frame != null) {
      frame.dispose();
    }

    displays.remove(src);
    display(src);
  }

  @Deprecated /* opacity not supported this way */
  public void displayFullScreen(String src, float opacity) throws MalformedURLException, AWTException {
    displayFullScreen(src);
  }

  @Deprecated /* opacity not supported this way */
  public void display(String src, float opacity) throws MalformedURLException, AWTException {
    display(src);
  }

  @Deprecated /* opacity not supported this way */
  public void displayScaled(String src, float opacity, float scale) throws MalformedURLException, AWTException {
    displayScaled(src, scale);
  }

  @Deprecated /* no longer supported */
  public void displayFadeIn(String src) throws MalformedURLException, AWTException {
    display(src);
  }

  @Override
  public void startService() {
    super.startService();
    if (GraphicsEnvironment.isHeadless()) {
      log.warn("in headless mode - %s will not display images", getName());
      return;
    } else {

      // ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      // gs = ge.getScreenDevices();
      // gd =
      // GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }
  }

  private JFrame addFrame(String name, GraphicsDevice gd) {

    if (displays.containsKey(name)) {
      return displays.get(name);
    }

    JFrame frame = new JFrame(gd.getDefaultConfiguration());
    frame.setName(name);
    frame.setLayout(new BorderLayout());
    JPanel panel = new JPanel(new BorderLayout());
    panel.setName("panel");
    JLabel label = new JLabel();
    label.setName("label");

    panel.add(label, BorderLayout.CENTER);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(panel, BorderLayout.CENTER);
    frame.setUndecorated(true);

    displays.put(name, frame);
    currentFrame = name;

    return frame;
  }

  public String display(String name, String src) {
    return display(name, src, null, null, null, null, null, null, null, null, null, null);
  }

  public String display(String src) {
    return display(null, src, null, null, null, null, null, null, null, null, null, null);
  }

  public String display(String inName, String inSrc, Boolean inFullscreen, Boolean inAlwaysOnTop, String inBgColor, Float inOpacity, Integer inScreen, Float inScale, Integer x,
      Integer y, Integer width, Integer height) {

    ImageDisplayConfig c = (ImageDisplayConfig) config;

    final ImageDisplay imageDisplay = this;

    

    if (!c.enabled) {
      log.info("not currently enabled");
      return null;
    }

    if (GraphicsEnvironment.isHeadless()) {
      log.warn("in headless mode - %s will not display images", getName());
      return null;
    }

    // use parameters or set from config defaults

    // if (src == null) {
    // error("cannot display null");
    // return null;
    // }

    final String name = (inName != null) ? inName : inSrc;
    final Boolean fullscreen = (inFullscreen != null) ? inFullscreen : c.fullscreen;
    final Boolean alwaysOnTop = (inAlwaysOnTop != null) ? inAlwaysOnTop : c.alwaysOnTop;

    final String src = (inSrc != null) ? inSrc : getResourceDir() + fs + "mrl_logo.jpg";

    String bgTemp = (inBgColor != null) ? inBgColor : c.bgColor;

    if (bgTemp != null && !bgTemp.startsWith("#")) {
      bgTemp = "#" + bgTemp;
    }

    final String bgColor = bgTemp;
    final Float opacity = (inOpacity != null) ? inOpacity : c.opacity;
    final Integer screen = (inScreen != null) ? inScreen : c.screen;
    final Float scale = (inScale != null) ? inScale : c.scale;



    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        try {
          
          
          Display disp = new Display();
          disp.x = x;
          disp.y = y;
          disp.width = width;
          disp.height = height;
          disp.scale = scale;
          disp.opacity = opacity;
          disp.screen = screen;
          disp.bgColor = bgColor;
          disp.alwaysOnTop = alwaysOnTop;
          disp.fullscreen = fullscreen;
          disp.src = src;
          

          if (gs == null) {
            ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            gs = ge.getScreenDevices();
          }

          GraphicsDevice gd = null;
          if (screen != null && screen <= gs.length) {
            gd = gs[screen];
          } else {
            gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
          }

          log.info("going to display on %s screen device", gd.getIDstring());

          // label & title
          JFrame frame = null;
          JPanel panel = null;
          JLabel label = null;

          // dynamic display creation ... "or" not
          if (displays.containsKey(name)) {
            log.info("found pre existing display %s", name);
            frame = displays.get(name);
            panel = (JPanel) frame.getContentPane().getComponent(0);
            label = (JLabel) panel.getComponent(0);
            label.setIcon(new ImageIcon(ImageIO.read(new File(src))));
            return;
          } else {
            frame = addFrame(name, gd);
            panel = (JPanel) frame.getContentPane().getComponent(0);
            label = (JLabel) panel.getComponent(0);
          }

          log.info("Loading image: ");
          BufferedImage image = null;
          if (src.startsWith("http://") || (src.startsWith("https://"))) {
            String cacheFile = cacheDir + fs + src.replace("/", "_");
            File check = new File(cacheFile);
            if (check.exists()) {
              image = ImageIO.read(new File(cacheFile));
            } else {
              log.info("from url...");
              byte[] bytes = Http.get(src);
              if (bytes != null) {
                ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
                image = ImageIO.read(bios);
                // save cache
                FileIO.toFile(cacheFile, bytes);
              }
            }
          } else {
            log.info("from file...");
            // DO I NEED THIS will new URL("data/blah.jpg") work ?
            image = ImageIO.read(new File(src));
            // image = ImageIO.read(new URL("file://src)); won't work requires
            // absolute path :(
          }

          // TODO - make better / don't use setImageAutoSize (very bad
          // algorithm)
          if (SystemTray.isSupported()) {
            log.info("SystemTray is supported");
            SystemTray tray = SystemTray.getSystemTray();
            // Dimension trayIconSize = tray.getTrayIconSize();

            TrayIcon trayIcon = new TrayIcon(image);
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
          }

          label.setIcon(new ImageIcon(image));

          if (bgColor != null) {
            Color color = Color.decode(bgColor);
            label.setOpaque(true);
            label.setBackground(color);
            frame.getContentPane().setBackground(color);
          }

          // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

          if (alwaysOnTop != null && alwaysOnTop) {
            frame.setAlwaysOnTop(true);
          }

          if (fullscreen != null && fullscreen) {

            // auto scale image
            int displayWidth = gd.getDisplayMode().getWidth();
            int displayHeight = gd.getDisplayMode().getHeight();

            float wRatio = (float) displayWidth / image.getWidth();
            float hRatio = (float) displayHeight / image.getHeight();
            float ratio = (wRatio > hRatio) ? hRatio : wRatio;

            // if (wDelta) // autoscaling min no crop - autoscale max would crop
            BufferedImage resized = resize(image, (int) (ratio * image.getWidth()), (int) (ratio * image.getHeight()));

            label.setSize(resized.getWidth(), resized.getHeight());
            label.setIcon(new ImageIcon(resized));

            frame.setLocation(displayWidth / 2 - resized.getWidth() / 2, displayHeight / 2 - resized.getHeight() / 2);
            // makes a difference on fullscreen
            frame.pack();

            // gd.setFullScreenWindow(frame);
            // vs
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            // frame.setLocationRelativeTo(null);

          } else {

            int displayWidth = gd.getDisplayMode().getWidth();
            int displayHeight = gd.getDisplayMode().getHeight();

            if (scale != null) {
              // FIXME - check this
              image = resize(image, (int) (scale * image.getWidth()), (int) (scale * image.getHeight()));
            } else if (width != null && height != null) {
              image = resize(image, width, height);
            }
            
            int imgWidth = (width != null) ? width : image.getWidth();
            int imgHeight = (height != null) ? height : image.getHeight();

            label.setSize(imgWidth, imgHeight);
            label.setIcon(new ImageIcon(image));

            int imgX = (x != null) ? x : displayWidth / 2 - image.getWidth() / 2;
            int imgY = (y != null) ? y : displayHeight / 2 - image.getHeight() / 2;

            frame.setLocation(imgX, imgY);

            // makes a difference on fullscreen
            frame.pack();

            // If the component is null, or the
            // GraphicsConfiguration associated with
            // this component is null, the window is placed in the center of the
            // screen.
            // frame.setLocationRelativeTo(null);
          }

          if (opacity != null) {
            label.setOpaque(false);
            panel.setOpaque(false);
            frame.setBackground(new Color(0, 0, 0, opacity));
          }

          frame.addMouseListener(imageDisplay);
          frame.addMouseMotionListener(imageDisplay);
          frame.setVisible(true);
        } catch (Exception e) {
          log.error("display threw", e);
        }

      }
    });
    return name;
  }

  BufferedImage resize(BufferedImage before, int width, int height) {

    // Graphics2D g2d = bImage.createGraphics();
    // g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
    // RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );

    int w = before.getWidth();
    int h = before.getHeight();
    BufferedImage after = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    AffineTransform at = new AffineTransform();
    at.scale((float) width / w, (float) height / h);
    AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
    after = scaleOp.filter(before, after);

    return after;
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    JFrame frame = (JFrame) e.getSource();
    // log.debug("mouseDragged {}", frame.getName());
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
    frame.setLocation(absMouseX - offsetX, absMouseY - offsetY);
    frame.repaint();
  }

  @Override
  public void mouseMoved(MouseEvent arg0) {
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // log.info("mouseClicked {}", e);
    if (SwingUtilities.isRightMouseButton(e)) {
      JFrame frame = (JFrame) e.getSource();
      // TODO options on hiding or disposing or
      // creating a popup menu etc..
      frame.setVisible(false);
      frame.dispose();
    }
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
  }

  @Override
  public void mousePressed(MouseEvent arg0) {
  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
  }

  public void closeAll() {
    for (JFrame frame : displays.values()) {
      if (frame != null) {
        frame.dispose();
      }
    }
    currentFrame = null;
    displays.clear();
  }

  public String close() {
    return close(currentFrame);
  }

  public String close(String src) {
    JFrame frame = displays.get(src);
    if (frame != null) {
      frame.dispose();
      displays.remove(src);
      return src;
    }
    return null;
  }

  public String displayFullScreen(String name, String src) {
    return display(name, src, true, null, null, null, null, null, null, null, null, null);
  }

  public String displayFullScreen(String src) {
    return display(null, src, true, null, null, null, null, null, null, null, null, null);
  }

  public String displayScaled(String src, float scale) {
    return display(null, src, true, null, null, null, null, scale, null, null, null, null);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");
      // display.setFullScreen(true);
      // display.setColor("FF0000");

      display.displayFullScreen("data/Emoji/512px/U+1F47D.png");
      display.display("data/Emoji/512px/U+1F47D.png");

      GoogleSearch search = (GoogleSearch) Runtime.start("google", "GoogleSearch");
      List<String> images = search.imageSearch("tiger");
      for (String img : images) {
        display.displayFullScreen(img);
        display.display(img);
        log.info("here");
      }

      display.displayFullScreen("data/Emoji/512px/U+1F47D.png");

      display.display("data/Emoji/512px/U+1F47D.png");

      display.display("data/Emoji/512px/U+1F47D.png");

      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/256px-Noto_Emoji_Pie_1f62c.svg.png");
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/32px-Noto_Emoji_Pie_1f62c.svg.png");
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif");
      display.display("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRl_1J1bmqyQCzmm5rJxQIManVbQJ1xu1emnJHbRmEqOFlv2OteTA");

      log.info("done");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    ImageDisplayConfig config = (ImageDisplayConfig) c;
    for (String displayName : config.displays.keySet()) {
      close(displayName);
      Display disp = config.displays.get(displayName);
      display(displayName, null, null, null, null, null, null, null, disp.x, disp.y, disp.width, disp.height);
    }
    return config;
  }

  @Override
  public ServiceConfig getConfig() {

    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.displays.clear();

    for (String d : displays.keySet()) {
      Display display = new Display();
      JFrame frame = displays.get(d);
      display.x = frame.getX();
      display.y = frame.getY();
      display.width = frame.getWidth();
      display.height = frame.getHeight();
      c.displays.put(d, display);
    }

    return c;
  }

  @Override
  public void onImage(ImageData img) {
    display(img.name, img.src);
  }

}
