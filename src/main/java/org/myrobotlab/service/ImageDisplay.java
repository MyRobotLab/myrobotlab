package org.myrobotlab.service;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
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
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.slf4j.Logger;

public class ImageDisplay extends Service implements ImageListener, MouseListener, ActionListener, MouseMotionListener {

  private static final long serialVersionUID = 1L;

  final static Logger log = LoggerFactory.getLogger(ImageDisplay.class);

  String currentDisplay = null;

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

  public void displayFullScreen(String src, float opacity) {
    Display display = getDisplay(null);
    display.src = src;
    display.fullscreen = true;
    display.opacity = opacity;
    displayInternal(display.name);
  }

  public void display(String src, float opacity) {
    Display display = getDisplay(null);
    display.src = src;
    display.fullscreen = false;
    display.opacity = opacity;
    displayInternal(display.name);
  }

  public void displayScaled(String src, float opacity, float scale) {
    Display display = getDisplay(null);
    display.src = src;
    display.opacity = opacity;
    display.scale = scale;
    displayInternal(display.name);
  }

  @Deprecated /* no longer supported */
  public void displayFadeIn(String src) {
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

  public String display(String name, String src) {
    Display display = getDisplay(name);
    display.src = src;
    displayInternal(display.name);
    return name;
  }

  public Display display(String src) {
    Display display = getDisplay(null);
    displayInternal(display.name);
    return display;
  }

  private String displayInternal(final String name) {

    ImageDisplayConfig c = (ImageDisplayConfig) config;

    final ImageDisplay imageDisplay = this;

    if (!c.displays.containsKey(name)) {
      error("could not fine %s display", name);
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

          GraphicsDevice gd = null;
          if (display.screen != null && display.screen <= gs.length) {
            gd = gs[display.screen];
          } else {
            gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
          }

          // creating the swing components if necessary
          if (display.frame == null) {
            display.frame = new JFrame(gd.getDefaultConfiguration());
            display.frame.setName(name);
            display.frame.setLayout(new BorderLayout());
            display.panel = new JPanel(new BorderLayout());
            display.panel.setName("panel");
            display.label = new JLabel();
            display.label.setName("label");

            display.panel.add(display.label, BorderLayout.CENTER);
            display.frame.getContentPane().setLayout(new BorderLayout());
            display.frame.getContentPane().add(display.panel, BorderLayout.CENTER);
            display.frame.setUndecorated(true);

          }

          log.info("display screen {} displaying {}", gd.getIDstring(), display.name);

          // FIXME - problematic for cache !!!
          // display.label.setIcon(new ImageIcon(ImageIO.read(new File(display.src))));

          log.info("Loading image: ");
//          BufferedImage image = null;
//          if (display.src.startsWith("http://") || (display.src.startsWith("https://"))) {
//            String cacheFile = cacheDir + fs + display.src.replace("/", "_");
//            File check = new File(cacheFile);
//            if (check.exists()) {
//              image = ImageIO.read(new File(cacheFile));
//            } else {
//              log.info("from url...");
//              byte[] bytes = Http.get(display.src);
//              if (bytes != null) {
//                ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
//                image = ImageIO.read(bios);
//                // save cache
//                FileIO.toFile(cacheFile, bytes);
//              }
//            }
//          } else {
//            log.info("from file...");
//            image = ImageIO.read(new File(display.src));
//          }
          
          // get final uri/url for file 
          URL imageUrl = null;

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
                return;
              }
            }
            imageUrl = check.toURI().toURL();
          } else {
            File check = new File(display.src);
            if (!check.exists()) {
              error("%s does not exist", display.src);
            }
            imageUrl = new File(display.src).toURI().toURL();
          }


          // Image icon = new ImageIcon(new URL("http://i.stack.imgur.com/KSnus.gif")).getImage();
          // File file = new File("http://i.stack.imgur.com/KSnus.gif");
//          ImageIcon replacement = new ImageIcon(file.toURI().toURL());
          
          
//          display.img = image;
//          
//          if (image == null) {
//            error("could not read image %s", display.src);
//            return;
//          }
          
          display.imageIcon = new ImageIcon(imageUrl);
          display.label.setIcon(display.imageIcon);
          // display.label.setIcon(new ImageIcon(image));


          // TODO - make better / don't use setImageAutoSize (very bad
          // algorithm)
//          if (SystemTray.isSupported()) {
//            log.info("SystemTray is supported");
//            SystemTray tray = SystemTray.getSystemTray();
//            // Dimension trayIconSize = tray.getTrayIconSize();
//
//            TrayIcon trayIcon = new TrayIcon(image);
//            trayIcon.setImageAutoSize(true);
//
//            tray.add(trayIcon);
//          }

          if (display.bgColor != null) {
            Color color = Color.decode(display.bgColor);
            display.label.setOpaque(true);
            display.label.setBackground(color);
            display.frame.getContentPane().setBackground(Color.decode("#440000"));
          }

          if (display.alwaysOnTop != null && display.alwaysOnTop) {
            display.frame.setAlwaysOnTop(true);
          }

          if (display.fullscreen != null && display.fullscreen) {

            // auto scale image
            int displayWidth = gd.getDisplayMode().getWidth();
            int displayHeight = gd.getDisplayMode().getHeight();

            float wRatio = (float) displayWidth / display.imageIcon.getIconWidth();
            float hRatio = (float) displayHeight / display.imageIcon.getIconHeight();
            float ratio = (c.autoscaleExtendsMax) ? (wRatio < hRatio) ? hRatio : wRatio : (wRatio > hRatio) ? hRatio : wRatio;
            
            int resizedWidth = (int)(ratio * display.imageIcon.getIconWidth());
            int resizedHeight = (int)(ratio * display.imageIcon.getIconHeight());
            
            Image image = display.imageIcon.getImage().getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_DEFAULT);
            display.imageIcon.setImage(image);

            // center it
            display.frame.setLocation(displayWidth / 2 - resizedWidth / 2, displayHeight / 2 - resizedHeight / 2);
            // makes a difference on fullscreen
            display.frame.pack();

            // gd.setFullScreenWindow(frame);
            // vs
            display.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            // frame.setLocationRelativeTo(null);

          } else if ((display.width != null && display.width != null) || display.scale != null){
            
            // FIXME - "IF" SCALED THEN SETICON(ICON) !!!

            Integer resizedWidth = null;
            Integer resizedHeight = null;

            if (display.scale != null) {
              // FIXME - check this
              resizedWidth = (int)(display.scale * display.imageIcon.getIconWidth());
              resizedHeight = (int)(display.scale * display.imageIcon.getIconHeight());

            } else if (display.width != null && display.height != null) {
              resizedWidth = display.width;
              resizedHeight = display.height;
            }
            
            display.imageIcon.getImage().getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_DEFAULT);

            // display.label.setSize(imgWidth, imgHeight);
//             display.label.setIcon(new display.imageIcon(image));


            // If the component is null, or the
            // GraphicsConfiguration associated with
            // this component is null, the window is placed in the center of the
            // screen.
            // frame.setLocationRelativeTo(null);
          }
          
          int displayWidth = gd.getDisplayMode().getWidth();
          int displayHeight = gd.getDisplayMode().getHeight();

          int imgX = (display.x != null) ? display.x : displayWidth / 2 - display.imageIcon.getIconWidth() / 2;
          int imgY = (display.y != null) ? display.y : displayHeight / 2 - display.imageIcon.getIconHeight() / 2;

          display.frame.setLocation(imgX, imgY);

          // makes a difference on fullscreen
          display.frame.pack();
          

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
    return name;
  }

//  private BufferedImage resize(BufferedImage before, int width, int height) {
//
//    int w = before.getWidth();
//    int h = before.getHeight();
//    BufferedImage after = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//    AffineTransform at = new AffineTransform();
//    at.scale((float) width / w, (float) height / h);
//    AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
//    after = scaleOp.filter(before, after);
//
//    return after;
//  }

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
    currentDisplay = null;
    displays.clear();
  }

  public String close() {
    return close(currentDisplay);
  }

  public String close(String name) {
    Map<String, ImageDisplayConfig.Display> displays = ((ImageDisplayConfig) config).displays;

    Display display = displays.get(name);
    if (display != null) {
      if (display.frame != null) {
        display.frame.dispose();
        display.frame = null;
      }
      // displays.remove(src);
      return name;
    }
    return null;
  }

  public String displayFullScreen(String name, String src) {
    Display display = getDisplay(name);
    display.name = name;
    display.src = src;
    display.fullscreen = true;
    displayInternal(display.name);
    return display.name;
  }

  public String displayFullScreen(String src) {
    Display display = getDisplay(null);
    display.src = src;
    display.fullscreen = true;
    displayInternal(display.name);
    return display.name;
  }

  public String displayScaled(String src, float scale) {
    Display display = getDisplay(null);
    display.src = src;
    display.scale = scale;
    displayInternal(display.name);
    return display.name;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    ImageDisplayConfig config = (ImageDisplayConfig) c;
    for (String displayName : config.displays.keySet()) {
      close(displayName);
      displayInternal(displayName);
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
          Image image = display.imageIcon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
          display.imageIcon.setImage(image);
          display.panel.remove(display.label);
          display.label = new JLabel();
          display.label.setIcon(display.imageIcon);
          display.panel.setPreferredSize(new Dimension(width, height));
          display.panel.add(display.label, BorderLayout.CENTER);
          display.frame.setPreferredSize(new Dimension(width, height));
          display.frame.setSize(new Dimension(width, height));
          display.frame.setExtendedState(JFrame.NORMAL);
          display.frame.pack();
          display.width = width;
          display.height = height;
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
          display.x = x;
          display.y = y;
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

  public void attach(Attachable attachable) {
    if (attachable instanceof ImagePublisher) {
      attachImagePublisher(attachable.getName());
    } else {
      error("don't know how to attach a %s", attachable.getName());
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      // Runtime.setConfig("default");
      ImageDisplay display = (ImageDisplay) Runtime.start("display", "ImageDisplay");

//       boolean done = true;
//       if (done) {
//       return;
//       }
      
      
      display.displayFullScreen("earth", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Rotating_earth_%28large%29.gif/300px-Rotating_earth_%28large%29.gif");
      display.resize("earth", 300, 300);
      display.displayFullScreen("robot", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/FANUC_6-axis_welding_robots.jpg/1280px-FANUC_6-axis_welding_robots.jpg");
      Service.sleep(1000);


//      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
//      webgui.autoStartBrowser(false);
//      webgui.startService();
      // display.setFullScreen(true);
      // display.setColor("FF0000");
      display.display("monkeys", "https://upload.wikimedia.org/wikipedia/commons/e/e8/Gabriel_Cornelius_von_Max%2C_1840-1915%2C_Monkeys_as_Judges_of_Art%2C_1889.jpg");


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
      // wikipedia.attach(display);
      display.attach(wikipedia);
      images = wikipedia.imageSearch("bear");

      display.setAutoscaleExtendsMax(false);

      display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Noto_Emoji_Pie_1f4e2.svg/512px-Noto_Emoji_Pie_1f4e2.svg.png?20190227024729");

      display.display("data/Emoji/512px/U+1F47D.png");

      display.display("data/Emoji/512px/U+1F47D.png");

      display.display("https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/512/emoji_u1f62c.png");
      display.setAutoscaleExtendsMax(true);
      Service.sleep(1000);
      display.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Noto_Emoji_Pie_1f4e2.svg/512px-Noto_Emoji_Pie_1f4e2.svg.png?20190227024729");
      Service.sleep(1000);
      display.display("https://upload.wikimedia.org/wikipedia/commons/thumb/1/1c/Noto_Emoji_Pie_1f995.svg/512px-Noto_Emoji_Pie_1f995.svg.png?20190227143252");
      Service.sleep(1000);

      display.save();

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
