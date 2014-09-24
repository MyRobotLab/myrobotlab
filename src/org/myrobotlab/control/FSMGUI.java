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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class FSMGUI extends ServiceGUI {

	public final static Logger log = LoggerFactory.getLogger(FSMGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	JList transitionStates;
	JTable table = new JTable(8, 4);
	BasicArrowButton addServiceButton = null;

	public FSMGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public void init() {

		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		String[] namesAndClasses = { "hello-speech.speak Hello", "report-system.statusReport", "find bug toy-camera.findToy" };

		transitionStates = new JList(namesAndClasses);

		GridBagConstraints inputgc = new GridBagConstraints();
		inputgc.anchor = GridBagConstraints.FIRST_LINE_START;

		JScrollPane currentServicesScrollPane = new JScrollPane(transitionStates);

		transitionStates.setFixedCellWidth(420);
		transitionStates.setVisibleRowCount(20);
		input.add(getAddServiceButton(), inputgc);
		input.add(currentServicesScrollPane, inputgc);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("local services - current");
		input.setBorder(title);

		display.add(input, gc);

		++gc.gridx;

		display.add(table, gc);

	}

	public class LogLevel implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
		}

	}

	public JButton getAddServiceButton() {
		addServiceButton = new BasicArrowButton(BasicArrowButton.EAST);
		addServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFrame frame = new JFrame();
				frame.setTitle("add new service");
				String name = JOptionPane.showInputDialog(frame, "new service name");
				if (name != null) {
					String newService = (String) transitionStates.getSelectedValue();
					myService.send(boundServiceName, "create", newService, name);
				}
			}

		});

		return addServiceButton;
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}

}