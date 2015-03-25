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

package org.myrobotlab.control.widget;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.NameValuePair;
import org.slf4j.Logger;

public class CFGSlider extends JPanel {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(CFGSlider.class.getCanonicalName());

	String boundService;
	Service myService = null;

	String CFGName;

	JSlider slider = null;
	JLabel label = null;
	JLabel outputLabel = null;

	int min = 0;
	int max = 100;
	int startValue = 0;

	public CFGSlider(String boundService, String CFGName, int min, int max, int startValue, Service myService) {
		super();
		this.boundService = boundService;
		this.min = min;
		this.max = max;
		this.startValue = startValue;
		this.CFGName = CFGName;
		this.myService = myService;
		initialize();
	}

	private JSlider getSlider() {
		if (slider == null) {
			slider = new JSlider(min, max, startValue);
			slider.addChangeListener(new javax.swing.event.ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					outputLabel.setText("" + slider.getValue());
					NameValuePair nvp = new NameValuePair(CFGName, Integer.toString(slider.getValue()));
					if (myService != null) {
						// myService.send(boundService, "setCFG", nvp);
						myService.send(boundService, "setCFG", nvp);

					} else {
						log.error("can not send message myService is null");
					}
				}
			});

		}
		return slider;
	}

	private void initialize() {
		// this.setSize(453, 62);
		this.setLayout(new GridBagLayout());

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;

		label = new JLabel(CFGName);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		this.add(label, gridBagConstraints);
		gridBagConstraints.gridx = 1;

		outputLabel = new JLabel(new Integer(startValue).toString());

		gridBagConstraints.gridx = 2;
		this.add(getSlider(), gridBagConstraints);

		this.add(outputLabel);

	}

}
