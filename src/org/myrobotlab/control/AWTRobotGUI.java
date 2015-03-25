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

//raver1975 was here!

package org.myrobotlab.control;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.AWTRobot;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service._TemplateService;
import org.slf4j.Logger;

public class AWTRobotGUI extends ServiceGUI implements ActionListener, MouseMotionListener, MouseListener, MouseWheelListener {

	class MyCanvas extends JPanel implements MouseListener, MouseMotionListener {

		int x1 = 0;
		int y1 = 0;
		private JFrame window;
		private Rectangle screenRect;

		// static BufferedImage capture = null;

		public MyCanvas(JFrame window) {
			super();
			this.window = window;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Graphics graphics = this.getGraphics();
			graphics.setColor(new Color(1, 0, 0, 0.5f));
			graphics.fillRect(x1, y1, e.getX() - x1, e.getY() - y1);
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseMoved(MouseEvent e) {
			Graphics graphics = this.getGraphics();
			graphics.setColor(Color.blue);
			graphics.drawOval(e.getX() - 2, e.getY() - 2, 5, 5);
			// graphics.drawString("Drag a window to OCR", e.getX() + 5,
			// e.getY() - 10);
			repaint();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			x1 = e.getX();
			y1 = e.getY();

		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int x3 = e.getX();
			int y3 = e.getY();
			if (x1 == x3 || y1 == y3)
				return;
			int x2 = x3;
			int y2 = y3;

			if (x3 < x1) {
				x2 = x1;
				x1 = x3;
			}
			if (y3 < y1) {
				y2 = y1;
				y1 = y3;
			}
			Rectangle max = ((AWTRobot) Runtime.getService(boundServiceName)).getMaxBounds();
			screenRect = new Rectangle(x1 + max.x, y1 + max.y, x2 - x1, y2 - y1);

			myService.send(boundServiceName, "setBounds", screenRect);
			window.setVisible(false);
			// WindowReaderMain.capture(screenRect);
		}

		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.red);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			g.drawRect(0, 0, screenSize.width - 1, screenSize.height - 1);
			g.drawRect(1, 1, screenSize.width - 3, screenSize.height - 3);
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN, .7f));
			Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, this.getWidth(), this.getHeight());
			((Graphics2D) g).fill(rect);
			// g.setColor(new Color(0,0,0,1f));
			// g.fillRect(0, 0,this.getWidth(), this.getHeight());
			// g.setColor(new Color(0,0,0,.01f));
			// g.fillRect(0, 0,this.getWidth(), this.getHeight());

			// if (capture!=null){g.drawImage(capture,100,100, null);
			// g.drawRect(100, 100, capture.getWidth() - 1, capture.getHeight()
			// -
			// 1);}
			// g.setColor(Color.blue);
			// g.drawString("o", x, y);
		}

	}

	static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(AWTRobotGUI.class.getCanonicalName());
	VideoWidget video0 = null;
	JTextField status = new JTextField("", 20);
	JLabel x = new JLabel("0");
	JLabel y = new JLabel("0");
	private JFrame window;

	private MyCanvas canvas;

	public AWTRobotGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", _TemplateService.class);
		subscribe("publishDisplay", "displayFrame", SerializableImage.class);
		video0.attachGUI(); // default attachment
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", _TemplateService.class);
		unsubscribe("publishDisplay", "displayFrame", SerializableImage.class);
		video0.detachGUI(); // default attachment
	}

	public void displayFrame(SerializableImage img) {
		video0.displayFrame(img);
	}

	public void getState(_TemplateService template) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	@Override
	public void init() {
		video0 = new VideoWidget(boundServiceName, myService, tabs, false);

		video0.init();
		// video0.setNormalizedSize(400,400);

		video0.getDisplay().addMouseListener(this);

		window = new JFrame("Translucent Window");
		canvas = new MyCanvas(window);
		// Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle maxBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int j = 0; j < gs.length; j++) {
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for (int i = 0; i < gc.length; i++) {
				maxBounds = maxBounds.union(gc[i].getBounds());
			}
		}
		canvas.setPreferredSize(new Dimension(maxBounds.width, maxBounds.height));
		window.setUndecorated(true);
		window.setAlwaysOnTop(true);
		canvas.addMouseListener(canvas);
		canvas.addMouseMotionListener(canvas);
		window.setBackground(new Color(0, 0, 0, .01f));
		// window.setOpacity(1f); not visible
		window.setBounds(maxBounds);
		window.add(canvas);
		window.pack();

		display.setLayout(new BorderLayout());
		display.add(video0.getDisplay(), BorderLayout.CENTER);

		JButton butt = new JButton("Set Bounds (drag a rectangle around capture area)");
		butt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				window.setVisible(true);

			}
		});
		JButton butt1 = new JButton("1:1 Image Resize");
		butt1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (canvas != null) {
					Dimension screenDim = new Dimension(canvas.screenRect.width, canvas.screenRect.height);
					myService.send(boundServiceName, "setResize", screenDim);
				}
			}
		});
		display.add(butt1, BorderLayout.NORTH);
		display.add(butt, BorderLayout.SOUTH);
		// display.add(status, BorderLayout.SOUTH);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		// TODO Auto-generated method stub

	}

}
