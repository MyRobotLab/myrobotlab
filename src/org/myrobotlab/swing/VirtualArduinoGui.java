/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.VirtualArduino;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.widget.DockableTabPane;
import org.myrobotlab.swing.widget.PinGui;
import org.myrobotlab.swing.widget.PortGui;

// FIXME - add stop watch capabilities
public class VirtualArduinoGui extends ServiceGui implements ActionListener {
  static final long serialVersionUID = 1L;
  PortGui portgui;
  JLabel status = new JLabel("disconnected");
  JComboBox<String> boardType = new JComboBox<String>();
  DockableTabPane localTabs = new DockableTabPane();
  VirtualArduinoGui self;

  /**
   * array list of graphical pin components built from pinList
   */
  ArrayList<PinGui> pinGuiList = null;

  List<PinDefinition> pinList = null;

  public VirtualArduinoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    self = this;
    // boardType.add(comp)
    VirtualArduino virtual = (VirtualArduino) Runtime.getService(boundServiceName);
    portgui = new PortGui(boundServiceName, myService);
    addTop(portgui.getDisplay(), boardType);
    addTop(status);
    localTabs.setTabPlacementRight();
    add(localTabs.getTabs());
    setPinTabUi(virtual);
  }

  @Override
  public void subscribeGui() {
    subscribe("publishBoardInfo");
    // subscribe("publishPinArray");
    subscribe("publishConnect");
    subscribe("publishDisconnect");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishBoardInfo");
    // unsubscribe("publishPinArray");
    unsubscribe("publishConnect");
    unsubscribe("publishDisconnect");
  }

  public void onState(final VirtualArduino c) {
  }

  public void onBoardInfo(BoardInfo boardInfo) {
    status.setText(String.format("connected %s", boardInfo));
  }

  public void setPinTabUi(VirtualArduino virtual) {

    JLayeredPane imageMap = new JLayeredPane();
    pinGuiList = new ArrayList<PinGui>();
    JLabel image = new JLabel();

    ImageIcon dPic = Util.getImageIcon("Arduino/uno.png");
    image.setIcon(dPic);
    Dimension s = image.getPreferredSize();
    image.setBounds(0, 0, s.width, s.height);
    imageMap.add(image, new Integer(1));

    List<PinDefinition> pins = virtual.getPinList();

    for (int i = 0; i < pins.size(); ++i) {

      PinGui p = new PinGui(virtual, pins.get(i));

      // p.showName();

      // set up the listeners
      p.addActionListener(self);
      pinGuiList.add(p);

      if (i < 14) { // digital pins -----------------
        int yOffSet = 0;
        if (i > 7) {
          yOffSet = 13; // gap between pins
        }

        p.setBounds(552 - 20 * i - yOffSet, 18, 15, 15);
        // p.onOff.getLabel().setUI(new VerticalLabelUI(true));
        imageMap.add(p.getDisplay(), new Integer(2));

      } else {

        p.setBounds(172 + 20 * i, 400, 15, 15);
        imageMap.add(p.getDisplay(), new Integer(2));

      }
    }
    localTabs.addTab("pin", imageMap);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub

  }

}
