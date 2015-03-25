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

package org.myrobotlab.control;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * @author GroG ServiceGUI - is owned immediately or through a routing map to
 *         ultimately a Service it has the capability of undocking and docking
 *         itself
 * 
 */

// if need an interface make TabAdapter extend WindowAdapter and contain it
public abstract class ServiceGUI extends WindowAdapter implements TabControlEventHandler {

	public final static Logger log = LoggerFactory.getLogger(ServiceGUI.class);
	public final String boundServiceName;
	public final GUIService myService;

	public GridBagConstraints gc = new GridBagConstraints();
	public JPanel display = new JPanel();

	// undocked information -- begin --

	public int x;

	public int y;

	public int width = 600;

	public int height = 600;

	transient private JFrame undocked;

	// undocked information -- end --

	// tab -- begin --
	TabControl2 tabControl;

	JTabbedPane tabs; // the tabbed pane this tab control belongs to

	// tab -- end --

	protected ServiceGUI self;
	boolean isHidden = false; // flipping visible on and off will ruin tab
								// panels i think

	public ServiceGUI(final String boundServiceName, final GUIService myService, JTabbedPane tabs) {
		self = this;
		this.boundServiceName = boundServiceName;
		this.myService = myService;
		this.tabs = tabs;
		this.tabControl = new TabControl2(this, myService.tabs, display, boundServiceName);

		tabs.addTab(boundServiceName, display);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, tabControl);

		gc.anchor = GridBagConstraints.FIRST_LINE_END;

		// place menu
		gc.gridx = 0;
		gc.gridy = 0;
		display.setLayout(new GridBagLayout());
		// gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

	}

	@Override
	public void actionPerformed(ActionEvent e, String tabName) {
		String cmd = e.getActionCommand();
		// parent.getSelectedComponent()
		String label = tabName;
		if (label.equals(tabName)) {
			// Service Frame
			ServiceInterface sw = Runtime.getService(tabName);
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName());

			} else if ("undock".equals(cmd)) {
				undockPanel();
			} else if ("release".equals(cmd)) {
				myService.send(Runtime.getInstance().getName(), "releaseService", label);
			} else if ("prevent export".equals(cmd)) {
				myService.send(label, "allowExport", false);
			} else if ("allow export".equals(cmd)) {
				myService.send(label, "allowExport", true);
			} else if ("hide".equals(cmd)) {
				// myService.send(label, "hide", true);
				// myService.hidePanel(label);
				hidePanel();
			}
		} else {
			// Sub Tabbed sub pane
			ServiceInterface sw = Runtime.getService(label);
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName() + "#" + tabName);

			} else if ("undock".equals(cmd)) {
				undockPanel();
			}
		}

	}

	public abstract void attachGUI();

	public abstract void detachGUI();

	// -- TabControlEventHandler -- begin
	@Override
	/**
	 * closes window and puts the panel back into the tabbed pane
	 */
	synchronized public void dockPanel() {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				// setting tabcontrol
				String label = tabControl.getText();
				display.setVisible(true);
				tabs.add(display);
				log.debug("here tabs count {}", tabs.getTabCount());
				tabs.setTabComponentAt(tabs.getTabCount() - 1, tabControl);

				savePosition();

				log.debug("{}", tabs.indexOfTab(label));

				if (undocked != null) {
					undocked.dispose();
					undocked = null;
				}

				// FIXME - necessary ? or just this panel invalidate?
				myService.getFrame().pack();
				myService.save();

				tabs.setSelectedComponent(display);
			}
		});
	}

	public JPanel getDisplay() {
		return display;
	}

	public Component getTabControl() {
		return tabControl;
	}

	public void hidePanel() {
		if (isHidden) {
			log.info("{} panel is already hidden", tabControl.getText());
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.info("hidePanel");
				if (undocked != null) {
					undocked.setVisible(false);
				} else {
					// YAY! - the way to do it !
					int index = tabs.indexOfComponent(display);
					// int index = tabs.indexOfTab(tabControl.getText());
					if (index != -1) {
						tabs.remove(index);
					} else {
						log.error("{} - has -1 index", tabControl.getText());
					}
				}

				isHidden = true;

			}
		});
	}

	public abstract void init();

	public boolean isDocked() {
		return undocked == null;
	}

	public boolean isHidden() {
		return isHidden;
	}

	/**
	 * hook for GUIService framework to query each panel before release checking
	 * if any panel needs user input before shutdown
	 * 
	 * @return
	 */
	public boolean isReadyForRelease() {
		return true;
	}

	public void makeReadyForRelease() {

	}

	@Override
	public void mouseClicked(MouseEvent event, String tabName) {
		if (myService != null) {
			myService.lastTabVisited = tabName;
		}
	}

	public void remove() {
		detachGUI();
		hidePanel();
		if (undocked != null) {
			undocked.dispose();
		}
	}

	public void savePosition() {
		if (undocked != null) {
			Point point = undocked.getLocation();
			x = point.x;
			y = point.y;
			width = undocked.getWidth();
			height = undocked.getHeight();
		}
	}

	public void send(String method) {
		send(method, (Object[]) null);
	}

	public void send(String method, Object... params) {
		myService.send(boundServiceName, method, params);
	}

	/*
	 * Service functions
	 */
	public void subscribe(String inOutMethod) {
		subscribe(inOutMethod, inOutMethod, (Class<?>[]) null);
	}

	public void subscribe(String inMethod, String outMethod) {
		subscribe(inMethod, outMethod, (Class<?>[]) null);
	}

	public void subscribe(String outMethod, String inMethod, Class<?>... parameterType) {
		MRLListener listener = null;
		if (parameterType != null) {
			listener = new MRLListener(outMethod, myService.getName(), inMethod, parameterType);
		} else {
			listener = new MRLListener(outMethod, myService.getName(), inMethod, null);
		}

		myService.send(boundServiceName, "addListener", listener);

	}

	public int test(int i, double d) {
		int x = 0;
		return x;
	}

	@Override
	/**
	 * undocks a tabbed panel into a JFrame FIXME - NORMALIZE - there are
	 * similar methods in GUIService FIXME - there needs to be clear pattern
	 * replacement - this is a decorator - I think... (also it will always be
	 * Swing)
	 * 
	 */
	// can't return JFrame referrence since its in a invokeLater..
	public void undockPanel() {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				tabs.remove(display);

				String label = tabControl.getText();

				if (undocked != null) {
					log.warn("{} undocked already created", label);
				}

				undocked = new JFrame(label);

				undocked.getContentPane().add(display);

				// icon
				URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
				Toolkit kit = Toolkit.getDefaultToolkit();
				Image img = kit.createImage(url);
				undocked.setIconImage(img);

				if (x != 0 || y != 0) {
					undocked.setLocation(x, y);
				}

				if (width != 0 || height != 0) {
					undocked.setSize(width, height);
				}

				undocked.addWindowListener(self);

				undocked.setVisible(true);
				undocked.pack();
			}
		});

	}

	public void unhidePanel() {
		if (!isHidden) {
			log.info("{} panel is already un-hidden", tabControl.getText());
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.info("unhidePanel {}", tabControl.getText());

				if (undocked != null) {
					undocked.setVisible(true);
				} else {
					display.setVisible(true);
					// tabs.addTab(tabControl.getText(), tabControl);

					tabs.add(tabControl.getText(), display);
					tabs.setTabComponentAt(tabs.getTabCount() - 1, tabControl);

				}

				// FIXME don't know what i'm doing...
				display.revalidate();
				// getFrame().revalidate();
				// getFrame().pack();

				isHidden = false;
			}

		});

	}

	// TODO - more closely model java event system with addNotification or
	// addListener
	public void unsubscribe(String inOutMethod) {
		unsubscribe(inOutMethod, inOutMethod, (Class<?>[]) null);
	}

	public void unsubscribe(String inMethod, String outMethod) {
		unsubscribe(inMethod, outMethod, (Class<?>[]) null);
	}

	public void unsubscribe(String outMethod, String inMethod, Class<?>... parameterType) {

		MRLListener listener = null;
		if (parameterType != null) {
			listener = new MRLListener(outMethod, myService.getName(), inMethod, parameterType);
		} else {
			listener = new MRLListener(outMethod, myService.getName(), inMethod, null);
		}
		myService.send(boundServiceName, "removeListener", listener);

	}

	// -- TabControlEventHandler -- end

	@Override
	public void windowClosing(WindowEvent winEvt) {
		dockPanel();
	}

}
