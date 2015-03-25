package org.myrobotlab.control.widget;

import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

/**
 * class or undocked display handles events of closing, saving position and
 * dimensions of
 *
 */
public class UndockedPanel implements Serializable {
	public class TabControlWindowAdapter extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent winEvt) {
			gui.dockPanel(label);
		}
	}

	public final static Logger log = LoggerFactory.getLogger(UndockedPanel.class.getCanonicalName());

	private static final long serialVersionUID = 1L;

	public int x;

	public int y;

	public int width;

	public int height;

	private String label;

	transient private JPanel panel;

	transient private JFrame frame;

	transient private GUIService gui;

	TabControlWindowAdapter windowAdapter = new TabControlWindowAdapter();

	public UndockedPanel(GUIService gui) {
		this.gui = gui;
	}

	public void close() {
		savePosition();
		if (frame != null) {
			frame.dispose();
			frame = null;
		}
	}

	public JFrame createFrame(String label, JPanel panel) {
		if (frame != null) {
			log.warn("{} frame already created", label);
			return frame;
		}

		this.label = label;
		this.panel = panel;

		frame = new JFrame(label);

		frame.getContentPane().add(panel);

		// icon
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		frame.setIconImage(img);

		if (x != 0 || y != 0) {
			frame.setLocation(x, y);
		}

		if (width != 0 || height != 0) {
			frame.setSize(width, height);
		}

		frame.addWindowListener(windowAdapter);

		frame.setVisible(true);
		frame.pack();
		return frame;
	}

	public JPanel getDisplay() {
		return panel;
	}

	public void hide() {
		if (frame != null) {
			frame.setVisible(false);
		} else {
			log.error("{} frame is null", label);
		}
	}

	public boolean isDocked() {
		return frame == null;
	}

	public void savePosition() {
		if (frame == null) {
			log.error("frame is null");
			return;
		}
		Point point = frame.getLocation();
		x = point.x;
		y = point.y;
		width = frame.getWidth();
		height = frame.getHeight();
	}

	public void unhide() {
		if (frame != null) {
			frame.setVisible(true);
		} else {
			log.error("{} frame is null", label);
		}
	}

}