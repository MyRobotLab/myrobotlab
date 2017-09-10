/**
 *                    
 * @author GroG (at) myrobotlab.org
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
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

/**
 * @author GroG ServiceGUI - is owned immediately or through a routing map to
 *         ultimately a Service it has the capability of undocking and docking
 *         itself
 * 
 */

public abstract class ServiceGui implements WindowListener {

	public final static Logger log = LoggerFactory.getLogger(ServiceGui.class);
	public final String boundServiceName;
	transient public final SwingGui myService; // FIXME - rename gui

	/**
	 * display is the panel to be added to the JTabbedPane it has border layout
	 * similar to how a default frame its a good paradigm to think of the
	 * JTabbedPane as its own frame since when it become undocked - it is within
	 * a frame
	 */
	transient JPanel display = new JPanel(new BorderLayout());

	// border parts
	transient JPanel north;
	transient JPanel south;
	transient JPanel center;
	transient JPanel west;
	transient JPanel east;

	transient private JFrame undocked;
	transient protected ServiceGui self;
	boolean isHidden = false;
	
	transient GridBagConstraints gcNorth = new GridBagConstraints();
	transient GridBagConstraints gcCenter = new GridBagConstraints();
	transient GridBagConstraints gcSouth = new GridBagConstraints();
	
	public ServiceGui(final String boundServiceName, final SwingGui myService) {
		self = this;
		this.boundServiceName = boundServiceName;
		this.myService = myService;

		north = new JPanel(new GridBagLayout());

		west = new JPanel(new GridLayout(0, 2));
		center = new JPanel(new GridBagLayout());
		east = new JPanel(new GridLayout(0, 2));
		south = new JPanel(new GridBagLayout()); // flow

		display.add(north, BorderLayout.NORTH);
		display.add(east, BorderLayout.EAST);
		display.add(center, BorderLayout.CENTER);
		display.add(west, BorderLayout.WEST);
		display.add(south, BorderLayout.SOUTH);

		gcNorth.gridy = 0;
		gcSouth.gridy = 0;
		
		gcCenter.gridy = 0;
		gcCenter.fill = GridBagConstraints.BOTH; // ???
		// gcCenter.fill = GridBagConstraints.HORIZONTAL;
		gcCenter.weightx = 1;
		gcCenter.weighty = 1;
	}
	
	public void info(String msg, Object...params){
		myService.info(msg, params);
	}
	
	public void warn(String msg, Object...params){
		myService.warn(msg, params);
	}
	
	public void error(String msg, Object...params){
		myService.error(msg, params);
	}

	/**
	 * stub for subscribing all service specific/gui events for desired
	 * callbacks
	 */
	public void subscribeGui() {	  
	}

	/**
	 * stub for unsubscribing all service specific/gui events from desired
	 * callbacks
	 */
	public void unsubscribeGui() {
	}

	public JPanel getDisplay() {
		return display;
	}

	public boolean isDocked() {
		return undocked == null;
	}

	public boolean isHidden() {
		return isHidden;
	}

	/*
	 * hook for SwingGui framework to query each panel before release checking
	 * if any panel needs user input before shutdown
	 */
	public boolean isReadyForRelease() {
		return true;
	}

	public void makeReadyForRelease() {
	}

	public void send(String method) {
		send(method, (Object[]) null);
	}

	public void send(String method, Object... params) {
		myService.send(boundServiceName, method, params);
	}

	public void subscribe(String method) {
		subscribe(method, CodecUtils.getCallBackName(method));
	}

	public void subscribe(String method, String callback) {
		
		// send a message to the service - to subscribe to a method 
		MRLListener listener = new MRLListener(method, myService.getName(), callback);
		myService.send(boundServiceName, "addListener", listener);
		
		// here is the new magic secret sauce !!!
		// this is in mrl.js / panelSvc.js too
		// add that method in SwingGui's message routing to get the callback to 'this' 
		// tab panel, or widget - in mrl.js too along with other subscriptions to types
		myService.subscribeToServiceMethod(String.format("%s.%s", boundServiceName, callback), this);
	}

	public void unsubscribe(String inOutMethod) {
		unsubscribe(inOutMethod, CodecUtils.getCallBackName(inOutMethod));
	}

	public void unsubscribe(String inMethod, String outMethod) {
		myService.unsubscribe(boundServiceName, inMethod, myService.getName(), outMethod);
	}

	public void sendPeer(String peerKey, String method, Object... params){
		String fullKey = String.format("%s.%s",  boundServiceName, peerKey);
		myService.sendPeer(fullKey, method, params);
	}

	public void addTopGroup(String title, Object... components) {
		addTopLine(createFlowPanel(title, components));
	}

	public void addGroup(String title, Object... components) {
		addLine(createFlowPanel(title, components));
	}

	public void addBottomGroup(String title, Object... components) {
		addBottomLine(createFlowPanel(title, components));
	}

	public JPanel createFlowPanel(String title, Object... components) {
		JPanel panel = new JPanel();
		if (title != null) {
			TitledBorder t;
			t = BorderFactory.createTitledBorder(title);
			panel.setBorder(t);
		}
		for (int i = 0; i < components.length; ++i) {
			Object o = components[i];
			JComponent j = null;
			if (o == null) {
				log.error("addLine - component is null !");
				return null;
			}
			if (o.getClass().equals(String.class)) {
				JLabel label = new JLabel((String) o);
				j = label;
			} else {
				j = (JComponent) o;
			}
			panel.add(j);
		}

		return panel;
	}

	public JComponent addComponents(JPanel panel, Object... components) {
		panel.setLayout(new GridLayout(0, components.length));
		
		GridBagConstraints gc = new GridBagConstraints();
		for (int i = 0; i < components.length; ++i) {
			Object o = components[i];
			JComponent j = null;
			if (o == null) {
				log.error("addLine - component is null !");
				return null;
			}
			if (o.getClass().equals(String.class)) {
				JLabel label = new JLabel((String) o);
				j = label;
			} else {
				j = (JComponent) o;
			}

			gc.gridx = 0;
			gc.gridy = 0;
			// must be a jcomponent at this point..
			// j.setAlignmentY(Component.TOP_ALIGNMENT);
			// j.setAlignmentX(Component.LEFT_ALIGNMENT);
			// panel.add(j, gc.gridx++);
			panel.add(j);
		}

		return panel;
	}

	public JComponent addTopLine(Object... components) {
		JComponent c = addComponents(north, components);
		return c;
	}

	public JComponent addBottomLine(Object... components) {
		JComponent c = addComponents(south, components);
		return c;
	}

	public JComponent addLeftLine(Object... components) {
		JComponent c = addComponents(west, components);
		return c;
	}

	public JComponent addRightLine(Object... components) {
		JComponent c = addComponents(east, components);
		return c;
	}

	/*
	 * add a line to a panel
	 */
	public JComponent addLine(Object... components) {
		JComponent c = addComponents(center, components);
		// center.add(c);
		return c;
	}

	public void setTitle(String title) {
		TitledBorder t;
		t = BorderFactory.createTitledBorder(title);
		center.setBorder(t);
	}

	public void setTopTitle(String title) {
		TitledBorder t;
		t = BorderFactory.createTitledBorder(title);
		north.setBorder(t);
	}

	public void setBottomTitle(String title) {
		TitledBorder t;
		t = BorderFactory.createTitledBorder(title);
		south.setBorder(t);
	}

	public void setLeftTitle(String title) {
		TitledBorder t;
		t = BorderFactory.createTitledBorder(title);
		west.setBorder(t);
	}

	public void setRightTitle(String title) {
		TitledBorder t;
		t = BorderFactory.createTitledBorder(title);
		east.setBorder(t);
	}

	public void error(String errorMsg) {
		send("publishStatus", Status.error(errorMsg));
	}

	public void addTopLeft(Object... components){
		Container test = north.getParent();
		if (test == display){
			// add new left shift panel
			JPanel left = new JPanel(new BorderLayout());
			display.add(left, BorderLayout.NORTH);
			left.add(north, BorderLayout.WEST);
		}
		addTop(components);
	}
	
	public void addTop(Object... components) {
		addLinex(north, gcNorth, components);
	}
	
	public void addBottom(Object... components) {
		addLinex(south, gcSouth, components);
	}

	public void add(Object... components) {
		addLinex(center, gcCenter, components);
	}

	public void addLinex(JPanel panel, GridBagConstraints gc, Object... components) {

		// reset - line
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.gridx = 0;

		for (int i = 0; i < components.length; ++i) {
			Object o = components[i];
			JComponent j = null;
			if (o == null) {
				log.error("addLine - component is null !");
				return;
			}
			if (o.getClass().equals(Integer.class)) {
				gc.gridwidth = (Integer) o;
				continue;
			}

			if (o.getClass().equals(String.class)) {
				JLabel label = new JLabel((String) o);
				j = label;
			} else {
				j = (JComponent) o;
			}

			panel.add(j, gc);
			gc.gridx += gc.gridwidth;
			gc.gridwidth = 1;
		}
		gc.gridy += 1;
	}
	

  @Override
  public void windowOpened(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void windowClosing(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void windowClosed(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void windowIconified(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void windowActivated(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
    // TODO Auto-generated method stub
    
  }

}
