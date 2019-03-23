package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.ServiceGui;

public class Oscope extends ServiceGui implements ActionListener {

  int w = 600;
  int h = 100;

  // JPanel oscopePanel = new JPanel(new BorderLayout());
  // TODO - bit-blit 2 video panels
  // VideoWidget video;
  GridBagConstraints bgc = new GridBagConstraints();
  GridBagConstraints sgc = new GridBagConstraints();
  JPanel buttonPanel = new JPanel(new GridBagLayout());
  JPanel screenPanel = new JPanel(new GridBagLayout());

  Map<String, Trace> traces = new HashMap<String, Trace>();
  Map<Integer, Trace> traceIndex = new HashMap<Integer, Trace>();
  float gradient;

  // raster level access
  protected int[] data;

  /**
   * this class extends JPanel so that we can control the painting
   * 
   * @author GroG
   *
   */
  public class PinDisplay extends JPanel {
    private static final long serialVersionUID = 1L;
    Trace trace;

    int blit = 0;

    BufferedImage b0;
    BufferedImage b1;

    Graphics2D g0;
    Graphics2D g1;

    boolean paused = false;

    int timeDivisor = 1;

    public PinDisplay(Trace trace) {
      super();
      setLayout(new BorderLayout());

      this.trace = trace;
      b0 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      b1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

      g0 = b0.createGraphics();
      g0.setPaint(Color.GREEN);
      g0.fillRect(0, 0, b0.getWidth(), b0.getHeight());
      g0.setColor(trace.color);

      g1 = b1.createGraphics();
      g1.setPaint(Color.CYAN);
      g1.fillRect(0, 0, b1.getWidth(), b1.getHeight());
      g1.setColor(trace.color);

      // setting starting point of b1 image
      // left of view port
      trace.b1Xpos = -1 * b1.getWidth();

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

      trace.b0Xpos += timeDivisor;
      trace.b1Xpos += timeDivisor;

      if (trace.b0Xpos == 600) {
        log.info("here");
      }

      // find active bit blit screen - its the one who's xpos is negative
      // because its "left" of the current viewing area being scrolled "right"
      // into view
      Graphics2D g = (trace.b0Xpos < 0) ? trace.pinDisplay.g0 : trace.pinDisplay.g1;

      g.setPaint(trace.color);

      // find the "drawing point" - this is where the active screen which is
      // currently
      // scrolled left point of where it is in view
      int drawPointX = (trace.b0Xpos < 0) ? (-1 * trace.b0Xpos) : (-1 * trace.b1Xpos);

      g.drawLine(drawPointX, trace.y + h / 2, drawPointX, trace.pinData.value * 20 + h / 2);
      log.info("{},{} - {},{}", trace.x, trace.y + h / 2, trace.b0Xpos, trace.pinData.value * 20 + h / 2);

      // if b0 is offscreen clear it
      if (trace.b0Xpos == trace.width) {
        ++blit;
        g0.setPaint(Color.GREEN);
        g0.fillRect(0, 0, b0.getWidth(), b0.getHeight());

        if (blit % 2 == 0) {
          trace.b0Xpos = 0;
        } else {
          trace.b0Xpos = -w;
        }
      }

      // if b1 is offscreen clear it
      if (trace.b1Xpos == trace.width) {
        ++blit;
        g1.setPaint(Color.CYAN);
        g1.fillRect(0, 0, b1.getWidth(), b1.getHeight());
        if (blit % 2 == 0) {
          trace.b1Xpos = -w;
        } else {
          trace.b1Xpos = 0;
        }
      }

      trace.pinInfo.setText(String.format("%d", pinData.value));
      // TODO - NOW IS THE TIME TO UPDATE BUFFERED IMAGES !!!
      // TODO - optimization of shifting the raster data ?

      // set our current draw point as the new last draw point
      trace.lastXPos = drawPointX;

      // request a repaint
      repaint();

    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(w, h);
    }

    // http://stackoverflow.com/questions/3256269/jtextfields-on-top-of-active-drawing-on-jpanel-threading-problems/3256941#3256941

    // https://www.nutt.net/create-scrolling-background-java/

    // g1 = b1.createGraphics();
    // http://stackoverflow.com/questions/15511282/how-can-i-make-a-java-swing-animation-smoother
    // http://stackoverflow.com/questions/2063607/java-panel-double-buffering
    // http://gamedev.stackexchange.com/questions/70711/how-do-i-double-buffer-renders-to-a-jpanel
    // http://stackoverflow.com/questions/4430356/java-how-to-do-double-buffering-in-swing
    // http://www.cokeandcode.com/info/tut2d.html
    @Override
    public void paintComponent(Graphics g) {
      // super.paintComponent(g); // possible slow parent method to fill
      // g.setColor(trace.color);
      // trace.xpos+=timeDivisor;

      // TODO - optimize ?
      // new way
      g.drawImage(b0, trace.b0Xpos, 0, b0.getWidth(), b0.getHeight(), this);
      g.drawImage(b1, trace.b1Xpos, 0, b1.getWidth(), b1.getHeight(), this);

      // g.drawImage(bufferImage, 0, 0, null); <-- FIXME - update buffer
      // outside of this method
      // g.drawLine(trace.x, trace.y + h / 2, trace.xpos,
      // trace.pinData.value * 20 + h / 2);
      // log.info("{},{} {}, {}", trace.x, trace.y + h / 2, trace.xpos,
      // trace.pinData.value * 20 + h / 2);
      // g.drawLine(trace.x, trace.y+20, trace.xpos, trace.pinData.value +
      // 20);

      // old way
      // g.drawLine(t.index, t.data[t.index - 1] * quantum + yoffset,
      // video.displayFrame(sensorImage);

    }
  }

  public class Trace implements ActionListener {
    JButton b;
    PinDefinition pinDef;
    Color color;
    Oscope oscope;
    ActionListener relay;

    PinDisplay pinDisplay;
    JPanel screenDisplay = new JPanel(new BorderLayout());
    JLabel pinInfo = new JLabel();

    int width = 600;
    int height = 100;

    int b0Xpos;
    int b1Xpos;
    int x;
    int y;

    PinData pinData;
    PinData lastPinData;
    int lastXPos = 0;

    public Trace(Oscope oscope, PinDefinition pinDef, Color hsv) {
      this.oscope = oscope;
      this.pinDef = pinDef;
      b = new JButton(pinDef.getPinName());
      b.setMargin(new Insets(0, 0, 0, 0));
      b.setBorder(null);
      b.setPreferredSize(new Dimension(30, 30));
      b.setBackground(hsv);
      color = hsv;
      b.addActionListener(this);
      // relay
      addActionListener(oscope);

      pinDisplay = new PinDisplay(this);

      screenDisplay.add(pinDisplay, BorderLayout.CENTER);

      JPanel flow = new JPanel();
      flow.add(new JLabel(pinDef.getPinName()));
      flow.add(pinInfo);
      screenDisplay.add(flow, BorderLayout.WEST);
      screenDisplay.setVisible(false);

    }

    public Component getButtonDisplay() {
      return b;
    }

    /**
     * events from button components are relayed to
     * 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      e.setSource(this);
      relay.actionPerformed(e);
    }

    public void addActionListener(ActionListener relay) {
      this.relay = relay;
    }

    public PinDefinition getPinDef() {
      return pinDef;
    }

    public Component getScreenDisplay() {
      return screenDisplay;
    }

    public void update(PinData pinData) {
      pinDisplay.update(pinData);
    }

    public void setVisible(boolean visible) {
      screenDisplay.setVisible(visible);
    }

  }

  public void onPinList(List<PinDefinition> pinList) {
    traces.clear();
    addButtons(pinList);
  }

  public Oscope(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    addTop(new JButton("clear"));
    JPanel flow = new JPanel();
    flow.add(buttonPanel);
    addLeftLine(flow);
    flow = new JPanel();
    flow.add(screenPanel);
    add(flow);

    // since this is a widget - subscribeGui is not auto-magically called
    // by the framework
    subscribeGui();
    // video.displayFrame(sensorImage);
  }

  public void subscribeGui() {
    subscribe("publishPinDefinition");
    subscribe("publishPinArray");
    subscribe("getPinList");
  }

  public void unsubscribeGui() {
    unsubscribe("publishPinDefinition");
    unsubscribe("publishPinArray");
    unsubscribe("getPinList");
  }

  // enabled -> true | false
  public void onPinDefinition(PinDefinition pinDef) {
    updatePinDisplay(pinDef);
  }

  public void updatePinDisplay(PinDefinition pinDef) {

  }

  public void addButtons(List<PinDefinition> pinList) {

    gradient = 1.0f / pinList.size();

    bgc.gridy = bgc.gridx = 0;
    sgc.gridy = sgc.gridx = 0;

    // gc.fill = GridBagConstraints.BOTH; // ???
    bgc.fill = GridBagConstraints.HORIZONTAL;
    sgc.fill = GridBagConstraints.HORIZONTAL;
    bgc.weighty = bgc.weightx = 1;
    sgc.weighty = sgc.weighty = 1;

    // List<PinDefinition> pinList = pinDefs.getList();
    for (int i = 0; i < pinList.size(); ++i) {
      PinDefinition pinDef = pinList.get(i);
      Color hsv = new Color(Color.HSBtoRGB((i * (gradient)), 0.5f, 1.0f));
      Trace trace = new Trace(this, pinDef, hsv);
      traces.put(pinDef.getPinName(), trace);
      traceIndex.put(pinDef.getAddress(), trace);
      buttonPanel.add(trace.getButtonDisplay(), bgc);
      screenPanel.add(trace.getScreenDisplay(), bgc);
      bgc.gridx++;
      sgc.gridy++;

      if (bgc.gridx % 4 == 0) {
        bgc.gridx = 0;
        bgc.gridy++;
      }

    }

  }

  public void onPinArray(final PinData[] data) {
    // optimize - test if visible
    for (PinData pinData : data) {

      Trace trace = traceIndex.get(pinData.address);
      trace.update(pinData);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Add relay ?
    Object o = e.getSource();
    if (o instanceof Trace) {
      Trace b = (Trace) o;
      PinDefinition pinDef = b.getPinDef();
      if (pinDef.isEnabled()) {
        send("disablePin", pinDef.getAddress());
        b.setVisible(false);
      } else {
        send("enablePin", pinDef.getAddress());
        b.setVisible(true);
      }
    }
  }
}
