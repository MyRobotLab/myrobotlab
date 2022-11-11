package org.myrobotlab.service.config;

import java.awt.GraphicsDevice;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ImageDisplayConfig extends ServiceConfig {

  public static class Display {

    // transient parts
    transient public JFrame frame;
    transient public JPanel panel;
    transient public JLabel label;
    transient public ImageIcon imageIcon;
    transient public GraphicsDevice gd;
    transient public String name;

    public String src = null;

    public Integer x = null;

    public Integer y = null;

    public Integer width = null;

    public Integer height = null;

    public Boolean fullscreen = null;

    public Boolean alwaysOnTop = null;

    public Boolean autoscaleExtendsMax = null;

    public String bgColor = null;

    // on multiple monitor systems
    public Integer screen = null;

    public Float opacity = null;

    public Float scale = null;

    public Boolean visible = null;

  }

  // DEFAULT VALUES FOR DISPLAYS !

  /**
   * if not set - default will be center of screen
   */
  public Integer x = null;

  public Integer y = null;

  /**
   * if not set - default will be size of image
   */
  public Integer width = null;

  public Integer height = null;

  public boolean fullscreen = false;

  public boolean alwaysOnTop = true;

  public boolean autoscaleExtendsMax = true;

  public String bgColor = "#000000";

  // on multiple monitor systems
  public Integer screen = null;

  public Float opacity = null;

  public Float scale = null;

  public boolean visible = true;

  /**
   * default src of images - if one is not supplied will be
   * resource/ImageDisplay/mrl_logo.jpg
   */
  public String src = "/ImageDisplay/mrl_logo.jpg ";

  public HashMap<String, Display> displays = new HashMap<>();

  /**
   * enables the service - making this false will prevent the ImageDisplay from
   * displaying anymore images
   */
  public boolean enabled = true;

}
