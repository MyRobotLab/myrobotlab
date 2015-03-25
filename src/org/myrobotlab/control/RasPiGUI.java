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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.RasPi;
import org.myrobotlab.service.interfaces.MemoryDisplay;
import org.slf4j.Logger;

public class RasPiGUI extends ServiceGUI implements ActionListener, MemoryDisplay {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RasPiGUI.class.getCanonicalName());

	public RasPiGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RasPi.class);
		subscribe("publishNode", "publishNode", String.class, Node.class);
		subscribe("putNode", "putNode", Node.NodeContext.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RasPi.class);
	}

	@Override
	public void display(Node node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void displayStatus(Status status) {
		// TODO Auto-generated method stub

	}

	public void getState(RasPi raspi) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	@Override
	public void init() {
	}

}
