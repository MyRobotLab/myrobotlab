package org.myrobotlab.control;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Gro-G
 * 
 *         Mmmmmm... right click
 * 
 *         References:
 *         http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components
 *         -To-The-Tabs-On-JTabbedPaneI-Now-A-breeze
 *         http://stackoverflow.com/questions/8080438/mouseevent-of-jtabbedpane
 *         http://www.jyloo.com/news/?pubId=1315817317000
 * 
 *         name of the tab is expected to be normalized in getText()
 */
public class TabControl2 extends JLabel implements ActionListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(TabControl2.class);

	JPopupMenu popup = new JPopupMenu(); // owns it
	JTabbedPane tabs; // the tabbed pane this tab control belongs to

	/**
	 * handles callback of the TabControl can be a ServiceGUI (ArduinoGUI), or a
	 * routed ServiceGUI (oscope)
	 */
	TabControlEventHandler handler;

	String boundServiceName;

	JMenuItem allowExportMenuItem;

	JMenuItem hide;

	public TabControl2(TabControlEventHandler handler, JTabbedPane tabs, Container myPanel, String label) {
		super(label);
		this.tabs = tabs;
		this.handler = handler;

		// build menu
		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getImageIcon("help.png"));
		menuItem.addActionListener(this);
		popup.add(menuItem);

		JMenuItem undockMenuItem = new JMenuItem("undock");
		undockMenuItem.addActionListener(this);
		undockMenuItem.setIcon(Util.getImageIcon("undock.png"));
		popup.add(undockMenuItem);

		JMenuItem releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getImageIcon("release.png"));
		popup.add(releaseMenuItem);

		allowExportMenuItem = new JMenuItem("prevent export");
		allowExportMenuItem.setActionCommand("prevent export");
		allowExportMenuItem.addActionListener(this);
		allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
		popup.add(allowExportMenuItem);

		hide = new JMenuItem("hide");
		hide.setActionCommand("hide");
		hide.addActionListener(this);
		hide.setIcon(Util.getImageIcon("hide.png"));
		popup.add(hide);

		addMouseListener(this);
		addMouseMotionListener(this);

		// this(gui, parent, myPanel, boundServiceName, txt);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if ("prevent export".equals(cmd)) {
			allowExportMenuItem.setIcon(Util.getImageIcon("allowExport.png"));
			allowExportMenuItem.setActionCommand("allow export");
			allowExportMenuItem.setText("allow export");
		} else if ("allow export".equals(cmd)) {
			allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
			allowExportMenuItem.setActionCommand("prevent export");
			allowExportMenuItem.setText("prevent export");
		}
		// routing swing events back down
		handler.actionPerformed(e, getText());
	}

	/**
	 * important relay to keep JTabbedPane & TabControl working together
	 * 
	 * @param e
	 */
	private void dispatchMouseEvent(MouseEvent e) {
		tabs.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, tabs));
	}

	public void dockPanel() {
		handler.dockPanel();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		handler.mouseClicked(e, getText());
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
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

	public void undockPanel() {
		handler.undockPanel();
	}

}
