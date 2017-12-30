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

package org.myrobotlab.swing.widget;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class Number {

  JTextField valueField = new JTextField(5);
  JLabel descriptionLabel = new JLabel();
  JLabel nameLabel = new JLabel();
  JPanel display = new JPanel();

  boolean isReal = true;

  double value = 0;
  double min = 0;
  double max = 0;
  double init = 0;
  String name = "";
  String description = "";

  public Number(String name, double init, double min, double max, String description) {
    this(name, init, min, max, description, true);
  }

  public Number(String name, double init, double min, double max, String description, boolean isReal) {
    this.name = name;
    this.value = init;
    this.init = init;
    this.min = min;
    this.max = max;
    this.description = description;
    this.isReal = isReal;

    valueField.setHorizontalAlignment(SwingConstants.RIGHT);

    if (isReal) {
      valueField.setText(init + "");
      descriptionLabel.setText(min + "-" + max + " " + description);
    } else {
      valueField.setText((int) init + "");
      descriptionLabel.setText((int) min + "-" + (int) max + " " + description);
    }

    nameLabel.setText(name + ":");

    display.add(nameLabel);
    display.add(valueField);
    display.add(descriptionLabel);
  }

  public Number(String name, int init, int min, int max, String description) {
    this(name, init, min, max, description, false);
  }

  public JComponent getDisplay() {
    return display;
  }

}
