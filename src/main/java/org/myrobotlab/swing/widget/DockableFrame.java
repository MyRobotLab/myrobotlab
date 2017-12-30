package org.myrobotlab.swing.widget;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public final class DockableFrame extends JFrame {
	final static Logger log = LoggerFactory.getLogger(DockableTab.class);
	private static final long serialVersionUID = 1L;

	DockableTabData tabData;
	DockableTabPane tabPane;
	DockableFrame self;

	public DockableFrame(DockableTabPane tabPane, String label, final DockableTabData tabData) {
		super(label);
		self = this;
		this.tabData = tabData;
		this.tabPane = tabPane;
		if (tabData.x != 0 || tabData.y != 0) {
			setLocation(new Point(tabData.x, tabData.y));
			setPreferredSize(new Dimension(tabData.width, tabData.height));
		} else {
			pack();
		}

		//this.getRootPane().addComponentListener(new ComponentAdapter() {
		this.addComponentListener(new ComponentAdapter() {
			public void componentMoved( ComponentEvent e ) {
                // tabData.x
				tabData.x = e.getComponent().getX();
				tabData.y = e.getComponent().getY();
				self.tabPane.save();
            }
			public void componentResized(ComponentEvent e) {
				// Component c = e.getComponent();
				/*
				 * tabData.x = c.getX(); tabData.y = c.getY();
				 */
				// Point p = getLocation();
				// tabData.x = p.x;
				// tabData.y = p.y;
				tabData.width = self.getWidth();				
				tabData.height = self.getHeight();
				self.tabPane.save();
			}
		});
	}
}
