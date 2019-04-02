package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.myrobotlab.service.data.PinData;

/**
 * This class extends JPanel so that we can control the painting and will do
 * continuous bit blitting of two buffered images, one stacked in front of the
 * other then swapped
 * 
 * @author GroG
 *
 */
public class OscopePinDisplay extends JPanel {
  
  private static final long serialVersionUID = 1L;
  
  OscopeTrace trace;

  int blit = 0;

  BufferedImage b0;
  BufferedImage b1;

  Graphics2D g0;
  Graphics2D g1;

  boolean paused = false;

  int timeDivisor = 1;

  public OscopePinDisplay(OscopeTrace trace) {
    super();
    setLayout(new BorderLayout());

    this.trace = trace;
    b0 = new BufferedImage(trace.width, trace.height, BufferedImage.TYPE_INT_RGB);
    b1 = new BufferedImage(trace.width, trace.height, BufferedImage.TYPE_INT_RGB);

    g0 = b0.createGraphics();
    g0.setPaint(trace.bgColor); // g0.setPaint(Color.GREEN);
    g0.fillRect(0, 0, b0.getWidth(), b0.getHeight());
    g0.setColor(trace.color);

    g1 = b1.createGraphics();
    g1.setPaint(trace.bgColor); // g1.setPaint(Color.CYAN);
    g1.fillRect(0, 0, b1.getWidth(), b1.getHeight());
    g1.setColor(trace.color);

    // setting starting point of b1 image
    // left of view port
    trace.screen1X = -1 * b1.getWidth();

    trace.lastX = b1.getWidth();

    // on the top-level Canvas to prevent AWT from repainting it, as
    // you'll
    // typically be doing this yourself within the animation loop.
    setIgnoreRepaint(true);
    setDoubleBuffered(false); // weird no access
    // setBackground(Color.GRAY);
    setOpaque(false);
  }

  /**
   * Drawing is done with the update thread, moving the viewport is done with
   * the swing thread.
   * 
   * the Oscope display composes of 2 screens - and a "viewport" the 2 screens
   * scroll from left to right in the viewport and the updating and clearing
   * of the images are done "outside" of the viewport. This keeps the graphics
   * very clean and the animation without flicker
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

    trace.pinData = pinData;

    trace.screen0X += timeDivisor;
    trace.screen1X += timeDivisor;

    // find active bit blit screen - its the one who's xpos is negative
    // because its "left" of the current viewing area being scrolled "right"
    // into view
    Graphics2D g = (blit % 2 == 0) ? trace.pinDisplay.g1 : trace.pinDisplay.g0;

    g.setPaint(trace.color);

    // find the "drawing point" - this is where the active screen which is
    // currently
    // scrolled left point of where it is in view
    int drawPointX = (blit % 2 == 0) ? (-1 * trace.screen1X) : (-1 * trace.screen0X);

    // FIXME - this needs to be inspected/refactored
    double y = trace.pinData.value * 20 + trace.height / 2;
    g.drawLine(trace.lastX, trace.lastY, drawPointX, (int) y);
    // log.info("{},{} - {},{}", trace.lastX, trace.lastY, drawPointX, y);

    if (trace.screen0X == trace.width) {
      // if b0 is offscreen clear it
      ++blit;
      g0.setPaint(trace.bgColor); // GREEN
      g0.fillRect(0, 0, b0.getWidth(), b0.getHeight());

      if (blit % 2 == 0) {
        trace.screen0X = 0;
        trace.lastX = -1; // offscreen
      } else {
        trace.screen0X = -trace.width; // starting roll with screen 0
        trace.lastX = trace.width; // offscreen
      }
    } else if (trace.screen1X == trace.width) {
      // if b1 is offscreen clear it
      ++blit;
      g1.setPaint(trace.bgColor); // CYAN
      g1.fillRect(0, 0, b1.getWidth(), b1.getHeight());
      if (blit % 2 == 0) {
        trace.screen1X = -trace.width; // starting roll with screen 1
        trace.lastX = trace.width; // offscreen
      } else {
        trace.screen1X = 0;
        trace.lastX = -1; // offscreen
      }
    } else {
      // set our current draw point as the new last draw point
      trace.lastX = drawPointX;
    }

    trace.pinInfo.setText(String.format("%.2f", pinData.value));
    // TODO - NOW IS THE TIME TO UPDATE BUFFERED IMAGES !!!
    // TODO - optimization of shifting the raster data ?

    trace.lastY = (int) y;

    // request a repaint to swing thread
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(trace.width, trace.height);
  }

  @Override
  public void paintComponent(Graphics g) {
    if (!paused) {
      g.drawImage(b0, trace.screen0X, 0, b0.getWidth(), b0.getHeight(), this);
      g.drawImage(b1, trace.screen1X, 0, b1.getWidth(), b1.getHeight(), this);
    }
  }
}

