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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.IpCamera;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.DirectionWidget;
import org.slf4j.Logger;

public class IpCameraGui extends ServiceGui implements ListSelectionListener {

  public class DirectionEventListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent ae) {
      log.info("{}", ae);
      if ("n".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_UP);
      } else if ("listener".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_UP);
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_LEFT);
      } else if ("e".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_LEFT);
      } else if ("se".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_DOWN);
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_LEFT);
      } else if ("s".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_DOWN);
      } else if ("sw".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_DOWN);
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_RIGHT);
      } else if ("w".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_RIGHT);
      } else if ("nw".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_UP);
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_RIGHT);
      } else if ("stop".equals(ae.getActionCommand())) {
        myService.send(boundServiceName, "move", IpCamera.FOSCAM_MOVE_STOP_RIGHT);
        // myService.send(boundServiceName, "move",
        // IPCamera.FOSCAM_MOVE_CENTER);
      } else if ("connect".equals(ae.getActionCommand())) {
        // host.getText(), user.getText(), password.getText()
        myService.send(boundServiceName, "connectVideoStream", videoURL.getText());
      } else if ("capture".equals(ae.getActionCommand())) {
        JButton b = (JButton) ae.getSource();
        if ("stop capture".equals(b.getText())) {
          b.setText("capture");
          myService.send(boundServiceName, "stopCapture");
        } else {
          b.setText("stop capture");
          myService.send(boundServiceName, "capture");
        }
      }
    }

  }

  /**
   * TODO - make Keyboard Widget
   * 
   * @author greg
   * 
   */
  public class Keyboard implements KeyListener {

    SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss:SSS");

    @Override
    public void keyPressed(KeyEvent keyEvent) {
      myService.send(boundServiceName, "keyPressed", keyEvent.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
      // log.error("Released" + keyEvent);
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
      // log.error("Typed" + keyEvent);
    }

  }

  public final static Logger log = LoggerFactory.getLogger(IpCameraGui.class);
  static final long serialVersionUID = 1L;

  VideoWidget video0;

  Keyboard keyboard;

  // JPanel info;

  IpCamera myIPCamera;
  JButton connect;
  // JButton capture;
  // JLabel connected;
  // JLabel notConnected;

  JTextField videoURL = new JTextField("http://fostcamIp/videostream.cgi?user=admin&pwd=password");
  JTextField controlURL = new JTextField("http://fostcamIp/decoder_control.cgi?user=admin&pwd=password&command=");

  /*
   * JTextField host = new JTextField("192.168.0.68", 8); JTextField user = new
   * JTextField("admin", 8); JPasswordField password = new
   * JPasswordField("xxxxx", 8);
   */

  DirectionWidget direction = new DirectionWidget();

  DirectionEventListener dirEventListener = new DirectionEventListener();

  public IpCameraGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    myIPCamera = (IpCamera) Runtime.getService(boundServiceName);
    direction.setDirectionListener(dirEventListener);


    display.setLayout(new BorderLayout());

    video0 = new VideoWidget(boundServiceName, myService);

    JPanel config = new JPanel(new GridLayout(0, 1));
    config.add(new JLabel("video url"));
    config.add(videoURL);
    config.add(new JLabel("control url"));
    config.add(controlURL);
    connect = new JButton("connect");
    connect.addActionListener(dirEventListener);
    config.add(connect);

    display.add(config, BorderLayout.SOUTH);

    // connected = new JLabel(new
    // ImageIcon(IPCameraGUI.class.getResource("/resource/bullet_ball_glass_green.png")));
    // notConnected = new JLabel(new
    // ImageIcon(IPCameraGUI.class.getResource("/resource/bullet_ball_glass_grey.png")));
    // display.add(notConnected, gc);
    // display.add(connected, gc);
    // connected.setVisible(false);
    JPanel center = new JPanel();
    center.add(video0.getDisplay());
    center.add(direction);
    display.add(center, BorderLayout.CENTER);

    // info = new JPanel();

    // capture = new JButton("capture");
    // capture.setActionCommand("capture");
    // capture.addActionListener(dirEventListener);
    /*
     * ++gc.gridy; display.add(capture, gc); ++gc.gridy; display.add(info, gc);
     */
  
  }

  @Override
  public void subscribeGui() {
    video0.subscribeGui();
    subscribe("setEnableControls");
    subscribe("publishDisplay");
  };


  @Override
  public void unsubscribeGui() {
    video0.unsubscribeGui();
    unsubscribe("setEnableControls");
    unsubscribe("publishDisplay");
  }


  /*
   * JUST STREAM THE #*%(%(*# VIDEO ! The return of IPCamera.getStatus which is
   * used to test connectivity
   * 
   * @param s
   */
  /*
   * public void getStatus(String s) { info.removeAll(); info.add(new
   * JLabel(s)); }
   */

  public void onDisplay(SerializableImage img) {
    video0.displayFrame(img);
  }

  public void onEnableControls(Boolean v) {
    // from service -> prevents control on the service level
    // event comes back and updates gui
    direction.btnN.setEnabled(v);
    direction.btnS.setEnabled(v);
    direction.btnE.setEnabled(v);
    direction.btnW.setEnabled(v);
    direction.btnStop.setEnabled(v);
    // direction.setEnabled(v);
  }

  @Override
  public void valueChanged(ListSelectionEvent arg0) {
    // TODO Auto-generated method stub

  }

}