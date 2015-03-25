package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.UndockedPanel;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * @author Gro-G
 * 
 * 
 *         THIS CLASS IS COMPLETELY BORKED !!!! - FIXE WITH TabControl2
 * 
 *         Mmmmmm... right click
 * 
 *         References:
 *         http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components
 *         -To-The-Tabs-On-JTabbedPaneI-Now-A-breeze
 *         http://stackoverflow.com/questions/8080438/mouseevent-of-jtabbedpane
 *         http://www.jyloo.com/news/?pubId=1315817317000
 */
public class TabControl extends JLabel implements ActionListener, MouseListener, MouseMotionListener {
	public class TabControlWindowAdapter extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent winEvt) {
			dockPanel();
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TabControl.class);
	JPopupMenu popup = new JPopupMenu();
	JTabbedPane parent;
	Container myPanel;
	private String boundServiceName;// FIXME - artifact of "Service" tabs
	JFrame undocked;
	TabControlWindowAdapter windowAdapter = new TabControlWindowAdapter();

	// JFrame top;
	GUIService myService;

	JMenuItem allowExportMenuItem;

	String filename = null;

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName) {
		this(gui, parent, myPanel, boundServiceName, boundServiceName, null, null);
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, Color foreground, Color background) {
		this(gui, parent, myPanel, boundServiceName, boundServiceName, foreground, background);
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, String txt) {
		super(txt);
		this.parent = parent;
		this.myPanel = myPanel;
		this.boundServiceName = boundServiceName;
		this.myService = gui;

		// build menu
		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getImageIcon("help.png"));
		menuItem.addActionListener(this);
		popup.add(menuItem);

		JMenuItem detachMenuItem = new JMenuItem("detach");
		detachMenuItem.addActionListener(this);
		detachMenuItem.setIcon(Util.getImageIcon("detach.png"));
		popup.add(detachMenuItem);

		JMenuItem releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getImageIcon("release.png"));
		popup.add(releaseMenuItem);

		allowExportMenuItem = new JMenuItem("prevent export");
		allowExportMenuItem.setActionCommand("prevent export");
		allowExportMenuItem.addActionListener(this);
		allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
		popup.add(allowExportMenuItem);

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, String txt, Color foreground, Color background) {
		this(gui, parent, myPanel, boundServiceName, txt);
		if (foreground != null) {
			setForeground(foreground);
		}
		if (background != null) {
			setBackground(background);
		}
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, String txt, String filename) {
		this(gui, parent, myPanel, boundServiceName, txt);
		this.filename = filename;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		// parent.getSelectedComponent()
		if (boundServiceName.equals(getText())) {
			// Service Frame
			ServiceInterface sw = Runtime.getService(getText());
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName());

			} else if ("detach".equals(cmd)) {
				undockPanel();
			} else if ("release".equals(cmd)) {
				myService.send(Runtime.getInstance().getName(), "releaseService", boundServiceName);
			} else if ("prevent export".equals(cmd)) {
				myService.send(boundServiceName, "allowExport", false);
				allowExportMenuItem.setIcon(Util.getImageIcon("allowExport.png"));
				allowExportMenuItem.setActionCommand("allow export");
				allowExportMenuItem.setText("allow export");
			} else if ("allow export".equals(cmd)) {
				myService.send(boundServiceName, "allowExport", true);
				allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
				allowExportMenuItem.setActionCommand("prevent export");
				allowExportMenuItem.setText("prevent export");
			}
		} else {
			// Sub Tabbed sub pane
			ServiceInterface sw = Runtime.getService(boundServiceName);
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName() + "#" + getText());

			} else if ("detach".equals(cmd)) {
				undockPanel();
			}
		}
	}

	private void dispatchMouseEvent(MouseEvent e) {
		parent.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, parent));
	}

	/**
	 * closes window and puts the panel back into the tabbed pane
	 */
	public void dockPanel() {
		// docking panel will move the data of the frame to serializable
		// position
		// FIXME - very hacked
		// myService.undockedPanels.get(boundServiceName).savePosition();
		// myService.undockedPanels.get(boundServiceName).isDocked();

		parent.add(myPanel);
		parent.setTabComponentAt(parent.getTabCount() - 1, this);
		if (undocked != null) {
			undocked.dispose();
			undocked = null;
		}

		// frame.pack(); - call pack
		myService.getFrame().pack();
		myService.save();
	}

	public String getFilename() {
		return filename;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (myService != null) {
			myService.lastTabVisited = this.getText();
		}
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

	/**
	 * undocks a tabbed panel into a JFrame FIXME - NORMALIZE - there are
	 * similar methods in GUIService FIXME - there needs to be clear pattern
	 * replacement - this is a decorator - I think... (also it will always be
	 * Swing)
	 * 
	 */
	public void undockPanel() {

		// myService.undockPanel(boundServiceName);
		// return;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				parent.remove(myPanel);
				if (boundServiceName.equals(getText())) {

					boolean hide = true;
					// if (!hide) {
					// service tabs
					undocked = new JFrame(boundServiceName);
					// check to see if this frame was positioned before
					UndockedPanel panel = null;
					/*
					 * if
					 * (myService.undockedPanels.containsKey(boundServiceName))
					 * { // has been undocked before panel =
					 * myService.undockedPanels.get(boundServiceName);
					 * undocked.setLocation(new Point(panel.x, panel.y));
					 * undocked.setPreferredSize(new Dimension(panel.width,
					 * panel.height)); } else { // first time undocked panel =
					 * new UndockedPanel(myService);
					 * myService.undockedPanels.put(boundServiceName, panel);
					 * panel.x = undocked.getWidth(); panel.y =
					 * undocked.getHeight(); }
					 */
					// panel.frame = undocked;
					// panel.isDocked = false;
					// undocked.setVisible(false);
					// }

				} else {
					// sub - tabs e.g. Arduino oscope, pins, editor
					// TABS ONLY FOR Arduino - sub tabs
					// GAH !! works but confusing !!
					undocked = new JFrame(boundServiceName + " " + getText());
				}

				// icon
				URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
				Toolkit kit = Toolkit.getDefaultToolkit();
				Image img = kit.createImage(url);
				if (undocked != null) {
					undocked.setIconImage(img);

					undocked.getContentPane().add(myPanel);
					undocked.addWindowListener(windowAdapter);
					// undocked.setTitle(boundServiceName);
					undocked.setVisible(true);
					undocked.pack();
				}
				myService.getFrame().pack();
				myService.save();

			}
		});

	}
}
