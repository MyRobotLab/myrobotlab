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
 * References :
 * 	http://www.pjrc.com/teensy/td_joystick.html gamepad map
 * 
 * */

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.myrobotlab.control.widget.JoystickButtonsPanel;
import org.myrobotlab.control.widget.JoystickCompassPanel;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Joystick;
import org.myrobotlab.service.Runtime;

public class JoystickGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;

	JComboBox controllers = new JComboBox();
	JoystickGUI self = null;

	TreeMap<String, Integer> controllerNamess;

	JComboBox outputFormat = new JComboBox();
	JButton startJoystick = null;
	Joystick myJoystick = null;
	JoystickButtonsPanel buttonsPanel = null;

	private JoystickCompassPanel xyPanel, zrzPanel, hatPanel;

	public JoystickGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		self = this;
	}

	// ////// transforms begin ///////////////////
	// hat
	JTextField hatMultiplier = new JTextField("1", 3);
	JTextField hatOffset = new JTextField("0", 6);
	JLabel hatOutput = new JLabel("0.000");
	JButton hatTransform = new JButton("transform");

	// xaxis
	JTextField XAxisMultiplier = new JTextField("1", 3);
	JTextField XAxisOffset = new JTextField("0", 6);
	JLabel XAxisOutput = new JLabel("0.000");
	JButton XAxisTransform = new JButton("transform");

	// yaxis
	JTextField YAxisMultiplier = new JTextField("1", 3);
	JTextField YAxisOffset = new JTextField("0", 6);
	JLabel YAxisOutput = new JLabel("0.000");
	JButton YAxisTransform = new JButton("transform");

	// zaxis
	JTextField ZAxisMultiplier = new JTextField("1", 3);
	JTextField ZAxisOffset = new JTextField("0", 6);
	JLabel ZAxisOutput = new JLabel("0.000");
	JButton ZAxisTransform = new JButton("transform");

	// xaxis
	JTextField ZRotMultiplier = new JTextField("1", 3);
	JTextField ZRotOffset = new JTextField("0", 6);
	JLabel ZRotOutput = new JLabel("0.000");
	JButton ZRotTransform = new JButton("transform");

	// ////// transforms end ///////////////////

	public void init() {
		display.setLayout(new BorderLayout());

		// PAGE_START
		JPanel page_start = new JPanel();
		// page_start.setLayout(new BoxLayout(page_start, BoxLayout.X_AXIS)); //
		// horizontal box
		// layout
		// three CompassPanels in a row
		hatPanel = new JoystickCompassPanel("POV");
		page_start.add(hatPanel);

		xyPanel = new JoystickCompassPanel("xy");
		page_start.add(xyPanel);

		zrzPanel = new JoystickCompassPanel("zRz");
		page_start.add(zrzPanel);

		display.add(page_start, BorderLayout.PAGE_START);

		// CENTER
		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		TitledBorder title;
		title = BorderFactory.createTitledBorder("input");
		JPanel input = new JPanel();
		input.setBorder(title);
		input.add(controllers);
		center.add(input);

		JPanel output = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;

		gc.gridx = 0;
		gc.gridy = 0;
		output.add(hatTransform, gc);
		++gc.gridx;
		output.add(new JLabel(" Hat ", SwingConstants.LEFT), gc);
		++gc.gridx;
		output.add(new JLabel(" X "), gc);
		++gc.gridx;
		output.add(hatMultiplier, gc);
		++gc.gridx;
		output.add(new JLabel(" + "), gc);
		++gc.gridx;
		output.add(hatOffset, gc);
		++gc.gridx;
		output.add(new JLabel(" = "), gc);
		++gc.gridx;
		output.add(hatOutput, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;
		output.add(XAxisTransform, gc);
		++gc.gridx;
		output.add(new JLabel(" X Axis ", SwingConstants.LEFT), gc);
		++gc.gridx;
		output.add(new JLabel(" X "), gc);
		++gc.gridx;
		output.add(XAxisMultiplier, gc);
		++gc.gridx;
		output.add(new JLabel(" + "), gc);
		++gc.gridx;
		output.add(XAxisOffset, gc);
		++gc.gridx;
		output.add(new JLabel(" = "), gc);
		++gc.gridx;
		output.add(XAxisOutput, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;
		output.add(YAxisTransform, gc);
		++gc.gridx;
		output.add(new JLabel(" Y Axis ", SwingConstants.LEFT), gc);
		++gc.gridx;
		output.add(new JLabel(" X "), gc);
		++gc.gridx;
		output.add(YAxisMultiplier, gc);
		++gc.gridx;
		output.add(new JLabel(" + "), gc);
		++gc.gridx;
		output.add(YAxisOffset, gc);
		++gc.gridx;
		output.add(new JLabel(" = "), gc);
		++gc.gridx;
		output.add(YAxisOutput, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;
		output.add(ZRotTransform, gc);
		++gc.gridx;
		output.add(new JLabel(" Z Rotation ", SwingConstants.LEFT), gc);
		++gc.gridx;
		output.add(new JLabel(" X "), gc);
		++gc.gridx;
		output.add(ZRotMultiplier, gc);
		++gc.gridx;
		output.add(new JLabel(" + "), gc);
		++gc.gridx;
		output.add(ZRotOffset, gc);
		++gc.gridx;
		output.add(new JLabel(" = "), gc);
		++gc.gridx;
		output.add(ZRotOutput, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;
		output.add(ZAxisTransform, gc);
		++gc.gridx;
		output.add(new JLabel(" Z Axis ", SwingConstants.LEFT), gc);
		++gc.gridx;
		output.add(new JLabel(" X "), gc);
		++gc.gridx;
		output.add(ZAxisMultiplier, gc);
		++gc.gridx;
		output.add(new JLabel(" + "), gc);
		++gc.gridx;
		output.add(ZAxisOffset, gc);
		++gc.gridx;
		output.add(new JLabel(" = "), gc);
		++gc.gridx;
		output.add(ZAxisOutput, gc);
		++gc.gridx;

		hatTransform.addActionListener(this);
		XAxisTransform.addActionListener(this);
		YAxisTransform.addActionListener(this);
		ZAxisTransform.addActionListener(this);
		ZRotTransform.addActionListener(this);

		/*
		 * output.add(getTranformPanel("Hat        ", hatMultiplier, hatOffset,
		 * hatOutput)); output.add(getTranformPanel("X Axis     ",
		 * XAxisMultiplier, XAxisOffset, XAxisOutput));
		 * output.add(getTranformPanel("Y Axis     ", YAxisMultiplier,
		 * YAxisOffset, YAxisOutput));
		 * output.add(getTranformPanel("Z Rotation ", ZRotMultiplier,
		 * ZRotOffset, ZRotOutput)); output.add(getTranformPanel("ZAxis      ",
		 * ZAxisMultiplier, ZAxisOffset, ZAxisOutput));
		 */
		title = BorderFactory.createTitledBorder("output");
		output.setBorder(title);

		center.add(output);

		// PAGE_END
		JPanel page_end = new JPanel();
		buttonsPanel = new JoystickButtonsPanel();
		display.add(buttonsPanel, BorderLayout.PAGE_END);

		display.add(center, BorderLayout.CENTER);
		myJoystick = (Joystick) Runtime.getService(boundServiceName);
	}

	public JPanel getTranformPanel(String title, JTextField multiplier, JTextField offset, JLabel output) {
		JPanel pov = new JPanel();
		pov.add(new JButton("transform"));
		pov.add(new JLabel(title));
		pov.add(new JLabel("X"));
		pov.add(multiplier);
		pov.add(new JLabel("+"));
		pov.add(offset);
		pov.add(new JLabel("="));
		pov.add(output);
		return pov;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == controllers) {
			String selected = (String) controllers.getSelectedItem();
			if (selected == null || "".equals(selected)) {
				myService.send(boundServiceName, "stopPolling");
			} else {
				log.info(String.format("changed to %s ", selected));
				myService.send(boundServiceName, "setController", selected);
				myService.send(boundServiceName, "startPolling");
			}

		} else if (e.getSource() == hatTransform) {
			if (hatTransform.getText().equals("transform")) {
				myService.send(boundServiceName, "setHatTransform", Integer.parseInt(hatMultiplier.getText()), Integer.parseInt(hatOffset.getText()));
				hatTransform.setText("reset");
			} else {
				myService.send(boundServiceName, "resetHatTransform");
				hatTransform.setText("transform");
				hatMultiplier.setText("1");
				hatOffset.setText("0");
			}
		} else if (e.getSource() == XAxisTransform) {
			if (XAxisTransform.getText().equals("transform")) {
				myService.send(boundServiceName, "setXAxisTransform", Integer.parseInt(XAxisMultiplier.getText()), Integer.parseInt(XAxisOffset.getText()));
				XAxisTransform.setText("reset");
			} else {
				myService.send(boundServiceName, "resetXAxisTransform");
				XAxisTransform.setText("transform");
				XAxisMultiplier.setText("1");
				XAxisOffset.setText("0");
			}
		} else if (e.getSource() == YAxisTransform) {
			if (YAxisTransform.getText().equals("transform")) {
				myService.send(boundServiceName, "setYAxisTransform", Integer.parseInt(YAxisMultiplier.getText()), Integer.parseInt(YAxisOffset.getText()));
				YAxisTransform.setText("reset");
			} else {
				myService.send(boundServiceName, "resetYAxisTransform");
				YAxisTransform.setText("transform");
				YAxisMultiplier.setText("1");
				YAxisOffset.setText("0");
			}
		} else if (e.getSource() == ZRotTransform) {
			if (ZRotTransform.getText().equals("transform")) {
				myService.send(boundServiceName, "setZRotTransform", Integer.parseInt(ZRotMultiplier.getText()), Integer.parseInt(ZRotOffset.getText()));
				ZRotTransform.setText("reset");
			} else {
				myService.send(boundServiceName, "resetZRotTransform");
				ZRotTransform.setText("transform");
				ZRotMultiplier.setText("1");
				ZRotOffset.setText("0");
			}
		} else if (e.getSource() == ZAxisTransform) {
			if (ZAxisTransform.getText().equals("transform")) {
				myService.send(boundServiceName, "setZAxisTransform", Integer.parseInt(ZAxisMultiplier.getText()), Integer.parseInt(ZAxisOffset.getText()));
				ZAxisTransform.setText("reset");
			} else {
				myService.send(boundServiceName, "resetZAxisTransform");
				ZAxisTransform.setText("transform");
				ZAxisMultiplier.setText("1");
				ZAxisOffset.setText("0");
			}
		}
		// myService.send(boundServiceName, "setType", e.getActionCommand());
	}

	// FIXME - is get/set state interact with Runtime registry ???
	// it probably should
	public void getState(final Joystick joy) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (joy != null) {

					controllers.removeAllItems();

					controllerNamess = joy.getControllerNames();
					Iterator<String> it = controllerNamess.keySet().iterator();

					controllers.addItem("");
					while (it.hasNext()) {
						String name = it.next();
						controllers.addItem(name);
					}

					controllers.addActionListener(self);
					// controllers.setSelectedItem(null);
				}
			}
		});

	}

	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <-
	// Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Joystick.class);

		subscribe("publishX", "publishX", Float.class);
		subscribe("publishY", "publishY", Float.class);
		subscribe("publishZ", "publishZ", Float.class);
		subscribe("publishRZ", "publishRZ", Float.class);
		subscribe("publishPOV", "publishPOV", Float.class);

		/*
		subscribe("XAxis", "XAxis", Integer.class);
		subscribe("YAxis", "YAxis", Integer.class);
		subscribe("ZAxis", "ZAxis", Integer.class);
		subscribe("ZRotation", "ZRotation", Integer.class);
		subscribe("hatSwitch", "hatSwitch", Integer.class);
		*/

		subscribe("publish0", "publish0", Float.class);
		subscribe("publish1", "publish1", Float.class);
		subscribe("publish2", "publish2", Float.class);
		subscribe("publish3", "publish3", Float.class);
		subscribe("publish4", "publish4", Float.class);
		subscribe("publish5", "publish5", Float.class);
		subscribe("publish6", "publish6", Float.class);
		subscribe("publish7", "publish7", Float.class);
		subscribe("publish8", "publish8", Float.class);
		subscribe("publish9", "publish9", Float.class);
		subscribe("publish10", "publish10", Float.class);
		subscribe("publish11", "publish11", Float.class);
		subscribe("publish12", "publish12", Float.class);
		subscribe("publish13", "publish13", Float.class);

		myService.send(boundServiceName, "publishState");
	}


	public void publishX(Float value) {
		xyPanel.setX(value);
		xyPanel.repaint();
		XAxisOutput.setText(String.format("%.3f", value));
	}

	public void publishY(Float value) {
		xyPanel.setY(value);
		xyPanel.repaint();
		YAxisOutput.setText(String.format("%.3f", value));
	}

	public void publishZ(Float value) {
		zrzPanel.setX(value);
		zrzPanel.repaint();
		ZAxisOutput.setText(String.format("%.3f", value));
	}

	public void publishRZ(Float value) {
		zrzPanel.setY(value);
		zrzPanel.repaint();
		ZRotOutput.setText(String.format("%.3f", value));
	}

	public void publishPOV(Float value) {
		log.debug("{}", value);
		hatPanel.setDir(value);
		hatPanel.repaint();
		hatOutput.setText(String.format("%.3f", value));

	}

	public void publish0(Float value) {
		buttonsPanel.setButton(0, value);
	}

	public void publish1(Float value) {
		buttonsPanel.setButton(1, value);
	}

	public void publish2(Float value) {
		buttonsPanel.setButton(2, value);
	}

	public void publish3(Float value) {
		buttonsPanel.setButton(3, value);
	}

	public void publish4(Float value) {
		buttonsPanel.setButton(4, value);
	}

	public void publish5(Float value) {
		buttonsPanel.setButton(5, value);
	}

	public void publish6(Float value) {
		buttonsPanel.setButton(6, value);
	}

	public void publish7(Float value) {
		buttonsPanel.setButton(7, value);
	}
	
	public void publish8(Float value) {
		buttonsPanel.setButton(8, value);
	}

	public void publish9(Float value) {
		buttonsPanel.setButton(9, value);
	}

	public void publish10(Float value) {
		buttonsPanel.setButton(10, value);
	}
	
	public void publish11(Float value) {
		buttonsPanel.setButton(11, value);
	}

	public void publish12(Float value) {
		buttonsPanel.setButton(12, value);
	}
	
	public void publish13(Float value) {
		buttonsPanel.setButton(13, value);
	}
	
	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Joystick.class);

		unsubscribe("XAxisRaw", "XAxisRaw", Float.class);
		unsubscribe("YAxisRaw", "YAxisRaw", Float.class);
		unsubscribe("ZAxisRaw", "ZAxisRaw", Float.class);
		unsubscribe("ZRotationRaw", "ZRotationRaw", Float.class);
		unsubscribe("hatSwitchRaw", "hatSwitchRaw", Float.class);

		unsubscribe("XAxis", "XAxis", Float.class);
		unsubscribe("YAxis", "YAxis", Float.class);
		unsubscribe("ZAxis", "ZAxis", Float.class);
		unsubscribe("ZRotation", "ZRotation", Float.class);
		unsubscribe("hatSwitch", "hatSwitch", Float.class);

		unsubscribe("button0", "button0", Float.class);
		unsubscribe("button1", "button1", Float.class);
		unsubscribe("button2", "button2", Float.class);
		unsubscribe("button3", "button3", Float.class);
		unsubscribe("button4", "button4", Float.class);
		unsubscribe("button5", "button5", Float.class);
		unsubscribe("button6", "button6", Float.class);
		unsubscribe("button7", "button7", Float.class);
		unsubscribe("button8", "button8", Float.class);
		unsubscribe("button9", "button9", Float.class);
		unsubscribe("button10", "button10", Float.class);
		unsubscribe("button11", "button11", Float.class);
	}

}
