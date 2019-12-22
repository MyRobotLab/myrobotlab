package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.kinematics.Pose;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;

import org.myrobotlab.service.ServoMixer;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

import com.jidesoft.swing.JideLabel;

public class ServoMixerGui extends ServiceGui implements ActionListener, ChangeListener, MouseListener {

  public final static Logger log = LoggerFactory.getLogger(ServoMixerGui.class.toString());
  static final long serialVersionUID = 1L;
  private final String boundServiceName;
  private final ServoMixer servoMixer;

  JButton savePoseButton = new JButton("Save Pose");
  JButton loadPoseButton = new JButton("Load Pose");
  JTextField poseName = new JTextField("defaultPose", 16);
  JPanel servoControlPanel = new JPanel();
  JPanel poseControls = new JPanel();

  public ServoMixerGui(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    this.boundServiceName = boundServiceName;
    servoMixer = (ServoMixer) Runtime.getService(boundServiceName);
    display.setLayout(new BorderLayout());
    createServoGuiLayout();
  }

  private void createServoGuiLayout() {

    servoControlPanel.setLayout(new FlowLayout());
    List<ServoControl> servos = servoMixer.listAllServos();
    for (ServoControl sc : servos) {
      // TODO: create a better single servo control panel here.

      JPanel servoMiniControl = new JPanel();
      // servoMiniControl.setLayout(new BoxLayout(servoMiniControl,
      // BoxLayout.Y_AXIS));
      servoMiniControl.setLayout(new BorderLayout());
      // TODO: make this lable render vertically
      JideLabel servoLabel = new JideLabel(sc.getName());
      servoLabel.setOrientation(JideLabel.VERTICAL);
      JSlider servoSlider = new JSlider(JSlider.VERTICAL, 0, 180, sc.getPos().intValue());
      servoSlider.setName(sc.getName());
      servoSlider.addChangeListener(this);
      servoMiniControl.add(servoSlider, BorderLayout.PAGE_START);
      servoMiniControl.add(servoLabel, BorderLayout.PAGE_END);

      servoControlPanel.add(servoMiniControl);

    }
    // add a control bar to the bottom

    poseControls.setLayout(new FlowLayout());
    JLabel saveLabel = new JLabel("Save Pose");

    //
    poseControls.add(saveLabel);
    poseControls.add(poseName);
    poseControls.add(savePoseButton);
    poseControls.add(loadPoseButton);

    // add callbacks
    savePoseButton.addActionListener(this);
    loadPoseButton.addActionListener(this);

    display.add(servoControlPanel, BorderLayout.PAGE_START);
    display.add(poseControls, BorderLayout.PAGE_END);
  }

  public void refreshPanel() {

    // first clear the dispaly?
    display.remove(servoControlPanel);
    display.remove(poseControls);
    servoControlPanel.removeAll();
    poseControls.removeAll();
    // rebuild the gui.
    createServoGuiLayout();

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == savePoseButton) {
      //
      // TODO: get the list of selected servos to save
      // for now. just all servos in the system
      String name = poseName.getText();
      List<ServoControl> scs = servoMixer.listAllServos();
      try {
        servoMixer.savePose(name, scs);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else if (o == loadPoseButton) {
      //
      String name = poseName.getText();
      try {
        Pose p = servoMixer.loadPose(name);

        servoMixer.moveToPose(p);
        refreshPanel();

      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  @Override
  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    // update the position of a servo based on the update from the gui.
    if (e.getSource() instanceof JSlider) {
      JSlider slider = (JSlider) e.getSource();
      // this is an update to the position of the slider.
      log.info("{} moveTo {}", slider.getName(), slider.getValue());
      // At this point we need to get the servo and move it to the new value
      ServoControl s = (ServoControl) Runtime.getService(slider.getName());
    
        s.moveTo((double)slider.getValue());
      
    }
  }

}
