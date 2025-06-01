package org.myrobotlab.service;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

//import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.config.ImageDisplayConfig;
import org.myrobotlab.service.config.ImageDisplayConfig.Display;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.slf4j.Logger;

public class ImageDisplay extends Service<ImageDisplayConfig> implements ImageListener, MouseListener, ActionListener, MouseMotionListener {

  final static Logger log = LoggerFactory.getLogger(ImageDisplay.class);

  private static final long serialVersionUID = 1L;

  Integer absLastMouseX = null;

  Integer absLastMouseY = null;

  Integer absMouseX = null;

  Integer absMouseY = null;

  String cacheDir = getDataDir() + fs + "cache";

  String currentDisplay = null;

  transient GraphicsEnvironment ge = null;

  transient GraphicsDevice[] gs = null;

  Integer offsetX = null;

  Integer offsetY = null;

  transient ImageDisplay self = null;

  public ImageDisplay(String name, String inId) {
    super(name, inId);
    File file = new File(cacheDir);
    file.mkdirs();
    self = this;
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
  }

  @Override
  public ImageDisplayConfig apply(ImageDisplayConfig c) {
    super.apply(c);
    if (c.displays != null) {
      for (String displayName : c.displays.keySet()) {
        close(displayName);
        displayInternal(displayName);
      }
    }
    return c;
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof ImagePublisher) {
      attachImagePublisher(attachable.getName());
    } else {
      error("don't know how to attach a %s", attachable.getName());
    }
  }

  public String close() {
    return close(currentDisplay);
  }

  public String close(String name) {
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

    Display display = displays.get(name);
    if (display != null) {
      if (display.frame != null) {
        display.frame.setVisible(false);
        display.frame.dispose();
        display.frame = null;
      }
      display.name = name;
      // displays.remove(src);
      return name;
    }
    return null;
  }

  public void closeAll() {
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

    for (Display display : displays.values()) {
      close(display.name);
    }
    currentDisplay = null;
    displays.clear();
  }

  public void reset() {
    closeAll();
    config = new ImageDisplayConfig();
  }

  public void disable() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.enabled = false;
  }

  public Display display(String src) {
    Display display = getDisplay(null);
    display.src = src;
    displayInternal(display.name);
    return display;
  }

  public void display(String src, float opacity) {
    Display display = getDisplay(null);
    display.src = src;
    display.fullscreen = false;
    display.opacity = opacity;
    displayInternal(display.name);
  }

  public String display(String name, String src) {
    Display display = getDisplay(name);
    display.src = src;
    display.name = name;
    displayInternal(name);
    return name;
  }

  @Deprecated /* no longer supported */
  public void displayFadeIn(String src) {
    display(src);
  }

  public String displayFullScreen(String src) {
    Display display = getDisplay(null);
    display.src = src;
    display.fullscreen = true;
    displayInternal(display.name);
    return display.name;
  }

  public void displayFullScreen(String src, float opacity) {
    Display display = getDisplay(null);
    display.src = src;
    display.fullscreen = true;
    display.opacity = opacity;
    displayInternal(display.name);
  }

  public String displayFullScreen(String name, String src) {
    Display display = getDisplay(name);
    display.name = name;
    display.src = src;
    display.fullscreen = true;
    displayInternal(display.name);
    return display.name;
  }

  private String displayInternal(final String name) {

    ImageDisplayConfig c = (ImageDisplayConfig) config;

    if (!c.displays.containsKey(name)) {
      error("could not find %s display", name);
      return name;
    }

    if (!c.enabled) {
      log.info("not currently enabled");
      return name;
    }

    if (GraphicsEnvironment.isHeadless()) {
      log.warn("in headless mode - %s will not display images", getName());
      return name;
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        try {

          if (gs == null) {
            ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            gs = ge.getScreenDevices();
          }

          Display display = c.displays.get(name);
          if (display == null) {
            error("null display for %s", name);
            return;
          }

          // creating the swing components if necessary
          if (display.frame == null) {
            initFrame(display);
          }

          int displayWidth = display.gd.getDisplayMode().getWidth();
          int displayHeight = display.gd.getDisplayMode().getHeight();

          log.info("display screen {} displaying {}", display.screen, display.name);
          log.info("Loading image: ");

          // get final uri/url for file
          URL imageUrl = null;

          if (display.src == null) {
            error("could not display null image");
            display.src = getResourceDir() + fs + "mrl_logo.jpg";
          }

          if (display.src.startsWith("http://") || (display.src.startsWith("https://"))) {
            String cacheFile = cacheDir + fs + display.src.replace("/", "_");
            File check = new File(cacheFile);
            if (!check.exists()) {
              byte[] bytes = Http.get(display.src);
              if (bytes != null) {
                // save cache
                FileIO.toFile(cacheFile, bytes);
              } else {
                error("could not download %s", display.src);
                check = new File(getResourceDir() + fs + "mrl_logo.jpg");
              }
            }
            imageUrl = check.toURI().toURL();
          } else {
            File check = new File(display.src);
            if (!check.exists()) {
              error("%s does not exist", display.src);
              display.src = getResourceDir() + fs + "mrl_logo.jpg";
            }
            imageUrl = new File(display.src).toURI().toURL();
          }

          display.imageIcon = new ImageIcon(imageUrl);
          display.label.setIcon(display.imageIcon);
          // display.label.setIcon(new ImageIcon(image));

          // TODO - make better / don't use setImageAutoSize (very bad
          // algorithm)
          /** <pre> No real use, and doesn't remove
          if (SystemTray.isSupported()) {
            log.info("SystemTray is supported");
            SystemTray tray = SystemTray.getSystemTray();
            // Dimension trayIconSize = tray.getTrayIconSize();

            TrayIcon trayIcon = new TrayIcon(display.imageIcon.getImage());
            trayIcon.setImageAutoSize(true);

            tray.add(trayIcon);
          }
          </pre>
          */

          if (display.bgColor != null) {
            Color color = Color.decode(display.bgColor);
            display.label.setOpaque(true);
            display.label.setBackground(color);
            display.panel.setBackground(color); // <- this one is the important
                                                // one
            display.frame.getContentPane().setBackground(color);
          }

          if (display.alwaysOnTop != null && display.alwaysOnTop) {
            display.frame.setAlwaysOnTop(true);
          }

          if (display.fullscreen != null && display.fullscreen) {

            // auto scale image

            float wRatio = (float) displayWidth / display.imageIcon.getIconWidth();
            float hRatio = (float) displayHeight / display.imageIcon.getIconHeight();
            float ratio = (c.autoscaleExtendsMax) ? (wRatio < hRatio) ? hRatio : wRatio : (wRatio > hRatio) ? hRatio : wRatio;

            int resizedWidth = (int) (ratio * display.imageIcon.getIconWidth());
            int resizedHeight = (int) (ratio * display.imageIcon.getIconHeight());

            Image image = display.imageIcon.getImage().getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_DEFAULT);
            display.imageIcon.setImage(image);

            // center it
            display.frame.setLocation(displayWidth / 2 - resizedWidth / 2, displayHeight / 2 - resizedHeight / 2);
            // makes a difference on fullscreen
            display.frame.pack();

            // gd.setFullScreenWindow(frame);
            // vs
            display.frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            // frame.setLocationRelativeTo(null);

          } else if ((display.width != null && display.height != null) || display.scale != null) {

            // FIXME - "IF" SCALED THEN SETICON(ICON) !!!

            Integer resizedWidth = null;
            Integer resizedHeight = null;

            if (display.scale != null) {
              // FIXME - check this
              resizedWidth = (int) (display.scale * display.imageIcon.getIconWidth());
              resizedHeight = (int) (display.scale * display.imageIcon.getIconHeight());

            } else if (display.width != null && display.height != null) {
              resizedWidth = display.width;
              resizedHeight = display.height;
            }

            display.imageIcon.getImage().getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_DEFAULT);
          }

          int imgX = (display.x != null) ? display.x : displayWidth / 2 - display.imageIcon.getIconWidth() / 2;
          int imgY = (display.y != null) ? display.y : displayHeight / 2 - display.imageIcon.getIconHeight() / 2;

          display.frame.setLocation(imgX, imgY);

          // makes a difference on fullscreen
          display.frame.pack();

          if (display.opacity != null) {
            display.label.setOpaque(false);
            display.panel.setOpaque(false);
            display.frame.setOpacity(display.opacity);
            display.frame.setBackground(new Color(0, 0, 0, display.opacity));
          }

          display.frame.setVisible(true);
        } catch (Exception e) {
          error("display error %s", e.getMessage());
          log.error("display threw", e);
        }

      }

    });
    return name;
  }

  public String displayScaled(String src, float scale) {
    Display display = getDisplay(null);
    display.src = src;
    display.scale = scale;
    displayInternal(display.name);
    return display.name;
  }

  public void displayScaled(String src, float opacity, float scale) {
    Display display = getDisplay(null);
    display.src = src;
    display.opacity = opacity;
    display.scale = scale;
    displayInternal(display.name);
  }

  public void enable() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.enabled = true;
  }

  // no longer interested in managing BufferedImages :(
  // private BufferedImage resize(BufferedImage before, int width, int height) {
  //
  // int w = before.getWidth();
  // int h = before.getHeight();
  // BufferedImage after = new BufferedImage(width, height,
  // BufferedImage.TYPE_INT_ARGB);
  // AffineTransform at = new AffineTransform();
  // at.scale((float) width / w, (float) height / h);
  // AffineTransformOp scaleOp = new AffineTransformOp(at,
  // AffineTransformOp.TYPE_BILINEAR);
  // after = scaleOp.filter(before, after);
  //
  // return after;
  // }

  @Deprecated
  public void exitFS() {
    exitFullScreen(null);
  }

  public void exitFullScreen() {
    exitFullScreen(null);
  }

  public void exitFullScreen(String name) {
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;
    if (name == null) {
      name = currentDisplay;
    }
    Display display = displays.get(name);
    if (display != null) {
      display.frame.dispose();
    }

    displays.remove(name);
    display(name);
  }

  /**
   * if the display currently get it, if it doesn't create one with default
   * values specified in config
   * 
   * @return
   */
  private Display getDisplay(String name) {

    if (name == null) {
      name = "default";
    }

    ImageDisplayConfig c = (ImageDisplayConfig) config;

    if (c.displays.containsKey(name)) {
      return c.displays.get(name);
    }

    // create a default display from config
    Display display = new Display();

    display.name = name;
    display.src = c.src;
    display.x = c.x;
    display.y = c.y;
    display.width = c.width;
    display.height = c.height;

    display.fullscreen = c.fullscreen;
    display.alwaysOnTop = c.alwaysOnTop;
    display.autoscaleExtendsMax = c.autoscaleExtendsMax;
    display.bgColor = c.bgColor;
    display.screen = c.screen;
    display.opacity = c.opacity;
    display.scale = c.scale;
    display.visible = c.visible;

    c.displays.put(display.name, display);

    return display;
  }

  private void initFrame(Display display) {
    if (display.frame != null) {
      display.frame.setVisible(false);
      display.frame.dispose();
    }

    if (display.screen != null && display.screen <= gs.length) {
      display.gd = gs[display.screen];
    } else {
      display.gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }

    display.frame = new JFrame(display.gd.getDefaultConfiguration());
    display.frame.setName(display.name);

    // display.panel = new JPanel(new BorderLayout());
    display.panel = new JPanel();
    FlowLayout fl = (FlowLayout) display.panel.getLayout();
    fl.setVgap(0);
    fl.setHgap(0);

    display.panel.setName("panel");
    display.label = new JLabel();
    display.label.setName("label");

    // display.panel.add(display.label, BorderLayout.CENTER);
    display.panel.add(display.label);

    display.frame.getContentPane().add(display.panel);
    display.frame.setUndecorated(true);

    display.frame.addMouseListener(self);
    display.frame.addMouseMotionListener(self);

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
  public void mouseEntered(MouseEvent arg0) {
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
  }

  @Override
  public void mouseMoved(MouseEvent arg0) {
  }

  @Override
  public void mousePressed(MouseEvent arg0) {
  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
  }

  public void move(String name, int x, int y) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

        if (displays.containsKey(name)) {
          Display display = displays.get(name);
          display.frame.setLocation(x, y);
          display.x = x;
          display.y = y;
        }
      }
    });
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
          initFrame(display);

          Image image = display.imageIcon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
          display.label.setIcon(new ImageIcon(image));
          display.frame.pack();
          display.width = width;
          display.height = height;
          if (display.x != null && display.y != null) {
            display.frame.setLocation(display.x, display.y);
          }
          display.frame.setVisible(true);
        }
      }
    });
  }

  public void setAlwaysOnTop(boolean b) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.alwaysOnTop = b;
  }

  public boolean setAutoscaleExtendsMax(boolean b) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.autoscaleExtendsMax = b;
    return b;
  }

  public void setColor(String color) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.bgColor = color;
  }

  public void setDimension(Integer width, Integer height) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.width = width;
    c.height = height;
  }

  public void setFullScreen(boolean b) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.fullscreen = b;
  }

  public void setLocation(Integer x, Integer y) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.x = x;
    c.y = y;
  }

  public Integer setScreen(Integer screen) {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.screen = screen;
    return screen;
  }

  public void setTransparent() {
    ImageDisplayConfig c = (ImageDisplayConfig) config;
    c.bgColor = null;
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

  @Override
  public void startService() {
    super.startService();
    if (GraphicsEnvironment.isHeadless()) {
      log.info("in headless mode - {} will not display images", getName());
      return;
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      // Runtime.setConfig("default");

      // your all purpose image display service
      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");

      // Default Values
      // setting these are setting default values
      // so that any new display is created they will
      // have the following properties automatically set accordingly
      //

      // the display will always be on top
      // display.setAlwaysOnTop(true);

      // when a picture is told to go fullscreen
      // and is not the same ratio as the screen dimensions
      // this tells whether to scale and extend to the min
      // or max extension
      // display.setAutoscaleExtendsMax(true);

      // if there is a background while fullscreen - set the color rgb
      // display.setColor("#000000");

      // if true will resize image (depending on setAutoscaleExtendsMax)
      // display.setFullScreen(false);

      // set which screen device to be displayed on
      // display.setScreen(0);

      // set the default location for images to display
      // null values will mean image will be positioned in the center of the
      // screen
      // display.setLocation(null, null);

      // set the default dimensions for images to display
      // null values will be the dimensions of the original image
      // display.setDimension(null, null);

      // most basic display - an image file, can be relative or absolute file
      // path
      // displays are named - if you don't name them - they're name will be
      // "default"
      // this creates a display named default and display a snake.jpg
      display.display("snake.jpg");
      sleep(1000);

      // this creates a new display called "beetle" and loads it with beetle.jpg
      // "default" display is still snake.jpg
      display.display("beetle", "beetle.jpg");
      sleep(1000);

      // the image display service can also display images from the web
      // just supply the full url - they can be named as well - this one
      // replaces the snake image
      // since a name was not specified - its loaded into "default"
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/InMoov_Wheel_1.jpg/220px-InMoov_Wheel_1.jpg");
      sleep(1000);

      // animated gifs can be displayed as well - this is the earth
      // in fullscreen mode
      display.displayFullScreen("earth", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif");
      sleep(1000);

      // we can resize a picture
      display.resize("earth", 600, 600);
      sleep(1000);

      // and re-position it
      display.move("earth", 800, 800);

      // make another picture go fullscreen
      display.displayFullScreen("robot", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/1280px-FANUC_6-axis_welding_robots.jpg");
      sleep(1000);

      display.display("monkeys", "https://upload.wikimedia.org/wikipedia/commons/e/e8/Gabriel_Cornelius_von_Max%2C_1840-1915%2C_Monkeys_as_Judges_of_Art%2C_1889.jpg");
      sleep(1000);

      display.displayFullScreen("robot", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/1280px-FANUC_6-axis_welding_robots.jpg");
      sleep(1000);

      display.display("inmoov", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/InMoov_Wheel_1.jpg/220px-InMoov_Wheel_1.jpg");
      sleep(1000);

      display.display("mrl", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/VirtualInMoov.jpg/220px-VirtualInMoov.jpg");
      sleep(1000);

      for (int i = 0; i < 100; ++i) {
        display.move("monkeys", 20 + i, 20 + i);
        sleep(50);
      }

      display.resize("monkeys", 200, 200);
      sleep(1000);

      display.move("monkeys", 20, 20);
      sleep(1000);

      // now we can close some of the displays
      display.close("monkeys");
      sleep(1000);

      display.close("robot");

      int x0 = 500, y0 = 500;
      int r = 300;
      double x, y = 0;

      // move the inmoov image in a circle on the screen
      for (double t = 0; t < 4 * Math.PI; t += 1) {
        x = r * Math.cos(t) + x0;
        y = r * Math.sin(t) + y0;
        display.move("inmoov", (int) x, (int) y);
        sleep(100);
      }

      // in this example we will search for images and display them
      // start a google search and get the images back, then display them
      GoogleSearch google = (GoogleSearch) Runtime.start("google", "GoogleSearch");
      List<ImageData> images = google.imageSearch("monkey");
      for (ImageData img : images) {
        display.displayFullScreen(img.src);
        // display.display(img);
        sleep(1000);
      }

      display.setFullScreen(true);
      display.setAutoscaleExtendsMax(true);

      // another example we'll use wikipedia service to search
      // and attach the wikipedia to the display service
      // it will automagically display when an image is found
      Wikipedia wikipedia = (Wikipedia) Runtime.start("wikipedia", "Wikipedia");
      wikipedia.attach(display);
      // display.attach(wikipedia);
      images = wikipedia.imageSearch("bear");
      sleep(2000);
      display.setFullScreen(false);
      display.setAutoscaleExtendsMax(false);
      display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Noto_Emoji_Pie_1f4e2.svg/512px-Noto_Emoji_Pie_1f4e2.svg.png?20190227024729");
      sleep(1000);

      display.display("data/Emoji/512px/U+1F47D.png");
      sleep(1000);

      display.display("https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/512/emoji_u1f62c.png");
      sleep(1000);
      display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Noto_Emoji_Pie_1f4e2.svg/512px-Noto_Emoji_Pie_1f4e2.svg.png?20190227024729");
      sleep(1000);
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1c/Noto_Emoji_Pie_1f995.svg/512px-Noto_Emoji_Pie_1f995.svg.png?20190227143252");
      sleep(1000);

      display.save();

      log.info("done");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
