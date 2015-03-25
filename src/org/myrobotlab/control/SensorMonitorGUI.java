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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.control.widget.JIntegerField;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SensorMonitor;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.Trigger;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

/**
 * @author Gro-G Display data sent to the SensorMonitor service.
 * 
 *         TODO - generalized tracing/triggering TODO - auto-sizing based on
 *         min/max values - sizes screen
 * 
 */
public class SensorMonitorGUI extends ServiceGUI implements ListSelectionListener, VideoGUISource {

	class AddTraceListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {

			JFrame frame = new JFrame();
			frame.setTitle("add new filter");
			String label = JOptionPane.showInputDialog(frame, "new trace name");
			String controllerName = (String) traceController.getSelectedItem();
			Color color = new Color(rand.nextInt(16777215));

			traceListModel.addElement("<html><body><font color=\"" + Integer.toHexString(color.getRGB() & 0x00ffffff) + "\"> " + controllerName + " " + tracePin.getSelectedItem()
					+ " " + label + " </font></body></html>");

			// add the data to the array
			TraceData t = new TraceData();
			t.label = label;
			t.color = color;
			t.controllerName = controllerName;
			t.pin = (Integer) tracePin.getSelectedItem();
			traceData.put(SensorMonitor.makeKey(controllerName, t.pin), t);

			MRLListener MRLListener = new MRLListener("publishPin", boundServiceName, "sensorInput", new Class[] { Pin.class });

			myService.send(controllerName, "addListener", MRLListener);

			// Notification SensorMonitor ------> GUIService
			// subscribe("publishSensorData", "inputSensorData",
			// PinData.class);// TODO-remove
			// already
			// in
			// attachGUI
			// this tells the Arduino to begin analog reads
			myService.send(controllerName, "analogReadPollingStart", (Integer) tracePin.getSelectedItem());

		}

	}

	class AddTriggerListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			JFrame frame = new JFrame();
			frame.setTitle("add new filter");
			// String name = JOptionPane.showInputDialog(frame,
			// "new alert name");

			TriggerDialog triggerDlg = new TriggerDialog();
			if (triggerDlg.action.equals("add")) {
				triggerListModel.addElement(triggerDlg.name.getText() + " " + triggerPin.getSelectedItem() + " " + triggerDlg.threshold.getInt());
				// this has to be pushed to service
				Trigger trigger = new Trigger();
				trigger.name = triggerDlg.name.getText();
				trigger.pinData = new Pin();
				trigger.pinData.source = triggerController.getSelectedItem().toString();
				trigger.pinData.pin = (Integer) triggerPin.getSelectedItem();
				trigger.threshold = triggerDlg.threshold.getInt();
				myService.send(boundServiceName, "addTrigger", trigger);
				// add line ! with name ! & color !
			}

		}

	}

	public class RemoveTraceListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String name = (String) traces.getSelectedValue();
			// myService.send(boundServiceName, "removeFilter", name);
			// TODO - block on response
			traceListModel.removeElement(name);
			String p[] = name.split(" ");
			String controllerName = p[2];
			Integer pin = Integer.parseInt(p[3]);
			myService.send(controllerName, "analogReadPollingStop", pin);
			traceData.remove(SensorMonitor.makeKey(controllerName, pin));
		}

	}

	public class RemoveTriggerListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String name = (String) triggers.getSelectedValue();
			myService.send(boundServiceName, "removeTrigger", name);
			// TODO - block on response
			triggerListModel.removeElement(name);
		}

	}

	class TraceData {
		Color color = null;
		String label;
		String controllerName;
		int pin;
		int data[] = new int[DATA_WIDTH];
		int index = 0;
		int total = 0;
		int max = 0;
		int min = 1024; // TODO - user input on min/max
		int sum = 0;
		int mean = 0;
	}

	class TriggerDialog extends JDialog {
		class TriggerButtonListener implements ActionListener {
			TriggerDialog myDialog = null;

			public TriggerButtonListener(TriggerDialog d) {
				myDialog = d;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				action = e.getActionCommand();
				myDialog.dispose();
			}
		}

		private static final long serialVersionUID = 1L;
		public JTextField name = new JTextField(15);
		public JIntegerField threshold = new JIntegerField(15);
		public JButton add = new JButton("add");

		public JButton cancel = new JButton("cancel");

		public String action = null;

		TriggerDialog() {
			super(myService.getFrame(), "Trigger Dialog", true);

			add.setActionCommand("add");
			cancel.setActionCommand("cancel");
			TriggerButtonListener a = new TriggerButtonListener(this);
			add.addActionListener(a);
			cancel.addActionListener(a);

			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			gc.gridx = 0;
			gc.gridy = 0;
			panel.add(new JLabel("name : "), gc);
			++gc.gridx;
			panel.add(name, gc);

			++gc.gridy;
			gc.gridx = 0;
			panel.add(new JLabel("threshold : "), gc);
			++gc.gridx;
			panel.add(threshold, gc);
			++gc.gridy;
			gc.gridx = 0;

			++gc.gridy;
			gc.gridx = 0;
			panel.add(add, gc);
			++gc.gridx;
			panel.add(cancel, gc);
			++gc.gridy;
			gc.gridx = 0;

			getContentPane().add(panel);
			pack();
			setVisible(true);

		}

	}

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(SensorMonitorGUI.class.toString());

	JList traces;
	JList triggers;

	VideoWidget video = null;
	Thread sensorTraceThread = null;

	// input
	DefaultListModel traceListModel = new DefaultListModel();
	DefaultListModel triggerListModel = new DefaultListModel();
	JButton addTrace = new JButton("add");
	JButton removeTrace = new JButton("remove");

	JButton addTrigger = new JButton("add");
	JButton removeTrigger = new JButton("remove");

	JComboBox traceController = null;
	JComboBox triggerController = null;

	JComboBox tracePin = null;
	JComboBox triggerPin = null;

	BufferedImage sensorImage = null;

	Graphics g = null;

	final int DATA_WIDTH = 320;

	final int DATA_HEIGHT = 512;

	SensorMonitor myBoundService = null;

	// trace data is owned by the GUIService
	HashMap<String, TraceData> traceData = new HashMap<String, TraceData>();
	// trigger data is owned by the Service

	public Random rand = new Random();

	public SensorMonitorGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	public Pin addTraceData(Pin pinData) {
		// add the data to the array
		TraceData t = new TraceData();
		t.label = pinData.source;
		t.color = new Color(rand.nextInt(16777215));
		t.controllerName = pinData.source;
		t.pin = pinData.pin;
		traceData.put(SensorMonitor.makeKey(pinData.source, t.pin), t);
		return pinData;
	}

	@Override
	public void attachGUI() {
		video.attachGUI();
		subscribe("publishState", "getState", SensorMonitor.class);
		subscribe("addTraceData", "addTraceData", Pin.class);
		subscribe("publishSensorData", "inputSensorData", Pin.class);

		// fire the update
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		video.detachGUI();
		unsubscribe("publishState", "getState", SensorMonitor.class);
		unsubscribe("addTraceData", "addTraceData", Pin.class);
		unsubscribe("publishSensorData", "inputSensorData", Pin.class);
	}

	@Override
	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video;
	}

	public void getState(SensorMonitor service) {
		myBoundService = service;
	}

	@Override
	public void init() {

		video = new VideoWidget(boundServiceName, myService, tabs);
		video.init();
		addTrace.addActionListener(new AddTraceListener());
		removeTrace.addActionListener(new RemoveTraceListener());
		addTrigger.addActionListener(new AddTriggerListener());
		removeTrigger.addActionListener(new RemoveTriggerListener());

		sensorImage = new BufferedImage(DATA_WIDTH, DATA_HEIGHT, BufferedImage.TYPE_INT_RGB);
		g = sensorImage.getGraphics();

		traces = new JList(traceListModel);
		traces.setFixedCellWidth(200);
		traces.addListSelectionListener(this);
		traces.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		traces.setSize(140, 160);
		traces.setVisibleRowCount(10);

		JScrollPane tracesScrollPane = new JScrollPane(traces);

		triggers = new JList(triggerListModel);
		triggers.setFixedCellWidth(200);
		triggers.addListSelectionListener(this);
		triggers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// alerts.setSelectedIndex(clist.length-1);
		triggers.setSize(140, 160);
		triggers.setVisibleRowCount(10);

		JScrollPane triggersScrollPane = new JScrollPane(triggers);

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 2;
		display.add(video.display, gc);
		gc.gridheight = 1;

		// trace begin ----------------------
		JPanel trace = new JPanel(new GridBagLayout());
		trace.setBorder(BorderFactory.createTitledBorder("trace"));

		trace.add(addTrace, gc);

		++gc.gridx;
		ArrayList<String> v = Runtime.getServiceNamesFromInterface(SensorDataPublisher.class);
		traceController = new JComboBox(v.toArray());
		trace.add(traceController, gc);

		// TODO - lame, pin config is based on Arduino D.
		Vector<Integer> p = new Vector<Integer>();
		p.addElement(0);
		p.addElement(1);
		p.addElement(2);
		p.addElement(3);
		p.addElement(4);
		p.addElement(5);
		p.addElement(6);
		p.addElement(7);
		p.addElement(8);
		p.addElement(9);
		p.addElement(10);
		p.addElement(11);
		p.addElement(12);
		p.addElement(13);
		p.addElement(14);
		p.addElement(15);
		p.addElement(16);
		p.addElement(17);
		p.addElement(18);
		p.addElement(19);

		tracePin = new JComboBox(p);

		++gc.gridx;
		trace.add(tracePin, gc);

		++gc.gridx;
		trace.add(removeTrace, gc);

		gc.gridx = 0;
		++gc.gridy;
		gc.gridwidth = 4;
		trace.add(tracesScrollPane, gc);

		// reusing gc for main panel

		gc.gridwidth = 1;
		gc.gridx = 1;
		gc.gridy = 0;
		display.add(trace, gc);

		// trace end -------------------------

		// trigger begin ----------------------
		JPanel triggerPanel = new JPanel(new GridBagLayout());
		triggerPanel.setBorder(BorderFactory.createTitledBorder("trigger"));

		triggerPanel.add(addTrigger, gc);

		++gc.gridx;
		triggerController = new JComboBox(v.toArray());
		triggerPanel.add(triggerController, gc);

		triggerPin = new JComboBox(p);

		++gc.gridx;
		triggerPanel.add(triggerPin, gc);

		++gc.gridx;
		triggerPanel.add(removeTrigger, gc);

		gc.gridx = 0;
		++gc.gridy;
		gc.gridwidth = 4;
		triggerPanel.add(triggersScrollPane, gc);

		// reusing gc for main panel

		gc.gridwidth = 1;
		gc.gridx = 1;
		gc.gridy = 1;
		display.add(triggerPanel, gc);

		// trigger end -------------------------

		setCurrentFilterMouseListener();
		// build filters end ------------------

	}

	/**
	 * method which displays the data published by the SensorMonitor on the
	 * video widget
	 * 
	 * @param pinData
	 */
	public void inputSensorData(Pin pinData) {
		// update trace array & trigger array if applicable
		// myService.logTime("start");
		String key = SensorMonitor.makeKey(pinData);

		if (!traceData.containsKey(key)) {
			addTraceData(pinData);
		}
		TraceData t = traceData.get(key);
		t.index++;
		t.data[t.index] = pinData.value;
		++t.total;
		t.sum += pinData.value;
		t.mean = t.sum / t.total;

		g.setColor(t.color);
		// g.drawRect(20, t.pin * 15 + 5, 200, 15);
		g.drawLine(t.index, DATA_HEIGHT - t.data[t.index - 1] / 2, t.index, DATA_HEIGHT - pinData.value / 2);

		// computer min max and mean
		// if different then blank & post to screen
		if (pinData.value > t.max)
			t.max = pinData.value;
		if (pinData.value < t.min)
			t.min = pinData.value;

		if (t.index < DATA_WIDTH - 1) {
		} else {
			t.index = 0;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, DATA_WIDTH, DATA_HEIGHT);
			g.setColor(Color.GRAY);
			g.drawLine(0, DATA_HEIGHT - 25, DATA_WIDTH - 1, DATA_HEIGHT - 25);
			g.drawString("50", 10, DATA_HEIGHT - 25);
			g.drawLine(0, DATA_HEIGHT - 50, DATA_WIDTH - 1, DATA_HEIGHT - 50);
			g.drawString("100", 10, DATA_HEIGHT - 50);
			g.drawLine(0, DATA_HEIGHT - 100, DATA_WIDTH - 1, DATA_HEIGHT - 100);
			g.drawString("200", 10, DATA_HEIGHT - 100);
			g.drawLine(0, DATA_HEIGHT - 200, DATA_WIDTH - 1, DATA_HEIGHT - 200);
			g.drawString("400", 10, DATA_HEIGHT - 200);
			g.drawLine(0, DATA_HEIGHT - 300, DATA_WIDTH - 1, DATA_HEIGHT - 300);
			g.drawString("600", 10, DATA_HEIGHT - 300);
			g.drawLine(0, DATA_HEIGHT - 400, DATA_WIDTH - 1, DATA_HEIGHT - 400);
			g.drawString("800", 10, DATA_HEIGHT - 400);

			g.setColor(Color.BLACK);
			g.fillRect(20, t.pin * 15 + 5, 200, 15);
			g.setColor(t.color);
			g.drawString(" min " + t.min + " max " + t.max + " mean " + t.mean + " total " + t.total + " sum " + t.sum, 20, t.pin * 15 + 20);

		}

		video.displayFrame(new SerializableImage(sensorImage, boundServiceName));
	}

	public void setCurrentFilterMouseListener() {
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					if (index >= 0) {
						Object o = theList.getModel().getElementAt(index);
						System.out.println("Double-clicked on: " + o.toString());
					}
				}
			}
		};

		traces.addMouseListener(mouseListener);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
	}

}
