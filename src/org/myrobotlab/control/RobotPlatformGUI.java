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

package org.myrobotlab.control;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.RobotPlatform;

public class RobotPlatformGUI extends ServiceGUI {

	// if is local - can get a reference (experimental)
	RobotPlatform localReference = null;

	JLabel speed = new JLabel("0.0");

	// bindings begin --------------

	JLabel dimensionX = new JLabel("0.0");;
	JLabel dimensionY = new JLabel("0.0");
	JLabel dimensionZ = new JLabel("0.0");

	JLabel positionX = new JLabel("0.0");
	JLabel positionY = new JLabel("0.0");
	JLabel positionZ = new JLabel("0.0");

	JLabel targetX = new JLabel("0.0");
	JLabel targetY = new JLabel("0.0");
	JLabel targetZ = new JLabel("0.0");

	JLabel dhT = new JLabel("0.0");
	JLabel speedLast = new JLabel("0.0");

	JLabel power = new JLabel("0.0");

	JLabel headingCurrent = new JLabel("0.0");
	JLabel headingTarget = new JLabel("0.0");
	JLabel headingLast = new JLabel("0.0");
	JLabel headingDelta = new JLabel("0.0");
	JLabel headingSpeed = new JLabel("0.0");

	JLabel directionCurrent = new JLabel("0.0");
	JLabel directionTarget = new JLabel("0.0");

	JLabel inMotion = new JLabel("false");

	public RobotPlatformGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RobotPlatform.class);
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RobotPlatform.class);
	}

	// new state function begin ---------------
	// binding function TODO - do reflectively with default components?
	public void getState(RobotPlatform t) {
		// dimensionX.setText(t.dimensionX.toString());
		// dimensionY.setText(t.dimensionY.toString());
		// dimensionZ.setText(t.dimensionZ.toString());

		positionX.setText(Integer.toString(t.positionX));
		positionY.setText(Integer.toString(t.positionY));

		targetX.setText(Integer.toString(t.targetX));
		targetY.setText(Integer.toString(t.targetY));

		dhT.setText(Long.toString(t.updateHeadingTime - t.updateHeadingTimeLast));

		// TODO separate left & right power or a ratio factor for unbalanced
		// wheels/motors
		// TODO - possibly let motor deal with it?
		// power.setText(t.power.toString());

		headingCurrent.setText(Integer.toString(t.headingCurrent));
		headingTarget.setText(Integer.toString(t.headingTarget));
		headingLast.setText(Integer.toString(t.headingLast));
		headingDelta.setText(Integer.toString(t.headingDelta));
		if (t.headingDelta < 0) {
			directionTarget.setText("left");
		} else if (t.headingDelta > 0) {
			directionTarget.setText("right");
		} else {
			directionTarget.setText("locked");
		}
		headingSpeed.setText(Integer.toString(t.headingSpeed));

		headingSpeed.setText(Integer.toString(t.headingSpeed));

	}

	@Override
	public void init() {

		// position
		TitledBorder title;
		title = BorderFactory.createTitledBorder("position");

		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(title);

		gc.gridx = 0;
		gc.gridy = 0;

		// display.add(new JLabel("position (x,y,z)"));
		p.add(new JLabel("(x,y) ("), gc);
		++gc.gridx;
		p.add(positionX, gc);
		++gc.gridx;
		p.add(new JLabel(","), gc);
		++gc.gridx;
		p.add(positionY, gc);
		++gc.gridx;
		p.add(new JLabel(")"), gc);

		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("inMotion "), gc);
		++gc.gridx;
		p.add(inMotion, gc);

		display.add(p);

		// heading
		title = BorderFactory.createTitledBorder("heading");
		p = new JPanel(new GridBagLayout());
		p.setBorder(title);

		gc.gridx = 0;
		gc.gridy = 0;

		p.add(new JLabel("current "), gc);
		++gc.gridx;
		p.add(headingCurrent, gc);
		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("last "), gc);
		++gc.gridx;
		p.add(headingLast, gc);

		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("dT "), gc);
		++gc.gridx;
		p.add(dhT, gc);

		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("speed "), gc);
		++gc.gridx;
		p.add(headingSpeed, gc);

		display.add(p);

		// target
		title = BorderFactory.createTitledBorder("target");
		p = new JPanel(new GridBagLayout());
		p.setBorder(title);

		gc.gridx = 0;
		gc.gridy = 0;

		p.add(new JLabel("(x,y)         ("), gc);
		++gc.gridx;
		p.add(targetX, gc);
		++gc.gridx;
		p.add(new JLabel(","), gc);
		++gc.gridx;
		p.add(targetY, gc);
		++gc.gridx;
		p.add(new JLabel(")"), gc);

		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("bearing "), gc);
		++gc.gridx;
		p.add(headingTarget, gc);

		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("delta "), gc);
		++gc.gridx;
		p.add(headingDelta, gc);

		gc.gridx = 0;
		++gc.gridy;
		p.add(new JLabel("direction "), gc);
		++gc.gridx;
		p.add(directionTarget, gc);

		display.add(p);

		// targetY.setPreferredSize(new Dimension(d.width+60,d.height));;

	}

}
