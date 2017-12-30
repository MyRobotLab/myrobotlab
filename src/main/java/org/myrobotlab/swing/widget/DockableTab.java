package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serializable;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

//FYI - isShowing is a element on the ui - but you probably need to replicated this for json

public class DockableTab implements ActionListener, MouseListener, MouseMotionListener, WindowListener, Serializable {
	private static final long serialVersionUID = 1L;
	final static Logger log = LoggerFactory.getLogger(DockableTab.class);

	JLabel title;

	/**
	 * needed for self reference inside swing utils runnable
	 */
	DockableTab self;

	/**
	 * frame for display when undocked
	 */
	DockableFrame undocked;

	/**
	 * right click popup menu
	 */
	JPopupMenu popup = new JPopupMenu();

	/**
	 * parent container
	 */
	DockableTabPane tabPane;

	/**
	 * menu items which maintain state
	 */
	JMenuItem allowExportMenuItem;
	JMenuItem hide;

	/**
	 * the contents of the tab
	 */
	Component display;

	/**
	 * the pane all tabs are in
	 */
	JTabbedPane tabs;

	/**
	 * dimensions for undocked frame panel
	 */
	DockableTabData tabData = new DockableTabData();
  public Color transitDockedColor;

	public JMenuItem addMenuItem(String action) {
		return addMenuItem(action, action);
	}

	public JMenuItem addMenuItem(String text, String action) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(this);
		menuItem.setActionCommand(action);
		menuItem.setIcon(Util.getImageIcon(String.format("%s.png", action)));
		popup.add(menuItem);
		return menuItem;
	}

	public DockableTab(DockableTabPane tabPane, String title, Component display) {
		this.title = new JLabel(title);
		this.tabPane = tabPane;
		this.display = display;
		this.tabs = tabPane.getTabs();
		this.self = this;

		addMenuItem(
				"<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>",
				"info");
		addMenuItem("undock");
		addMenuItem("release");
		addMenuItem("hide");
		addMenuItem("showAll");
		addMenuItem("explode");
		addMenuItem("collapse");

		this.title.addMouseListener(this);
		this.title.addMouseMotionListener(this);
	}

	// TODO - chain actions ? or prolly better to chain them @ DockableTabPane
	// ...
	// FIXME - how to chain callbacks ?
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		/*
		 * if ("prevent export".equals(cmd)) {
		 * allowExportMenuItem.setIcon(Util.getImageIcon("allowExport.png"));
		 * allowExportMenuItem.setActionCommand("allow export");
		 * allowExportMenuItem.setText("allow export"); } else if (
		 * "allow export".equals(cmd)) {
		 * allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
		 * allowExportMenuItem.setActionCommand("prevent export");
		 * allowExportMenuItem.setText("prevent export"); } else
		 */

		if ("undock".equals(cmd)) {
			undockTab();
		} else if ("dock".equals(cmd)) {
			dockTab();
		} else if ("info".equals(cmd)) {
			try {
				ServiceInterface si = Runtime.getService(title.getText());
				BareBonesBrowserLaunch.openURL(String.format("http://myrobotlab.org/service/%s", si.getSimpleName()));
			} catch (Exception e2) {
				log.error("info threw", e2);
			}
		} else if ("hide".equals(cmd)) {
			hideTab();
	  } else if ("release".equals(cmd)) {
      release();
    } else if ("collapse".equals(cmd)) {
			tabPane.collapse();
		} else if ("explode".equals(cmd)) {
			tabPane.explode();
		}
		// chain ?
		tabPane.actionPerformed(e);
	}

	/**
	 * important relay to keep JTabbedPane &amp; TabControl working together
	 * 
	 * @param e
	 */
	private void dispatchMouseEvent(MouseEvent e) {
		tabPane.getTabs().dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, tabPane.getTabs()));
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// useless -> handler.mouseClicked(e, getText());
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// log.info("e {}", e);
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		log.debug("mouseReleased");

		if (SwingUtilities.isRightMouseButton(e)) {
			log.debug("mouseReleased - right");
			popUpTrigger(e);
		}
		dispatchMouseEvent(e);
	}

	public void popUpTrigger(MouseEvent e) {
		popup.show(e.getComponent(), e.getX(), e.getY());
	}

	/**
	 * undocks a tabbed panel into a JFrame FIXME - NORMALIZE - there are
	 * similar methods in SwingGui FIXME - there needs to be clear pattern
	 * replacement - this is a decorator - I think... (also it will always be
	 * SwingGui)
	 * 
	 */
	// can't return JFrame referrence since its in a invokeLater..
	public void undockTab() {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			  
			  tabs.remove(display);

				String label = self.title.getText();

				if (undocked != null) {
					log.warn("{} undocked already created", label);
				}

				undocked = new DockableFrame(tabPane, label, tabData);
				undocked.getContentPane().add(display);

				// icon
				URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
				Toolkit kit = Toolkit.getDefaultToolkit();
				Image img = kit.createImage(url);
				undocked.setIconImage(img);

				if (tabData.x != 0 || tabData.y != 0) {
					undocked.setLocation(tabData.x, tabData.y);
				}

				if (tabData.width != 0 || tabData.height != 0) {
					undocked.setSize(tabData.width, tabData.height);
				}

				undocked.addWindowListener(self);
				undocked.addMouseMotionListener(self);
				
				undocked.setVisible(true);

				undocked.pack();
			}
		});

	}

	synchronized public void dockTab() {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				// setting tabcontrol
				String label = self.title.getText();
				display.setVisible(true);
				tabs.add(display);
				log.debug("here tabs count {}", tabs.getTabCount());
				tabs.setTabComponentAt(tabs.getTabCount() - 1, self.getTitleLabel());
				tabs.setBackgroundAt(tabs.getTabCount() - 1,transitDockedColor);
						
				// FIXME - callback
				savePosition();

				log.debug("{}", tabs.indexOfTab(label));

				if (undocked != null) {
					undocked.dispose();
					undocked = null;
				}

				tabs.setSelectedComponent(display);
			}
		});
	}

	// FIXME - CALLBACK
	public void savePosition() {
		if (undocked != null) {
			Point point = undocked.getLocation();
			tabData.x = point.x;
			tabData.y = point.y;
			tabData.width = undocked.getWidth();
			tabData.height = undocked.getHeight();
			tabData.title = title.getText();
			tabPane.save();
		}

	}

	@Override
	public void windowOpened(WindowEvent e) {
		log.info("windowOpened");
	}

	@Override
	public void windowClosing(WindowEvent e) {
		dockTab();
	}

	@Override
	public void windowClosed(WindowEvent e) {
		log.info("windowClosed");
	}

	@Override
	public void windowIconified(WindowEvent e) {
		log.info("windowIconified");
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		log.info("windowDeiconified");
	}

	@Override
	public void windowActivated(WindowEvent e) {
		log.info("windowActivated");
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		log.info("windowDeactivated");
	}
	


	public void hideTab() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.info("hideTab");
				if (undocked != null) {
					undocked.setVisible(false);
				} else {
					// YAY! - the way to do it !
					int index = tabs.indexOfComponent(display);
					// int index = tabs.indexOfTab(tabControl.getText());
					if (index != -1) {
						tabs.remove(index);
					} else {
						log.error("{} - has -1 index", self.title.getText());
					}
				}

				tabData.isHidden = true;

			}
		});
	}
	
	 private void release() {
	   log.info("release invoked from SwingGui");
	   Runtime.releaseService(title.getText());
	 }

	public void remove() {
		// unsubscribeGui(); -> Runtime is responsible for unsubcribing ...
		hideTab();
		if (undocked != null) {
			undocked.dispose();
		}
	}

	public void unhideTab() {
		// TODO Auto-generated method stub

	}

	public Component getTitleLabel() {
		return title;
	}
	



	public int getX() {
		return tabData.x;
	}

	public int getY() {
		return tabData.y;
	}

	public int getWidth() {
		return tabData.width;
	}

	public int getHeight() {
		return tabData.height;
	}

	public DockableTabData getData() {
		return tabData;
	}

	public void setData(DockableTabData tabData) {
		this.tabData = tabData;
	}

}
