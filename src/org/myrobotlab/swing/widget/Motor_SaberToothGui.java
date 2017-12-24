package org.myrobotlab.swing.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.myrobotlab.service.Motor;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.Pin;

public class Motor_SaberToothGui extends MotorControllerPanel implements ActionListener {

  private static final long serialVersionUID = 1L;
  private SwingGui myService;

  JLabel motorPortLabel = new JLabel("motor port");
  JComboBox<String> motorPort = new JComboBox<String>();
  JButton attachButton = new JButton("attach");
  String arduinoName;
  String motorName;

  ArrayList<Pin> pinList = null;

  public Motor_SaberToothGui(SwingGui myService, String motorName, String controllerName) {
    super();
    this.myService = myService;
    this.arduinoName = controllerName;
    this.motorName = motorName;

    for (int i = 1; i < 3; ++i) {
      motorPort.addItem(String.format("m%d", i));
    }

    setBorder(BorderFactory.createTitledBorder("type - Adafruit Motor Shield"));
    add(motorPortLabel);
    add(motorPort);
    add(attachButton);
    setEnabled(true);

    attachButton.addActionListener(this);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();

    if (o == attachButton) {
      if ("attach".equals(attachButton.getText())) {
        Object[] motorData = new Object[] { motorPort.getSelectedItem() };
        myService.send(arduinoName, "motorAttach", motorName, motorData);
        attachButton.setText("detach");
      } else {
        myService.send(arduinoName, "motorDetach", motorName);
        attachButton.setText("attach");
      }

    }

  }

  /**
   * method to update the SwingGui from MotorController data
   */
  @Override
  public void set(Motor motor) {
    // TODO Auto-generated method stub
    motorPort.setSelectedItem(motor.getName());
  }

  @Override
  public void setAttached(boolean state) {
    if (state) {
      attachButton.setText("detach");
    } else {
      attachButton.setText("attach");
    }

  }

}
