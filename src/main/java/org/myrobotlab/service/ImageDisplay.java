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
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.interfaces.ImageListener;
import org.slf4j.Logger;

public class ImageDisplay extends Service implements ImageListener, MouseListener, ActionListener, MouseMotionListener {

  private static final long serialVersionUID = 1L;

  final static Logger log = LoggerFactory.getLogger(ImageDisplay.class);

  String currentFrame = null;

  // Map<String, ImageDisplayConfig.Display> displays = new HashMap<>();

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
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;
    if (src == null) {
      src = currentFrame;
    }
    Display display = displays.get(src);
    if (display != null) {
      display.frame.dispose();
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
    }
  }

  private Display addFrame(String name, GraphicsDevice gd) {
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

    if (displays.containsKey(name)) {
      return displays.get(name);
    }

    Display data = new Display();

    data.frame = new JFrame(gd.getDefaultConfiguration());
    data.frame.setName(name);
    data.frame.setLayout(new BorderLayout());
    data.panel = new JPanel(new BorderLayout());
    data.panel.setName("panel");
    data.label = new JLabel();
    data.label.setName("label");

    data.panel.add(data.label, BorderLayout.CENTER);
    data.frame.getContentPane().setLayout(new BorderLayout());
    data.frame.getContentPane().add(data.panel, BorderLayout.CENTER);
    data.frame.setUndecorated(true);

    displays.put(name, data);
    currentFrame = name;

    return data;
  }

  public String display(String name, String src) {
    return displayInternal(name, src, null, null, null, null, null, null, null, null, null, null);
  }

  public Display display(String src) {
    return addDisplay(null, src, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  /**
   * Adds a display 
   */
  private Display addDisplay(String name, String src, Integer x, Integer y, Integer width, Integer height, Boolean fullscreen, Boolean alwaysOnTop, Boolean autoscaleExtendsMax,
      String bgColor, Integer screen, Float opacity, Float scale, Boolean visible

  ) {
    Display display = createDisplay(name, src, x, y, width, height, fullscreen, alwaysOnTop, autoscaleExtendsMax, bgColor, screen, opacity, scale, visible);
    displayInternal(display);
    return display;
  }

  /**
   * Create a default display from a large set of options, integrates "default" configuration in the construction
   */
  private Display createDisplay(String name, String src, Integer x, Integer y, Integer width, Integer height, Boolean fullscreen, Boolean alwaysOnTop, Boolean autoscaleExtendsMax,
      String bgColor, Integer screen, Float opacity, Float scale, Boolean visible

  ) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    Display display = new Display();
    display.name = (name != null) ? name : src;
    String stockImg = (c.src != null) ? c.src : getResourceDir() + fs + "mrl_logo.jpg";
    display.src = (src != null) ? src : stockImg;
    display.x = (x != null) ? x : c.x;
    display.y = (y != null) ? y : c.y;
    display.width = (width != null) ? width : c.width;
    display.height = (height != null) ? height : c.height;
    display.fullscreen = (fullscreen != null) ? fullscreen : c.fullscreen;
    display.alwaysOnTop = (alwaysOnTop != null) ? alwaysOnTop : c.alwaysOnTop;
    display.autoscaleExtendsMax = (autoscaleExtendsMax != null) ? autoscaleExtendsMax : c.autoscaleExtendsMax;
    display.bgColor = (bgColor != null) ? bgColor : c.bgColor;
    display.screen = (screen != null) ? screen : c.screen;
    display.scale = (scale != null) ? scale : c.scale;
    display.visible = (visible != null) ? visible : c.visible;

    return display;
  }

  private String displayInternal(String inName, String inSrc, Boolean inFullscreen, Boolean inAlwaysOnTop, String inBgColor, Float inOpacity, Integer inScreen, Float inScale,
      Integer x, Integer y, Integer width, Integer height) {

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

          log.info("display screen {} displaying {}", gd.getIDstring(), name);

          // label & title
          Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

          Display display = null;
          // dynamic display creation ... "or" not
          if (displays.containsKey(name)) {
            log.info("found pre existing display %s", name);
            display = displays.get(name);
            display.label.setIcon(new ImageIcon(ImageIO.read(new File(src))));
            return;
          } else {
            display = addFrame(name, gd);
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

          display.img = image;

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

          display.label.setIcon(new ImageIcon(image));

          if (bgColor != null) {
            Color color = Color.decode(bgColor);
            display.label.setOpaque(true);
            display.label.setBackground(color);
            display.frame.getContentPane().setBackground(color);
          }

          // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

          if (alwaysOnTop != null && alwaysOnTop) {
            display.frame.setAlwaysOnTop(true);
          }

          if (fullscreen != null && fullscreen) {

            // auto scale image
            int displayWidth = gd.getDisplayMode().getWidth();
            int displayHeight = gd.getDisplayMode().getHeight();

            float wRatio = (float) displayWidth / image.getWidth();
            float hRatio = (float) displayHeight / image.getHeight();
            float ratio = (c.autoscaleExtendsMax) ? (wRatio < hRatio) ? hRatio : wRatio : (wRatio > hRatio) ? hRatio : wRatio;

            // if (wDelta) // autoscaling min no crop - autoscale max would crop
            BufferedImage resized = resize(image, (int) (ratio * image.getWidth()), (int) (ratio * image.getHeight()));
            display.img = resized;
            display.label.setSize(resized.getWidth(), resized.getHeight());
            display.label.setIcon(new ImageIcon(resized));

            display.frame.setLocation(displayWidth / 2 - resized.getWidth() / 2, displayHeight / 2 - resized.getHeight() / 2);
            // makes a difference on fullscreen
            display.frame.pack();

            // gd.setFullScreenWindow(frame);
            // vs
            display.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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

            display.label.setSize(imgWidth, imgHeight);
            display.label.setIcon(new ImageIcon(image));

            int imgX = (x != null) ? x : displayWidth / 2 - image.getWidth() / 2;
            int imgY = (y != null) ? y : displayHeight / 2 - image.getHeight() / 2;

            display.frame.setLocation(imgX, imgY);

            // makes a difference on fullscreen
            display.frame.pack();

            // If the component is null, or the
            // GraphicsConfiguration associated with
            // this component is null, the window is placed in the center of the
            // screen.
            // frame.setLocationRelativeTo(null);
          }

          if (opacity != null) {
            display.label.setOpaque(false);
            display.panel.setOpaque(false);
            display.frame.setBackground(new Color(0, 0, 0, opacity));
          }

          display.frame.addMouseListener(imageDisplay);
          display.frame.addMouseMotionListener(imageDisplay);
          display.frame.setVisible(true);
        } catch (Exception e) {
          log.error("display threw", e);
        }

      }
    });
    return name;
  }

  private String displayInternal(final Display display) {

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

    // final String name = (inName != null) ? inName : inSrc;
    // final Boolean fullscreen = (inFullscreen != null) ? inFullscreen :
    // c.fullscreen;
    // final Boolean alwaysOnTop = (inAlwaysOnTop != null) ? inAlwaysOnTop :
    // c.alwaysOnTop;
    //
    // final String src = (inSrc != null) ? inSrc : getResourceDir() + fs +
    // "mrl_logo.jpg";
    //
    // String bgTemp = (inBgColor != null) ? inBgColor : c.bgColor;
    //
    // if (bgTemp != null && !bgTemp.startsWith("#")) {
    // bgTemp = "#" + bgTemp;
    // }
    //
    // final String bgColor = bgTemp;
    // final Float opacity = (inOpacity != null) ? inOpacity : c.opacity;
    // final Integer screen = (inScreen != null) ? inScreen : c.screen;
    // final Float scale = (inScale != null) ? inScale : c.scale;

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        try {

          // Display disp = new Display();
          // disp.x = x;
          // disp.y = y;
          // disp.width = width;
          // disp.height = height;
          // disp.scale = scale;
          // disp.opacity = opacity;
          // disp.screen = screen;
          // disp.bgColor = bgColor;
          // disp.alwaysOnTop = alwaysOnTop;
          // disp.fullscreen = fullscreen;
          // disp.src = src;

          if (gs == null) {
            ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            gs = ge.getScreenDevices();
          }

          GraphicsDevice gd = null;
          if (display.screen != null && display.screen <= gs.length) {
            gd = gs[display.screen];
          } else {
            gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
          }

          log.info("display screen {} displaying {}", gd.getIDstring(), display.name);

          // label & title
          Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

          // Display display = null;
          // dynamic display creation ... "or" not
          // if (displays.containsKey(display.name)) {
          // log.info("found pre existing display %s", display.name);
          // display = displays.get(display.name);
          // return;
          // } else {
          // display = addFrame(display.name, gd);
          // }

          display.label.setIcon(new ImageIcon(ImageIO.read(new File(display.src))));

          log.info("Loading image: ");
          BufferedImage image = null;
          if (display.src.startsWith("http://") || (display.src.startsWith("https://"))) {
            String cacheFile = cacheDir + fs + display.src.replace("/", "_");
            File check = new File(cacheFile);
            if (check.exists()) {
              image = ImageIO.read(new File(cacheFile));
            } else {
              log.info("from url...");
              byte[] bytes = Http.get(display.src);
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
            image = ImageIO.read(new File(display.src));
            // image = ImageIO.read(new URL("file://src)); won't work requires
            // absolute path :(
          }

          display.img = image;

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

          display.label.setIcon(new ImageIcon(image));

          if (display.bgColor != null) {
            Color color = Color.decode(display.bgColor);
            display.label.setOpaque(true);
            display.label.setBackground(color);
            display.frame.getContentPane().setBackground(color);
          }

          // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

          if (display.alwaysOnTop != null && display.alwaysOnTop) {
            display.frame.setAlwaysOnTop(true);
          }

          if (display.fullscreen != null && display.fullscreen) {

            // auto scale image
            int displayWidth = gd.getDisplayMode().getWidth();
            int displayHeight = gd.getDisplayMode().getHeight();

            float wRatio = (float) displayWidth / image.getWidth();
            float hRatio = (float) displayHeight / image.getHeight();
            float ratio = (c.autoscaleExtendsMax) ? (wRatio < hRatio) ? hRatio : wRatio : (wRatio > hRatio) ? hRatio : wRatio;

            // if (wDelta) // autoscaling min no crop - autoscale max would crop
            BufferedImage resized = resize(image, (int) (ratio * image.getWidth()), (int) (ratio * image.getHeight()));
            display.img = resized;
            display.label.setSize(resized.getWidth(), resized.getHeight());
            display.label.setIcon(new ImageIcon(resized));

            display.frame.setLocation(displayWidth / 2 - resized.getWidth() / 2, displayHeight / 2 - resized.getHeight() / 2);
            // makes a difference on fullscreen
            display.frame.pack();

            // gd.setFullScreenWindow(frame);
            // vs
            display.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            // frame.setLocationRelativeTo(null);

          } else {

            int displayWidth = gd.getDisplayMode().getWidth();
            int displayHeight = gd.getDisplayMode().getHeight();

            if (display.scale != null) {
              // FIXME - check this
              image = resize(image, (int) (display.scale * image.getWidth()), (int) (display.scale * image.getHeight()));
            } else if (display.width != null && display.height != null) {
              image = resize(image, display.width, display.height);
            }

            int imgWidth = (display.width != null) ? display.width : image.getWidth();
            int imgHeight = (display.height != null) ? display.height : image.getHeight();

            display.label.setSize(imgWidth, imgHeight);
            display.label.setIcon(new ImageIcon(image));

            int imgX = (display.x != null) ? display.x : displayWidth / 2 - image.getWidth() / 2;
            int imgY = (display.y != null) ? display.y : displayHeight / 2 - image.getHeight() / 2;

            display.frame.setLocation(imgX, imgY);

            // makes a difference on fullscreen
            display.frame.pack();

            // If the component is null, or the
            // GraphicsConfiguration associated with
            // this component is null, the window is placed in the center of the
            // screen.
            // frame.setLocationRelativeTo(null);
          }

          if (display.opacity != null) {
            display.label.setOpaque(false);
            display.panel.setOpaque(false);
            display.frame.setBackground(new Color(0, 0, 0, display.opacity));
          }

          display.frame.addMouseListener(imageDisplay);
          display.frame.addMouseMotionListener(imageDisplay);
          display.frame.setVisible(true);
        } catch (Exception e) {
          log.error("display threw", e);
        }

      }
    });
    return display.name;
  }

  private BufferedImage resize(BufferedImage before, int width, int height) {

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
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

    for (Display display : displays.values()) {
      if (display != null) {
        display.frame.dispose();
      }
    }
    currentFrame = null;
    displays.clear();
  }

  public String close() {
    return close(currentFrame);
  }

  public String close(String src) {
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

    Display display = displays.get(src);
    if (display != null) {
      display.frame.dispose();
      displays.remove(src);
      return src;
    }
    return null;
  }

  public String displayFullScreen(String name, String src) {
    return displayInternal(name, src, true, null, null, null, null, null, null, null, null, null);
  }

  public String displayFullScreen(String src) {
    return displayInternal(null, src, true, null, null, null, null, null, null, null, null, null);
  }

  public String displayScaled(String src, float scale) {
    return displayInternal(null, src, true, null, null, null, null, scale, null, null, null, null);
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    ImageDisplayConfig config = (ImageDisplayConfig) c;
    for (String displayName : config.displays.keySet()) {
      close(displayName);
      Display disp = config.displays.get(displayName);
      displayInternal(displayName, null, null, null, null, null, null, null, disp.x, disp.y, disp.width, disp.height);
    }
    return config;
  }

  @Override
  public ServiceConfig getConfig() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    // don't need to do anything
    return c;
  }

  @Override
  public void onImage(ImageData img) {
    display(img.name, img.src);
  }

  public void resize(String name, int width, int height) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

        if (displays.containsKey(name)) {
          Display display = displays.get(name);
          display.img = resize(display.img, width, height);
          display.label.setIcon(new ImageIcon(display.img));
          display.label.invalidate();
          display.frame.pack();
        }
      }
    });
  }

  public void move(String name, int x, int y) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

        if (displays.containsKey(name)) {
          Display display = displays.get(name);
          display.frame.setLocation(x, y);
        }
      }
    });
  }

  public void setVisible(String name, boolean b) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

        if (displays.containsKey(name)) {
          Display display = displays.get(name);
          display.frame.setVisible(b);
        }
      }
    });
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("python", "Python");
      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      // display.setFullScreen(true);
      // display.setColor("FF0000");
      display.display("monkeys", "https://upload.wikimedia.org/wikipedia/commons/e/e8/Gabriel_Cornelius_von_Max%2C_1840-1915%2C_Monkeys_as_Judges_of_Art%2C_1889.jpg");

      boolean done = true;
      if (done) {
        return;
      }

      display.displayFullScreen("robot", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/1280px-FANUC_6-axis_welding_robots.jpg");
      display.display("inmoov", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/InMoov_Wheel_1.jpg/220px-InMoov_Wheel_1.jpg");
      display.display("mrl", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/VirtualInMoov.jpg/220px-VirtualInMoov.jpg");

      for (int i = 0; i < 100; ++i) {
        display.move("monkeys", 20 + i, 20 + i);
        Service.sleep(50);
      }

      display.resize("monkeys", 200, 200);
      Service.sleep(1000);
      display.move("monkeys", 20, 20);
      Service.sleep(1000);
      display.close("monkeys");
      Service.sleep(1000);
      display.close("monkeys");
      display.close("robot");

      int x0 = 500, y0 = 500;
      int r = 300;
      double x, y = 0;

      for (double t = 0; t < 4 * Math.PI; t += 1) {
        x = r * Math.cos(t) + x0;
        y = r * Math.sin(t) + y0;
        display.move("inmoov", (int) x, (int) y);
        Service.sleep(100);
      }

      // get images - display
      GoogleSearch google = (GoogleSearch) Runtime.start("google", "GoogleSearch");
      List<String> images = google.imageSearch("monkey");
      for (String img : images) {
        display.displayFullScreen(img);
        // display.display(img);
        Service.sleep(1000);
      }

      // attach pub/sub display style
      display.setFullScreen(true);
      display.setAutoscaleExtendsMax(true);
      Wikipedia wikipedia = (Wikipedia) Runtime.start("wikipedia", "Wikipedia");
      wikipedia.attach(display);
      images = wikipedia.imageSearch("bear");

      display.setAutoscaleExtendsMax(false);

      display.displayFullScreen("data/Emoji/512px/U+1F47D.png");

      display.display("data/Emoji/512px/U+1F47D.png");

      display.display("data/Emoji/512px/U+1F47D.png");

      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/256px-Noto_Emoji_Pie_1f62c.svg.png");
      display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Noto_Emoji_Pie_1f62c.svg/32px-Noto_Emoji_Pie_1f62c.svg.png");
      display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif");
      display.display("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRl_1J1bmqyQCzmm5rJxQIManVbQJ1xu1emnJHbRmEqOFlv2OteTA");

      log.info("done");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public boolean setAutoscaleExtendsMax(boolean b) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.autoscaleExtendsMax = b;
    return b;
  }

}
