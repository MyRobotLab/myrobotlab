package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;

/**
 * class to display a specific pin's data
 * @author GroG
 * 
 * FIXME - 
 *
 */
public class OscopePinTrace extends JPanel implements ActionListener {

  private static final long serialVersionUID = 1L;

  BufferedImage b0;
  BufferedImage b1;
  
  Color bgColor = Color.BLACK;
  int blit = 0;
  Color color;

  Graphics2D g0;
  Graphics2D g1;

  int height = 100;
  PinData lastPinData;

  int lastX = 0;
  int lastY = 0;

  Oscope oscope;
  
  JButton pause;

  boolean paused = false;

  PinData pinData;

  PinDefinition pinDef;

  JLabel valueLabel = new JLabel();
  JLabel minLabel = new JLabel("0.00");
  JLabel maxLabel = new JLabel("0.00");
  JLabel avgLabel = new JLabel("0.00");
  
  double min = 0;
  double max = 0;
  double avg = 0;
  double cnt = 0;

  JButton pinButton;
  int screen0X;

  int screen1X;
  JPanel screenDisplay = new JPanel(new BorderLayout());

  int timeDivisor = 1;

  // default trace dimensions
  int width = 600;

  Color inactiveColor;

  double multiplier = 20.0;

  boolean initialized = false;
  
  // mapper to provide auto-scaling
  Mapper pinMapper = new MapperLinear(0, 1, 0, 1);

  boolean newMinOrMax = false;

  public OscopePinTrace(Oscope oscope, PinDefinition pinDef, float hsv) {
    super();
    setLayout(new BorderLayout());
    
    this.oscope = oscope;
    this.pinDef = pinDef;
    
    color =  new Color(Color.HSBtoRGB((hsv), 0.9f, 1.0f));
    inactiveColor =  new Color(Color.HSBtoRGB((hsv), 0.5f, 0.7f));
    
    // screen 0 setup
    b0 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    g0 = b0.createGraphics();
    g0.setPaint(bgColor);
    g0.fillRect(0, 0, width, height);
    g0.setColor(color);

    // screen 1 setup
    b1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    g1 = b1.createGraphics();
    g1.setPaint(bgColor);
    g1.fillRect(0, 0, width, height);
    g1.setColor(color);

    // setting starting point of b1 image
    // left of view port
    screen1X = -1 * width;
    lastX = width;

    // on the top-level Canvas to prevent AWT from repainting it, as
    // you'll
    // typically be doing this yourself within the animation loop.
    
//    FIXME - not sure about these ..    
//    setIgnoreRepaint(true);
//    setDoubleBuffered(false); // weird no access
//    setOpaque(false);

    pinButton = new JButton(pinDef.getPinName());
    pinButton.setMargin(new Insets(0, 0, 0, 0));
    pinButton.setBorder(null);
    pinButton.setPreferredSize(new Dimension(30, 30));
    pinButton.setBackground(inactiveColor);
    pinButton.addActionListener(this);

    screenDisplay.add(this, BorderLayout.CENTER);

    JPanel traceControl = new JPanel();
    traceControl.setPreferredSize(new Dimension(140, 40));
    traceControl.setLayout(new GridLayout(0, 2));
    traceControl.add(new JLabel(" " + pinDef.getPinName()));
    traceControl.add(valueLabel);
    
    traceControl.add(new JLabel("  min"));
    traceControl.add(minLabel);

    traceControl.add(new JLabel("  max"));
    traceControl.add(maxLabel);

    traceControl.add(new JLabel("  avg"));
    traceControl.add(avgLabel);
    
    pause = new JButton(Util.getScaledIcon(Util.getImage("pause.png"), 0.25));
    pause.setBorder(BorderFactory.createEmptyBorder());
    pause.setContentAreaFilled(false);
    
    traceControl.add(pause);
    pause.addActionListener(this);

    screenDisplay.add(traceControl, BorderLayout.EAST);
    screenDisplay.setVisible(false);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();

    if (o == pause) {
      // toggle pause
      paused = !paused;
    } else if (o == pinButton) {
      if (pinDef.isEnabled()) {
        oscope.send("disablePin", pinDef.getAddress()); //<- wrong .. getPin() !
        pinButton.setBackground(inactiveColor);
        setVisible(false);
      } else {
        oscope.send("enablePin", pinDef.getAddress());
        setVisible(true);
        pinButton.setBackground(color);
      }
    }
  }

  public Component getButtonDisplay() {
    return pinButton;
  }

  public PinDefinition getPinDef() {
    return pinDef;
  }

  /**
   * used by swing framework to get the preferred size
   */
  @Override
  public Dimension getPreferredSize() {
    return new Dimension(width, height);
  }

  /**
   * used by oscope to set a pin's display 
   * @return
   */
  public Component getScreenDisplay() {
    return screenDisplay;
  }

  /**
   * the drawing of the two blitt'd images
   */
  @Override
  public void paintComponent(Graphics g) {
    if (!paused) {
      g.drawImage(b0, screen0X, 0, width, height, this);
      g.drawImage(b1, screen1X, 0, width, height, this);
    }
  }

  public void setVisible(boolean visible) {
    screenDisplay.setVisible(visible);
  }

  /**
   * Drawing is done with the update thread, moving the viewport is done with
   * the swing thread.
   * 
   * the Oscope display composes of 2 screens - and a "viewport" the 2 screens
   * scroll from left to right in the viewport and the updating and clearing of
   * the images are done "outside" of the viewport. This keeps the graphics very
   * clean and the animation without flicker
   * 
   * This function updates the offline screen. The moving of the "viewport" is
   * done with the swing thread in the "paint" routine
   * 
   * @param pinData
   *          - trace updated pin data
   */
  public void update(PinData pinData) {

    if (paused) {
      return;
    }
    
    ++cnt;
    
    if (!initialized) {
      min = pinData.value;
      minLabel.setText(String.format("%.2f", min));
   
      max = pinData.value;
      maxLabel.setText(String.format("%.2f", max));
      
      avg = pinData.value;
      avgLabel.setText(String.format("%.2f", avg));
      
      initialized = true;
    }

    screen0X += timeDivisor;
    screen1X += timeDivisor;
    
    ////////////////////////////////////////////////////
    // ======= begin some values here can be initialized once or in some modulus =========
    // FIXME - if (autoScale) ....
    // FIXME - this needs to be inspected/refactored
    double y = 0;
    int yMargin = 20;
    int yLow = yMargin;
    int yHi = height - yMargin;
    int yMaxDelta = yHi - yLow;
    
    ////////////////////////////////////////////////////
    
    // y = pinMapper.calcOutput(pinData.value) + yMargin;
    y = yHi - pinMapper.calcOutput(pinData.value);
    
    ////////////////////////////////////////////////////
        

    // find active bit blit screen - its the one who's xpos is negative
    // because its "left" of the current viewing area being scrolled "right"
    // into view
    Graphics2D g = (blit % 2 == 0) ? g1 : g0;

    g.setPaint(color);

    // find the "drawing point" - this is where the active screen which is
    // currently
    // scrolled left point of where it is in view
    int drawPointX = (blit % 2 == 0) ? (-1 * screen1X) : (-1 * screen0X);


    double yTest = height / 2 - pinData.value * multiplier;
    g.drawLine(lastX, lastY, drawPointX, (int) y);
    // log.info("{},{} - {},{}", lastX, lastY, drawPointX, y);

    if (screen0X == width) {
      // if b0 is offscreen clear it
      ++blit;
      g0.setPaint(bgColor);
      g0.fillRect(0, 0, width, height);

      if (blit % 2 == 0) {
        screen0X = 0;
        lastX = -1; // offscreen
      } else {
        screen0X = -width; // starting roll with screen 0
        lastX = width; // offscreen
      }
    } else if (screen1X == width) {
      // if b1 is offscreen clear it
      ++blit;
      g1.setPaint(bgColor);
      g1.fillRect(0, 0, width, height);
      if (blit % 2 == 0) {
        screen1X = -width; // starting roll with screen 1
        lastX = width; // offscreen
      } else {
        screen1X = 0;
        lastX = -1; // offscreen
      }
    } else {
      // set our current draw point as the new last draw point
      lastX = drawPointX;
    }

    valueLabel.setText(String.format("%.2f", pinData.value));
    // TODO - NOW IS THE TIME TO UPDATE BUFFERED IMAGES !!!
    // TODO - optimization of shifting the raster data ?

    lastY = (int) y;
    
    if (pinData.value < min) {
      min = pinData.value;
      minLabel.setText(String.format("%.2f", min));
      newMinOrMax  = true;
    }
    
    if (pinData.value > max) {
      max = pinData.value;
      maxLabel.setText(String.format("%.2f", max));
      newMinOrMax = true;
    }
    
    if (newMinOrMax) {
      pinMapper = new MapperLinear(min, max, 0.0, (double)yMaxDelta);
    }
    
    avg = ((cnt - 1) * avg + pinData.value)/cnt;
    avgLabel.setText(String.format("%.2f", avg));
  
    lastPinData = pinData;
    // request a repaint to swing thread
    repaint();
  }

}
