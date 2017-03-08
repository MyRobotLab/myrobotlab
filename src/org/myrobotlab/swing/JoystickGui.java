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

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.myrobotlab.service.Joystick;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.swing.widget.JoystickButtonsPanel;
import org.myrobotlab.swing.widget.JoystickCompassPanel;

public class JoystickGui extends ServiceGui implements ActionListener {

	static final long serialVersionUID = 1L;

	JComboBox<String> controllers = new JComboBox<String>();
	TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();
	TreeMap<String, Integer> components = new TreeMap<String, Integer>();
	HashMap<String, JLabel> outputValues = new HashMap<String, JLabel>();

	JButton refresh = new JButton("refresh");

	JoystickGui self = null;
	Joystick myJoy = null;

	JPanel output = new JPanel();

	JoystickButtonsPanel buttonsPanel = null;

	JoystickCompassPanel xyPanel, zrzPanel, rxryPanel, hatPanel;

	public JoystickGui(final String boundServiceName, final SwingGui myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		self = this;

		TitledBorder title;
		title = BorderFactory.createTitledBorder("axis");

		JPanel axisDisplay = new JPanel();
		axisDisplay.setBorder(title);
		// page_start.setLayout(new BoxLayout(page_start, BoxLayout.X_AXIS)); //
		// horizontal box
		// layout
		// three CompassPanels in a row

		// FIXME - dynamically build these out
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
	public void subscribeGui() {
		subscribe("getComponents");
		subscribe("getControllers");
		subscribe("publishJoystickInput");
		
		send("publishState");
		send("getControllers");
	}

	// FIXME - unsubscribes should be handled by Runtime
	// unless its implementation logic specific related
	@Override
	public void unsubscribeGui() {
		unsubscribe("getComponents");
		unsubscribe("getControllers");
		unsubscribe("publishJoystickInput");
	}

	public void onComponents(final HashMap<String, Integer> cmpnts) {
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

	public void onControllers(final Map<String, Integer> contrls) {
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
	public void onState(final Joystick joy) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// update reference
				myJoy = joy;
			}
		});

	}

	public void onJoystickInput(final JoystickData input) {
		log.info(String.format("onButton %s", input));
		if (input.value == null) {
			outputValues.get(input.id).setText("null");
			return;
		}
		if (outputValues.containsKey(input.id)) {
			outputValues.get(input.id).setText(input.value.toString());
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
