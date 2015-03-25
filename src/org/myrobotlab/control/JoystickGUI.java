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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.myrobotlab.control.widget.JoystickButtonsPanel;
import org.myrobotlab.control.widget.JoystickCompassPanel;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Joystick;
import org.myrobotlab.service.Joystick.Button;
import org.myrobotlab.service.Runtime;

public class JoystickGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;

	JComboBox<String> controllers = new JComboBox<String>();
	TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();
	TreeMap<String, Integer> components = new TreeMap<String, Integer>();
	HashMap<String, JLabel> outputValues = new HashMap<String, JLabel>();

	JButton refresh = new JButton("refresh");

	JoystickGUI self = null;
	Joystick myJoy = null;

	JPanel output = new JPanel();

	JoystickButtonsPanel buttonsPanel = null;

	private JoystickCompassPanel xyPanel, zrzPanel, rxryPanel, hatPanel;

	public JoystickGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		self = this;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == controllers) {
			String selected = (String) controllers.getSelectedItem();
			if (selected == null || "".equals(selected)) {
				send("stopPolling");
			} else {
				log.info(String.format("changed to %s ", selected));
				send("setController", selected);
				send("startPolling");
			}
		} else if (o == refresh) {
			send("getControllers");
		}
		// myService.send(boundServiceName, "setType", e.getActionCommand());
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Joystick.class);
		subscribe("getControllers", "getControllers", HashMap.class);

		subscribe("publishX", "publishX", Float.class);
		subscribe("publishY", "publishY", Float.class);

		// xbox specific begin
		subscribe("publishRX", "publishRX", Float.class);
		subscribe("publishRY", "publishRY", Float.class);
		// xbox specific end

		subscribe("publishZ", "publishZ", Float.class);
		subscribe("publishRZ", "publishRZ", Float.class);
		subscribe("publishPOV", "publishPOV", Float.class);

		/*
		 * subscribe("XAxis", "XAxis", Integer.class); subscribe("YAxis",
		 * "YAxis", Integer.class); subscribe("ZAxis", "ZAxis", Integer.class);
		 * subscribe("ZRotation", "ZRotation", Integer.class);
		 * subscribe("hatSwitch", "hatSwitch", Integer.class);
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

		subscribe("getComponents", "getComponents", HashMap.class);
		subscribe("publishButton", "onButton", Float.class);

		send("publishState");
		send("getControllers");
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

	public void getComponents(final HashMap<String, Integer> cmpnts) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				output.removeAll();
				outputValues.clear();

				components.clear();
				components.putAll(cmpnts);

				Iterator<String> it = components.keySet().iterator();

				while (it.hasNext()) {
					String name = it.next();
					JPanel p = new JPanel();

					TitledBorder title = BorderFactory.createTitledBorder("");
					p.setBorder(title);

					p.add(new JLabel(String.format("%s:", name)));
					JLabel l = new JLabel("0.0");
					outputValues.put(name, l);
					p.add(l);
					output.add(p);
				}

				output.invalidate();
				output.repaint();
			}
		});

	}

	public void getControllers(final HashMap<String, Integer> contrls) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				controllers.removeAllItems();
				controllerNames.clear();
				controllerNames.putAll(contrls);
				Iterator<String> it = controllerNames.keySet().iterator();
				controllers.addItem("");
				while (it.hasNext()) {
					String name = it.next();
					controllers.addItem(name);
				}
				controllers.addActionListener(self);

			}
		});

	}

	// FIXME - is get/set state interact with Runtime registry ???
	// it probably should
	public void getState(final Joystick joy) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// update reference
				myJoy = joy;

				// controllers.setSelectedItem(null);

			}
		});

	}

	@Override
	public void init() {
		display.setLayout(new BorderLayout());

		// PAGE_START
		TitledBorder title;
		title = BorderFactory.createTitledBorder("axis");

		JPanel axisDisplay = new JPanel();
		axisDisplay.setBorder(title);
		// page_start.setLayout(new BoxLayout(page_start, BoxLayout.X_AXIS)); //
		// horizontal box
		// layout
		// three CompassPanels in a row
		hatPanel = new JoystickCompassPanel("POV");
		axisDisplay.add(hatPanel);

		xyPanel = new JoystickCompassPanel("x y");
		axisDisplay.add(xyPanel);

		rxryPanel = new JoystickCompassPanel("rx ry");
		axisDisplay.add(rxryPanel);

		zrzPanel = new JoystickCompassPanel("z rz");
		axisDisplay.add(zrzPanel);

		JPanel north = new JPanel(new BorderLayout());
		north.add(axisDisplay, BorderLayout.NORTH);

		// CENTER
		JPanel topCenter = new JPanel();
		// center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		title = BorderFactory.createTitledBorder("controller");
		JPanel controllerPanel = new JPanel();
		controllerPanel.setBorder(title);
		controllerPanel.add(controllers);
		controllerPanel.add(refresh);

		topCenter.add(controllerPanel);
		north.add(topCenter, BorderLayout.CENTER);

		display.add(north, BorderLayout.NORTH);

		refresh.addActionListener(this);

		title = BorderFactory.createTitledBorder("output");
		output.setBorder(title);

		topCenter.add(output);

		// PAGE_END
		buttonsPanel = new JoystickButtonsPanel();
		display.add(buttonsPanel, BorderLayout.PAGE_END);

		display.add(output, BorderLayout.CENTER);
		myJoy = (Joystick) Runtime.getService(boundServiceName);
	}

	public void onButton(final Button button) {
		log.info(String.format("onButton %s", button));
		if (button.value == null) {
			outputValues.get(button.id).setText("null");
			return;
		}
		if (outputValues.containsKey(button.id)) {
			outputValues.get(button.id).setText(button.value.toString());
		}
	}

	public void publish0(Float value) {
		buttonsPanel.setButton(0, value);
	}

	public void publish1(Float value) {
		buttonsPanel.setButton(1, value);
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

	public void publishPOV(Float value) {
		log.debug("{}", value);
		hatPanel.setDir(value);
		hatPanel.repaint();
		// hatOutput.setText(String.format("%.3f", value));
	}

	public void publishRX(Float value) {
		rxryPanel.setX(value);
		rxryPanel.repaint();
	}

	public void publishRY(Float value) {
		rxryPanel.setY(value);
		rxryPanel.repaint();
	}

	public void publishRZ(Float value) {
		zrzPanel.setY(value);
		zrzPanel.repaint();
	}

	public void publishX(Float value) {
		xyPanel.setX(value);
		xyPanel.repaint();
	}

	public void publishY(Float value) {
		xyPanel.setY(value);
		xyPanel.repaint();
	}

	public void publishZ(Float value) {
		zrzPanel.setX(value);
		zrzPanel.repaint();
	}

}
