package org.myrobotlab.swing;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.service.DruppNeck;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;

/**
 * Super simplistic GUI for controlling the roll, pitch, and yaw for the drupp
 * neck.
 * 
 * @author kwatters
 *
 */
public class DruppNeckGui extends ServiceGui implements ChangeListener {

  JPanel rollPitchYawControlPanel = new JPanel();
  JLabel rollLabel = new JLabel("Roll");
  JLabel pitchLabel = new JLabel("Pitch");
  JLabel yawLabel = new JLabel("Yaw");

  // These seem to be the limits where the IK algorithm blows up and goes
  // negative!
  JSlider rollSlider = new JSlider(JSlider.HORIZONTAL, -90, 90, 0);
  JSlider pitchSlider = new JSlider(JSlider.HORIZONTAL, -90, 90, 0);
  JSlider yawSlider = new JSlider(JSlider.HORIZONTAL, -90, 90, 0);

  public DruppNeckGui(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    rollSlider.addChangeListener(this);
    pitchSlider.addChangeListener(this);
    yawSlider.addChangeListener(this);
    JPanel rollPanel = new JPanel();
    rollPanel.add(rollLabel, BorderLayout.PAGE_START);
    rollPanel.add(rollSlider, BorderLayout.PAGE_END);
    JPanel pitchPanel = new JPanel();
    pitchPanel.add(pitchLabel, BorderLayout.PAGE_START);
    pitchPanel.add(pitchSlider, BorderLayout.PAGE_END);
    JPanel yawPanel = new JPanel();
    yawPanel.add(yawLabel, BorderLayout.PAGE_START);
    yawPanel.add(yawSlider, BorderLayout.PAGE_END);
    rollPitchYawControlPanel.setLayout(new BorderLayout());
    rollPitchYawControlPanel.add(rollPanel, BorderLayout.PAGE_START);
    rollPitchYawControlPanel.add(pitchPanel, BorderLayout.CENTER);
    rollPitchYawControlPanel.add(yawPanel, BorderLayout.PAGE_END);
    display.add(rollPitchYawControlPanel, BorderLayout.PAGE_START);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof JSlider) {
      // some position changed. let's update the drupp neck
      DruppNeck neck = (DruppNeck) Runtime.getService(boundServiceName);
      double roll = rollSlider.getValue();
      double pitch = pitchSlider.getValue();
      double yaw = yawSlider.getValue();
      try {
        neck.moveTo(roll, pitch, yaw);
        rollLabel.setText("Roll: " + roll);
        pitchLabel.setText("Pitch: " + pitch);
        yawLabel.setText("Yaw: " + yaw);
      } catch (Exception e1) {
        log.error("Error occurred updating pos {} {} {}", roll, pitch, yaw, e1);
      }
    }
  }

}
