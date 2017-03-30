package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.ServiceGui;
import org.myrobotlab.swing.VideoWidget;

public class Oscope extends ServiceGui implements ActionListener {

  int PREFERRED_WIDTH = 400;
  int PREFERRED_HEIGHT = 200;

  // JPanel oscopePanel = new JPanel(new BorderLayout());
  // TODO - bit-blit 2 video panels
  VideoWidget video;
  GridBagConstraints gc = new GridBagConstraints();
  JPanel buttonPanel = new JPanel(new GridBagLayout());

  Map<String, Button> buttons = new HashMap<String, Button>();
  Map<Integer, Button> buttonIndex = new HashMap<Integer, Button>();
  float gradient;

  // raster level access
  protected int[] data;
  int w = 800;
  int h = 100;
  BufferedImage buffer;
  SerializableImage sensorImage;

  public class Button implements ActionListener {
    JButton b;
    PinDefinition pinDef;
    Color color;
    Oscope oscope;
    ActionListener relay;

    public Button(Oscope oscope, PinDefinition pinDef, Color hsv) {
      this.pinDef = pinDef;
      b = new JButton(pinDef.getName());
      b.setBackground(hsv);
      color = hsv;
      b.addActionListener(this);
      // relay
      addActionListener(oscope);
      this.oscope = oscope;
    }

    public Component getDisplay() {
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
    
    public void addActionListener(ActionListener relay){
      this.relay = relay;
    }

    public PinDefinition getPinDef() {
      return pinDef;
    }
 
  }

  public void onPinList(List<PinDefinition> pinList) {
    buttons.clear();
    addButtons(pinList);
  }

  public Oscope(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    video = new VideoWidget(boundServiceName, myService);
    video.allowFork(true);

    initialize();

    addTop(new JButton("clear"), buttonPanel);
    add(video.getDisplay());

    // since this is a widget - subscribeGui is not auto-magically called
    // by the framework
    subscribeGui();
    // video.displayFrame(sensorImage);
  }
  
  public void subscribeGui(){
    subscribe("publishPinDefinition");
    subscribe("publishPinArray");
    subscribe("getPinList");
  }
  
  public void unsubscribeGui(){
    unsubscribe("publishPinDefinition");
    unsubscribe("publishPinArray");
    unsubscribe("getPinList");
  }
  
  // enabled -> true | false
  public void onPinDefinition(PinDefinition pinDef){
    updatePinDisplay(pinDef);
  }

  public void updatePinDisplay(PinDefinition pinDef) {
    
  }

  public void addButtons(List<PinDefinition> pinList) {

    gradient = 1.0f / pinList.size();

    gc.gridy = gc.gridx = 0;
    // gc.fill = GridBagConstraints.BOTH; // ???
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weighty = gc.weightx = 1;

    // List<PinDefinition> pinList = pinDefs.getList();
    for (int i = 0; i < pinList.size(); ++i) {
      PinDefinition pinDef = pinList.get(i);
      Color hsv = new Color(Color.HSBtoRGB((i * (gradient)), 0.8f, 0.7f));
      Button button = new Button(this, pinDef, hsv);
      buttons.put(pinDef.getName(), button);
      buttonIndex.put(pinDef.getAddress(), button);
      buttonPanel.add(button.getDisplay(), gc);
      gc.gridx++;
      if (gc.gridx > 9) {
        gc.gridx = 0;
        gc.gridy++;
      }
    }

  }

  public void initialize() {
    data = new int[h * w];

    // Fill data array with pure solid black
    Arrays.fill(data, 0xff000000);

    // Java's endless black magic to get it working
    DataBufferInt db = new DataBufferInt(data, h * w);
    ColorModel cm = ColorModel.getRGBdefault();
    SampleModel sm = cm.createCompatibleSampleModel(w, h);
    WritableRaster wr = Raster.createWritableRaster(sm, db, null);
    buffer = new BufferedImage(cm, wr, false, null);
    sensorImage = new SerializableImage(buffer, "oscope");
  }

  // ??? - onPaint in extended JPanel or JLabel ???
  public void onPinArray(final PinData[] pins) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        for (PinData pin : pins) {
          
          Button b = buttonIndex.get(pin.address);
          PinDefinition pinDef = b.getPinDef();
          
          // log.info("PinData:{}", pin);
          sensorImage.setSource(pinDef.getName());
          video.displayFrame(sensorImage);

          /*
           * if (!traceData.containsKey(pin.address)) { TraceData td = new
           * TraceData(); float gradient = 1.0f / pinComponentList.size(); Color
           * color = new Color(Color.HSBtoRGB((pin.address * (gradient)), 0.8f,
           * 0.7f)); td.color = color; traceData.put(pin.address, td); td.index
           * = lastTraceXPos; }
           * 
           * int value = pin.value / 2;
           * 
           * TraceData t = traceData.get(pin.address); t.index++; lastTraceXPos
           * = t.index; t.data[t.index] = value; ++t.total; t.sum += value;
           * t.mean = t.sum / t.total;
           * 
           * g.setColor(t.color);
           * 
           * int yoffset = pin.address * 15 + 35; int quantum = -10;
           * 
           * g.drawLine(t.index, t.data[t.index - 1] * quantum + yoffset,
           * t.index, value * quantum + yoffset);
           * 
           * // computer min max and mean // if different then blank & post to
           * screen if (value > t.max) t.max = value; if (value < t.min) t.min =
           * value;
           * 
           * if (t.index < DATA_WIDTH - 1) { } else { // TODO - when hit marks
           * all startTracePos - cause the // screen is // blank - must iterate
           * through all t.index = 0;
           * 
           * clearScreen(); drawGrid();
           * 
           * g.setColor(Color.BLACK); g.fillRect(20, t.pin * 15 + 5, 200, 15);
           * g.setColor(t.color);
           * 
           * g.drawString(String.format("min %d max %d mean %d ", t.min, t.max,
           * t.mean), 20, t.pin * 15 + 20);
           * 
           * t.total = 0; t.sum = 0;
           * 
           * }
           */
        }

        video.displayFrame(sensorImage);

      }
    });

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Add relay ?
    Object o = e.getSource();
    if (o instanceof Button){
      Button b = (Button)o;
      PinDefinition pinDef = b.getPinDef();
      if (pinDef.isEnabled()){
        send("disablePin", pinDef.getAddress());
      } else {
        send("enablePin", pinDef.getAddress());
      }
    }
  }
}
