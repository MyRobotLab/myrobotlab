package org.myrobotlab.service.config;

import java.util.HashMap;

import javax.swing.JFrame;

import org.myrobotlab.math.geometry.Rectangle;

public class ImageDisplayConfig extends ServiceConfig {

  
    public static class Display{
      // problematic - make transient
      String name;
      
      public Integer x, y;
      public Integer width, height;
      
      public Boolean fullscreen = null;
      
      public Boolean alwaysOnTop = true;
      
      public String bgColor = null;
      
      // on multiple monitor systems
      public Integer screen = null;

      public Float opacity = null;
      
      public Float scale = null;

      public String src;

      protected JFrame frame;

    }
  
  
    // yep Boolean not boolean because it needs to be
    // true false and "do not change current"
    public Boolean fullscreen = null;
    
    public Boolean alwaysOnTop = true;
    
    public String bgColor = null;
    
    // on multiple monitor systems
    public Integer screen = null;

    public Float opacity = null;
    
    public Float scale = null;

    public boolean enabled = true;
    
    public HashMap<String, Display> displays = new HashMap<>();

    
}
