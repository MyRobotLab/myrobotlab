/**
 *
 * @author greg (at) myrobotlab.org
 *
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version (subject to the "Classpath" exception as provided in the LICENSE.txt
 * file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for details.
 *
 * Enjoy !
 *
 *
 */
package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service._TemplateService;
import org.slf4j.Logger;

public class GpsGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GpsGui.class);
  private JLabel latitudeTextField = new JLabel();
  private JLabel longitudeTextField = new JLabel();
  private JLabel altitudeTextField = new JLabel();
  private JLabel stringTypeTextField = new JLabel();
  private JLabel speedTextField = new JLabel();
  private JLabel headingTextField = new JLabel();

  public GpsGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    setTitle("gps");
    add("gps string type:", stringTypeTextField);
    add("latitude:", latitudeTextField);
    add("longitude:", longitudeTextField);
    add("altitude(meters):", altitudeTextField);
    add("speed (knots,kph):", speedTextField);
    add("heading (deg):", headingTextField);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public void subscribeGui() {
    subscribe("publishGGAData", "onData");
    subscribe("publishGLLData", "onData");
    subscribe("publishRMCData", "onData");
    subscribe("publishVTGData", "onData");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishGGAData", "onData");
    unsubscribe("publishGLLData", "onData");
    unsubscribe("publishRMCData", "onData");
    unsubscribe("publishVTGData", "onData");
  }

  public void onData(String[] tokens) {
    if (tokens[0].contains("GGA")) {
      stringTypeTextField.setText(tokens[0]);
      latitudeTextField.setText(tokens[2]);
      longitudeTextField.setText(tokens[4]);
      altitudeTextField.setText(tokens[9]);
    } else if (tokens[0].contains("VTG")) {
      stringTypeTextField.setText(tokens[0]);
      headingTextField.setText(tokens[1]);
      speedTextField.setText(tokens[5] + ", " + tokens[7]);
    } else if (tokens[0].contains("RMC")) {
      stringTypeTextField.setText(tokens[0]);
      latitudeTextField.setText(tokens[3]);
      longitudeTextField.setText(tokens[5]);
      speedTextField.setText(tokens[7]);
      headingTextField.setText(tokens[8]);
    } else if (tokens[0].contains("GLL")) {
      stringTypeTextField.setText(tokens[0]);
      latitudeTextField.setText(tokens[1]);
      longitudeTextField.setText(tokens[3]);
    }
  }

  public void onState(_TemplateService template) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
      }
    });
  }
}
