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

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.CanvasFrame.Exception;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.myrobotlab.swing.widget.DockableTabPane;
import org.myrobotlab.vision.VisionData;
import org.slf4j.Logger;

public class VisionGui extends ServiceGui implements ListSelectionListener, VideoGUISource, ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VisionGui.class);

  transient VideoWidget video0 = null;
  // transient CanvasFrame cframe = null; // new
  transient Map<String, CanvasFrame> canvases = new HashMap<String, CanvasFrame>();
  transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
  transient JButton capturex = new JButton("capture");

  transient JButton capture = new JButton("capture");
  transient JButton filters = new JButton("filters");
  transient JButton record = new JButton("record");
  transient JButton fullscreen = new JButton("fullscreen");

  JPanel menuPanel = new JPanel();
  DockableTabPane tabs;

  JPanel vmenu = new JPanel(new GridLayout(0, 1));

  public VisionGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    vmenu.add(capture);
    vmenu.add(filters);
    vmenu.add(record);
    vmenu.add(fullscreen);

    capture.addActionListener(this);
    filters.addActionListener(this);
    record.addActionListener(this);
    fullscreen.addActionListener(this);

    video0 = new VideoWidget(boundServiceName, myService);
    menuPanel.add(capturex);
    tabs = new DockableTabPane(myService); // <-- SwingGui ???
    tabs.addTab("input", menuPanel);
    tabs.addTab("filter", menuPanel);
    tabs.getTabs().setVisible(false);

    add(video0.getDisplay(), vmenu);

  }

  @Override
  public void subscribeGui() {
    // subscribe("publishDisplay");
    subscribe("publishOpenCVData");
    // subscribe("getKeys");
  }

  @Override
  public void unsubscribeGui() {
    // unsubscribe("publishDisplay");
    unsubscribe("publishOpenCVData");
    // unsubscribe("getKeys");
  }

  public void onOpenCVData(VisionData data) {
    // Needed to avoid null pointer exception when
    // using RemoteAdapter
   // if (canvases.containsKey(data.getn) != null) {
      // cframe.showImage(converter.convert(data.getImage()));
   // } else {
      video0.displayFrame(new SerializableImage(data.getDisplayBufferedImage(), data.getDisplayFilterName()));
   // }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == filters) {
      int result = JOptionPane.showConfirmDialog(null, tabs.getTabs(), "My custom dialog", JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        System.out.println("You entered " + firstName.getText() + ", " + lastName.getText() + ", " + password.getText());
      } else {
        System.out.println("User canceled / closed the dialog, result = " + result);
      }
    } else if (o == fullscreen) {
      /*
      if (cframe == null) {
        // cframe = new CanvasFrame("test", 0);
        String[] desc = CanvasFrame.getScreenDescriptions();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        // Get size of each screen

        for (int i = 0; i < gs.length; i++) {
          DisplayMode dm = gs[i].getDisplayMode();
          int screenWidth = dm.getWidth();
          int screenHeight = dm.getHeight();
          try {
            cframe = new CanvasFrame("test", 0, dm);
          } catch (Exception e1) {
            log.error("canvas threw", e);
          }
        }

        cframe.addWindowListener(this);

        log.info("here");
      } else {
        cframe.setVisible(false);
        // cframe.dispose();
        // cframe = null;
      }
      */
    }

  }

  @Override
  public void windowClosed(WindowEvent e) {
    /*
    super.windowClosed(e);
    if (cframe != null) {
      cframe.setVisible(false);
      // cframe.dispose();
      // cframe = null;
    }    
    */
  }

  @Override
  public VideoWidget getLocalDisplay() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    // TODO Auto-generated method stub

  }

  public void addFilter() {
    // TODO Auto-generated method stub

  }

  JTextField firstName = new JTextField();
  JTextField lastName = new JTextField();
  JPasswordField password = new JPasswordField();
  final JComponent[] inputs = new JComponent[] { new JLabel("First"), firstName, new JLabel("Last"), lastName, new JLabel("Password"), password };

}
