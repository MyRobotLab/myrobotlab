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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Pid;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class PidGui extends ServiceGui implements ActionListener {

  JTextField key = new JTextField("test", 10);
  JTextField input = new JTextField(10);
  JLabel output = new JLabel("          ");
	JTextField kp = new JTextField(10);
	JTextField ki = new JTextField(10);
	JTextField kd = new JTextField(10);
	JButton setPID = new JButton("set");

	JButton direction = new JButton("invert");
  JButton setPid = new JButton("set pid");
  JButton compute = new JButton("compute");

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(PidGui.class);

	public PidGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);

    direction.addActionListener(this);
    setPID.addActionListener(this);
    
    addLine("key", key);
    addLine("Kp", kp);
    addLine("Ki", ki);
    addLine("Kd", kd);
    addLine(setPid, direction);
    addLine("input", input, "output", output, compute);
  
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == direction) {
			if (direction.getText().equals("invert")) {
				myService.send(boundServiceName, "setControllerDirection", new Integer(Pid.DIRECTION_REVERSE));
				direction.setText("direct");
			} else {
				myService.send(boundServiceName, "setControllerDirection", new Integer(Pid.DIRECTION_DIRECT));
				direction.setText("invert");
			}
		} else if (o == setPID) {
			Double Kp = Double.parseDouble(kp.getText());
			Double Ki = Double.parseDouble(ki.getText());
			Double Kd = Double.parseDouble(kd.getText());
			myService.send(boundServiceName, "setPID", Kp, Ki, Kd);
		}

	}

	@Override
	public void subscribeGui() {
	}

	@Override
	public void unsubscribeGui() {		
	}

	public void onState(final Pid pid) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Map<String, PidData> data = pid.getPidData();
				for (String p : data.keySet()) {
					int dir = pid.getControllerDirection(p);
					if (dir == Pid.DIRECTION_REVERSE) {
						direction.setText("direct");
					} else {
						direction.setText("invert");
					}

					ki.setText(String.format("%s", pid.getKi(p)));
					kp.setText(String.format("%s", pid.getKp(p)));
					kd.setText(String.format("%s", pid.getKd(p)));

				}
			}
		});
	}

}
