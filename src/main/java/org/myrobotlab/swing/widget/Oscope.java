package org.myrobotlab.swing.widget;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.ServiceGui;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         class to display set of pins
 * 
 *         FIXME - auto-scaling amplitude FIXME - min / max FIXME - attempt to
 *         use completely by self, not embedded in any other service FIXME - put
 *         in AbstractMicrocontroller ! FIXME - update with PinArrayController -
 *         all pin states FIXME - line starts in the wrong direction FIXME -
 *         make a JButtonToggleImage with insets etc. - make a text one with 2
 *         BG and 2 FG colors (maybe glass like)
 *
 */
public class Oscope extends ServiceGui implements ActionListener {

  public final static Logger log = LoggerFactory.getLogger(Oscope.class);

  final JPanel buttonPanel = new JPanel(new GridBagLayout());

  final Box screenPanel = Box.createVerticalBox();

  final Map<String, OscopePinTrace> traces = new HashMap<>();

  public Oscope(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    addTop(new JButton("clear"));

    JPanel flow = new JPanel();
    flow.add(buttonPanel);
    addLeftLine(flow);

    flow = new JPanel();
    flow.add(screenPanel);
    add(flow);

    // add(screenPanel);

    // since this is a widget - subscribeGui is not auto-magically called
    // by the framework
    // subscribeGui();
  }

  /**
   * function for "all" pins, global actions, e.g. clear all pins
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
  }

  public void setPins(List<PinDefinition> pinList) {

    buttonPanel.removeAll();
    screenPanel.removeAll();

    GridBagConstraints bgc = new GridBagConstraints();
    GridBagConstraints sgc = new GridBagConstraints();

    float gradient = 1.0f / pinList.size();

    bgc.gridy = bgc.gridx = 0;
    sgc.gridy = sgc.gridx = 0;

    // gc.fill = GridBagConstraints.BOTH; // ???
    bgc.fill = GridBagConstraints.HORIZONTAL;
    sgc.fill = GridBagConstraints.HORIZONTAL;
    bgc.weighty = bgc.weightx = 1;
    sgc.weighty = sgc.weighty = 1;
    traces.clear();

    for (int i = 0; i < pinList.size(); ++i) {
      PinDefinition pinDef = pinList.get(i);

      OscopePinTrace trace = new OscopePinTrace(this, pinDef, gradient * i);
      traces.put(pinDef.getPinName(), trace);
      buttonPanel.add(trace.getButtonDisplay(), bgc);
      screenPanel.add(trace.getScreenDisplay());
      bgc.gridx++;
      sgc.gridy++;

      if (bgc.gridx % 4 == 0) {
        bgc.gridx = 0;
        bgc.gridy++;
      }
    }
  }

  /**
   * process the pin data for each pin
   * 
   * @param data
   */
  public void onPinArray(final PinData[] data) {
    for (PinData pinData : data) {
      if (pinData != null && pinData.pin != null) {
        OscopePinTrace trace = traces.get(pinData.pin);       
        trace.update(pinData);
      }
    }
  }

  // enabled -> true | false
  public void onPinDefinition(PinDefinition pinDef) {
    // updatePinDisplay(pinDef);
  }

  public void onPinList(List<PinDefinition> pinList) {
    traces.clear();
    setPins(pinList);
  }

  /*
   * public void subscribeGui() { subscribe("publishPinDefinition");
   * subscribe("publishPinArray"); subscribe("getPinList"); }
   * 
   * public void unsubscribeGui() { unsubscribe("publishPinDefinition");
   * unsubscribe("publishPinArray"); unsubscribe("getPinList"); }
   */
}
