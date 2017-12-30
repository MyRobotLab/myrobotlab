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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.UltrasonicSensor;
import org.slf4j.Logger;

public class UltrasonicSensorGui extends ServiceGui implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(UltrasonicSensorGui.class);

	JProgressBar range;

	public UltrasonicSensorGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);
		display.setLayout(new BorderLayout());

		range = new JProgressBar(0, 300);
		range.setValue(0);
		range.setStringPainted(true);
		range.setPreferredSize(new Dimension(380, 25));

		display.add(range, BorderLayout.NORTH);
		// JPanel center = new JPanel();

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

	}

	@Override
	public void subscribeGui() {
		subscribe("publishRange");
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("publishRange");
	}

	public void onState(UltrasonicSensor template) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	public void onRange(final Double r) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				range.setValue(r.intValue());
				range.setString(String.format("%d cm", r.intValue()));
			}
		});

	}

}
