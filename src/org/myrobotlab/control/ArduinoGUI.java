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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.DigitalButton;
import org.myrobotlab.control.widget.EditorArduino;
import org.myrobotlab.control.widget.JIntegerField;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.Pin;

/*
 * TODO - move menu into ArduinoGUI from editor
 *      - synch up repo with createLabs
 *      - correct pin state on menu
 * 		- make Communication -> menu -> MRLComm.ino
 *      - make menu builder
 *      - auto-load - MRLComm first
 *      - refresh serial ?
 *      - message syphone - message pump - stdout stdin pipes process creator etc...
 *      - all traces start stop at same time
 *      - 100% on compile & upload
 *      - arrow changed for upload to "up" duh
 *      - incoming pin data -> determines state of inactive/active & oscope pin update
 *      
 *      - Java console - duh
 *      - uploader progress - duh
 *      - error goes to status	
 *      - console info regarding the state & progress of "connecting" to a serialDevice
 *      - TODO - "errorMessage vs message" warnMessage too - embed in Console logic
 *      
 */

public class ArduinoGUI extends ServiceGUI implements ItemListener, ActionListener, TabControlEventHandler {

	public ArduinoGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		self = this;
	}

	/**
	 * component array - to access all components by name
	 */
	public Arduino myArduino;
	public ArduinoGUI self;
	HashMap<String, Component> components = new HashMap<String, Component>();

	static final long serialVersionUID = 1L;
	// FIXME - you need a pattern or a new Menu
	// A Menu in the ArduinoGUI versus the Arduino Editor
	private JMenuItem serialRefresh = new JMenuItem("refresh");
	private JMenuItem softReset = new JMenuItem("soft reset");
	private JMenuItem serialDisconnect = new JMenuItem("disconnect");

	JTabbedPane tabs = new JTabbedPane();

	/*
	 * ---------- Pins begin -------------------------
	 */
	JLayeredPane imageMap;
	/*
	 * ---------- Pins end -------------------------
	 */

	/*
	 * ---------- Config begin -------------------------
	 */

	JIntegerField rawReadMsgLength = new JIntegerField(4);
	JCheckBox rawReadMessage = new JCheckBox();
	/*
	 * ---------- Config end -------------------------
	 */

	/*
	 * ---------- Oscope begin -------------------------
	 */
	/**
	 * array list of graphical pin components built from pinList
	 */
	ArrayList<PinComponent> pinComponentList = null;
	SerializableImage sensorImage = null;
	Graphics g = null;
	VideoWidget oscope = null;
	JPanel oscopePanel = null;
	/*
	 * ---------- Oscope end -------------------------
	 */

	/*
	 * ---------- Editor begin -------------------------
	 */
	// Base arduinoIDE;
	DigitalButton uploadButton = null;
	GridBagConstraints epgc = new GridBagConstraints();
	Dimension size = new Dimension(620, 512);
	Map<String, String> boardPreferences;
	String boardName;
	// JCheckBoxMenuItem serialDevice;
	SerialMenuListener serialMenuListener = new SerialMenuListener();

	/*
	 * ---------- Editor end -------------------------
	 */

	/**
	 * pinList - from Arduino
	 */
	ArrayList<Pin> pinList = null;

	public void init() {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				display.setLayout(new BorderLayout());

				// ---------------- tabs begin ----------------------
				tabs.setTabPlacement(JTabbedPane.RIGHT);

				getPinPanel();
				getOscopePanel();
				getEditorPanel();

				display.add(tabs, BorderLayout.CENTER);
				//tabs.setSelectedIndex(0);

				serialRefresh.addActionListener(self);
				softReset.addActionListener(self);
				serialDisconnect.addActionListener(self);

			}
		});
	}

	public void getPinPanel() {

		if (myArduino != null && boardName != null && boardName.contains("Mega")) {
			getMegaPanel();
			return;
		}
		getDuemilanovePanel();
	}

	class SerialMenuListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JRadioButtonMenuItem checkbox = (JRadioButtonMenuItem) e.getSource();
			if (checkbox.isSelected()) {
				myService.send(boundServiceName, "connect", checkbox.getText(), 57600, 8, 1, 0);
			} else {
				myService.send(boundServiceName, "disconnect");
			}

		}
	}

	/**
	 * getState is called when the Arduino service changes state information
	 * FIXME - this method is often called by other threads so gui - updates
	 * must be done in the swing post method way
	 * 
	 * @param arduino
	 */
	public void getState(final Arduino arduino) { // TODO - all getState data
													// should be final
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				log.info("getState Arduino");
				if (arduino != null) {
					myArduino = arduino; // FIXME - super updates registry state
											// ?
					boardPreferences = myArduino.getBoardPreferences(); // FIXME
																		// -
																		// class
																		// member
																		// has
																		// precedence
																		// - do
																		// away
																		// with
																		// properties
																		// !
					boardName = boardPreferences.get("name"); // FIXME - class
																// member has
																// precedence -
																// do away with
																// properties !
					pinList = myArduino.getPinList();

					// update panels based on state change
					// TODO - check what state the panels are to see if a
					// change is needed
					getPinPanel();
					getOscopePanel();

					editor.serialDeviceMenu.removeAll();
					publishMessage(String.format("found %d serial ports", myArduino.portNames.size()));
					for (int i = 0; i < myArduino.portNames.size(); ++i) {
						String portName = myArduino.portNames.get(i);
						publishMessage(String.format(" %s", portName));

						JRadioButtonMenuItem serialDevice = new JRadioButtonMenuItem(myArduino.portNames.get(i));
						SerialDevice sd = myArduino.getSerialDevice();
						if (sd != null && sd.getName().equals(portName)) {
							if (sd.isOpen()) {
								editor.connectButton.activate();
								serialDevice.setSelected(true);
							} else {
								editor.connectButton.deactivate();
								serialDevice.setSelected(false);
							}
						} else {
							serialDevice.setSelected(false);
						}
						serialDevice.addActionListener(serialMenuListener);
						editor.serialDeviceMenu.add(serialDevice);
						// editor.getTextArea().setText(arduino.getSketch());

						// if the service has a different sketch update the gui
						// TODO - kinder - gentler - ask user if they want the
						// update
					}
					if (!editor.getTextArea().equals(arduino.getSketch())) {
						editor.getTextArea().setText(arduino.getSketch());
					}

				}

				// TODO - work on generalizing editor
				editor.serialDeviceMenu.add(serialRefresh);
				editor.serialDeviceMenu.add(serialDisconnect);
				editor.serialDeviceMenu.add(softReset);

				String statusString = boardName + " " + myArduino.preferences.get("serial.port");
				editor.setStatus(statusString);

			}
		});

	}

	public void setCompilingProgress(Integer percent) {
		editor.progress.setValue(percent);
	}

	public void compilerError(String msg) {
		editor.status.setText(msg);
	}

	public void publishMessage(String msg) {
		if (editor != null) {
			editor.console.append(msg);
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishPin", "publishPin", Pin.class);
		subscribe("publishState", "getState", Arduino.class);
		subscribe("publishCompilingProgress", "setCompilingProgress", Integer.class);
		subscribe("publishMessage", "publishMessage", String.class);
		subscribe("compilerError", "compilerError", String.class);
		subscribe("getPorts", "getPorts", String.class);
		// subscribe("setBoard", "setBoard", String.class);
		// myService.send(boundServiceName, "broadcastState");

		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishPin", "publishPin", Pin.class);
		unsubscribe("publishState", "getState", Arduino.class);
		unsubscribe("publishCompilingProgress", "setCompilingProgress", Integer.class);
		unsubscribe("publishMessage", "publishMessage", String.class);
		unsubscribe("compilerError", "compilerError", String.class);
	}

	@Override
	public void itemStateChanged(ItemEvent item) {
		{
			// called when the button is pressed
			JCheckBox cb = (JCheckBox) item.getSource();
			// Determine status
			boolean isSel = cb.isSelected();
			if (isSel) {
				myService.send(boundServiceName, "setRawReadMsg", true);
				myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(false);
			} else {
				myService.send(boundServiceName, "setRawReadMsg", false);
				myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(true);
			}
		}
	}

	/**
	 * The guts of the business logic of handling all the graphical components
	 * and their relations with each other.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		String cmd = e.getActionCommand();
		Component c = (Component) e.getSource();

		if (o == serialRefresh) {

			myService.send(boundServiceName, "getPortNames");
			myService.send(boundServiceName, "publishState");
			return;

		}

		if (o == softReset) {

			myService.send(boundServiceName, "softReset");
			return;
		}

		if (o == serialDisconnect) {

			myService.send(boundServiceName, "disconnect");
			return;
		}

		// buttons
		if (DigitalButton.class == o.getClass()) {
			DigitalButton b = (DigitalButton) o;

			if (uploadButton == c) {
				uploadButton.toggle();
				return;
			}

			PinComponent pin = null;
			int address = -1;
			int value = -1;

			if (b.parent != null) {
				address = ((PinComponent) b.parent).pinNumber;
				pin = ((PinComponent) b.parent);
			}

			if (b.type == PinComponent.TYPE_ONOFF) {
				if ("off".equals(cmd)) {
					// now on
					value = PinComponent.HIGH;
					myService.send(boundServiceName, "digitalWrite", address, value);
					b.toggle();
				} else {
					// now off
					value = PinComponent.LOW;
					myService.send(boundServiceName, "digitalWrite", address, value);
					b.toggle();
				}

			} else if (b.type == PinComponent.TYPE_INOUT) {
				if ("out".equals(cmd)) {
					// is now input
					value = PinComponent.INPUT;
					myService.send(boundServiceName, "pinMode", address, value);
					myService.send(boundServiceName, "digitalReadPollStart", address);
					b.toggle();
				} else if ("in".equals(cmd)) {
					// is now output
					value = PinComponent.OUTPUT;
					myService.send(boundServiceName, "pinMode", address, value);
					myService.send(boundServiceName, "digitalReadPollStop", address);
					b.toggle();
				} else {
					log.error(String.format("unknown digital pin cmd %s", cmd));
				}
			} else if (b.type == PinComponent.TYPE_TRACE || b.type == PinComponent.TYPE_ACTIVEINACTIVE) {

				// digital pin
				if (!pin.isAnalog) {
					if (!pin.inOut.isOn) { // pin is off turn it on
						value = PinComponent.INPUT;
						myService.send(boundServiceName, "pinMode", address, value);
						myService.send(boundServiceName, "digitalReadPollStart", address);
						pin.inOut.setOn(); // in
						b.setOn();
					} else {
						value = PinComponent.OUTPUT;
						myService.send(boundServiceName, "pinMode", address, value);
						myService.send(boundServiceName, "digitalReadPollStop", address);
						pin.inOut.setOff();// out
						b.setOff();
					}
				} else {
					value = PinComponent.INPUT;
					myService.send(boundServiceName, "pinMode", address, value);
					// analog pin
					if (pin.activeInActive.isOn) {
						myService.send(boundServiceName, "analogReadPollingStop", address);
						pin.activeInActive.setOff();
						pin.trace.setOff();
						b.setOff();
					} else {
						myService.send(boundServiceName, "analogReadPollingStart", address);
						pin.activeInActive.setOn();
						pin.trace.setOn();
						b.setOn();
					}
				}

			} else {
				log.error("unknown pin type " + b.type);
			}

			log.info("DigitalButton");
		}

	}

	public void closeSerialDevice() {
		myService.send(boundServiceName, "closeSerialDevice");
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
		int traceStart = 0;
	}

	int DATA_WIDTH = size.width;
	int DATA_HEIGHT = size.height;
	HashMap<Integer, TraceData> traceData = new HashMap<Integer, TraceData>();
	int clearX = 0;
	int lastTraceXPos = 0;

	public void publishPin(final Pin pin) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (!traceData.containsKey(pin.pin)) {
					TraceData td = new TraceData();
					float gradient = 1.0f / pinComponentList.size();
					Color color = new Color(Color.HSBtoRGB((pin.pin * (gradient)), 0.8f, 0.7f));
					td.color = color;
					traceData.put(pin.pin, td);
					td.index = lastTraceXPos;
				}

				TraceData t = traceData.get(pin.pin);
				t.index++;
				lastTraceXPos = t.index;
				t.data[t.index] = pin.value;
				++t.total;
				t.sum += pin.value;
				t.mean = t.sum / t.total;

				g.setColor(t.color);
				if (pin.type == Pin.DIGITAL_VALUE || pin.type == Pin.PWM_VALUE) {
					int yoffset = pin.pin * 15 + 35;
					int quantum = -10;
					g.drawLine(t.index, t.data[t.index - 1] * quantum + yoffset, t.index, pin.value * quantum + yoffset);
				} else if (pin.type == Pin.ANALOG_VALUE) {
					g.drawLine(t.index, DATA_HEIGHT - t.data[t.index - 1] / 2, t.index, DATA_HEIGHT - pin.value / 2);
				} else {
					log.error("dont know how to display pin data method");
				}

				// computer min max and mean
				// if different then blank & post to screen
				if (pin.value > t.max)
					t.max = pin.value;
				if (pin.value < t.min)
					t.min = pin.value;

				if (t.index < DATA_WIDTH - 1) {
					clearX = t.index + 1;
				} else {
					// TODO - when hit marks all startTracePos - cause the
					// screen is
					// blank - must iterate through all
					t.index = 0;

					clearScreen();
					drawGrid();

					g.setColor(Color.BLACK);
					g.fillRect(20, t.pin * 15 + 5, 200, 15);
					g.setColor(t.color);

					g.drawString(String.format("min %d max %d mean %d ", t.min, t.max, t.mean), 20, t.pin * 15 + 20);

					t.total = 0;
					t.sum = 0;

				}

				oscope.displayFrame(sensorImage);

			}
		});

	}

	public void clearScreen() // TODO - static - put in oscope/image package
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, DATA_WIDTH, DATA_HEIGHT); // TODO - ratio - to expand
													// or reduce view
	}

	public void drawGrid() // TODO - static & put in oscope/image package
	{
		g.setColor(Color.DARK_GRAY);
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

	}

	/**
	 * Spew the contents of a String object out to a file.
	 */
	static public void saveFile(String str, File file) throws IOException {
		File temp = File.createTempFile(file.getName(), null, file.getParentFile());
		// PApplet.saveStrings(temp, new String[] { str }); FIXME
		if (file.exists()) {
			boolean result = file.delete();
			if (!result) {
				throw new IOException("Could not remove old version of " + file.getAbsolutePath());
			}
		}
		boolean result = temp.renameTo(file);
		if (!result) {
			throw new IOException("Could not replace " + file.getAbsolutePath());
		}
	}

	/**
	 * Get the number of lines in a file by counting the number of newline
	 * characters inside a String (and adding 1).
	 */
	static public int countLines(String what) {
		int count = 1;
		for (char c : what.toCharArray()) {
			if (c == '\n')
				count++;
		}
		return count;
	}

	public void getMegaPanel() {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (imageMap != null) {
					tabs.remove(imageMap);
				}

				pinComponentList = new ArrayList<PinComponent>();
				imageMap = new JLayeredPane();
				imageMap.setPreferredSize(size);

				// set correct arduino image
				JLabel image = new JLabel();

				ImageIcon dPic = Util.getImageIcon("Arduino/mega.200.pins.png");
				image.setIcon(dPic);
				Dimension s = image.getPreferredSize();
				image.setBounds(0, 0, s.width, s.height);
				imageMap.add(image, new Integer(1));

				for (int i = 0; i < 70; ++i) {

					PinComponent p = null;

					if (i > 1 && i < 14) { // pwm pins -----------------
						p = new PinComponent(myService, boundServiceName, i, true, false, true);
						int xOffSet = 0;
						if (i > 7)
							xOffSet = 18; // skip pin
						p.inOut.setBounds(252 - 18 * i - xOffSet, 30, 15, 30);
						imageMap.add(p.inOut, new Integer(2));
						p.onOff.setBounds(252 - 18 * i - xOffSet, 0, 15, 30);
						// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
						imageMap.add(p.onOff, new Integer(2));

						if (p.isPWM) {
							p.pwmSlider.setBounds(252 - 18 * i - xOffSet, 75, 15, 90);
							imageMap.add(p.pwmSlider, new Integer(2));
							p.data.setBounds(252 - 18 * i - xOffSet, 180, 32, 15);
							p.data.setForeground(Color.white);
							p.data.setBackground(Color.decode("0x0f7391"));
							p.data.setOpaque(true);
							imageMap.add(p.data, new Integer(2));
						}
					} else if (i < 54 && i > 21) {
						// digital pin racks
						p = new PinComponent(myService, boundServiceName, i, false, false, false);

						if (i != 23 && i != 25 && i != 27 && i != 29) {
							if ((i % 2 == 0)) {
								// first rack of digital pins
								p.inOut.setBounds(472, 55 + 9 * (i - 21), 30, 15);
								imageMap.add(p.inOut, new Integer(2));
								p.onOff.setBounds(502, 55 + 9 * (i - 21), 30, 15);
								// p.onOff.getLabel().setUI(new
								// VerticalLabelUI(true));
								imageMap.add(p.onOff, new Integer(2));
							} else {
								// second rack of digital pins
								p.inOut.setBounds(567, 45 + 9 * (i - 21), 30, 15);
								imageMap.add(p.inOut, new Integer(2));
								p.onOff.setBounds(597, 45 + 9 * (i - 21), 30, 15);
								// p.onOff.getLabel().setUI(new
								// VerticalLabelUI(true));
								imageMap.add(p.onOff, new Integer(2));
							}
						}

					} else if (i > 53) {
						p = new PinComponent(myService, boundServiceName, i, false, true, true);
						// analog pins -----------------
						int xOffSet = 0;
						if (i > 61)
							xOffSet = 18; // skip pin
						p.activeInActive.setBounds(128 + 18 * (i - 52) + xOffSet, 392, 15, 48);
						imageMap.add(p.activeInActive, new Integer(2));
						/*
						 * bag data at the moment - go look at the Oscope
						 * p.data.setBounds(208 + 18 * (i - 52) + xOffSet, 260,
						 * 32, 18); p.data.setForeground(Color.white);
						 * p.data.setBackground(Color.decode("0x0f7391"));
						 * p.data.setOpaque(true); imageMap.add(p.data, new
						 * Integer(2));
						 */
					} else {
						p = new PinComponent(myService, boundServiceName, i, false, false, false);
					}

					// set up the listeners
					p.onOff.addActionListener(self);
					p.inOut.addActionListener(self);
					p.activeInActive.addActionListener(self);
					p.trace.addActionListener(self);
					// p.inOut2.addActionListener(this);

					pinComponentList.add(p);

				}

				JFrame top = myService.getFrame();
				tabs.insertTab("pins", null, imageMap, "pin panel", 0);
				GUIService gui = (GUIService) myService;// FIXME - bad bad bad
				
				//TabControl2(TabControlEventHandler handler, JTabbedPane tabs, Container myPanel, String label)
				tabs.setTabComponentAt(0, new TabControl2(self, tabs, imageMap, "pins"));
				
			}
		});
	}

	// public

	public void getDuemilanovePanel() {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (imageMap != null) {
					tabs.remove(imageMap);
				}

				imageMap = new JLayeredPane();
				imageMap.setPreferredSize(size);
				pinComponentList = new ArrayList<PinComponent>();

				// set correct arduino image
				JLabel image = new JLabel();

				ImageIcon dPic = Util.getImageIcon("Arduino/arduino.duemilanove.200.pins.png");
				image.setIcon(dPic);
				Dimension s = image.getPreferredSize();
				image.setBounds(0, 0, s.width, s.height);
				imageMap.add(image, new Integer(1));

				for (int i = 0; i < 20; ++i) {

					PinComponent p = null;
					if (i < 14) {
						if (((i == 3) || (i == 5) || (i == 6) || (i == 9) || (i == 10) || (i == 11))) {
							p = new PinComponent(myService, boundServiceName, i, true, false, false);
						} else {
							p = new PinComponent(myService, boundServiceName, i, false, false, false);
						}
					} else {
						p = new PinComponent(myService, boundServiceName, i, false, true, false);
					}

					// set up the listeners
					p.onOff.addActionListener(self);
					p.inOut.addActionListener(self);
					p.activeInActive.addActionListener(self);
					p.trace.addActionListener(self);
					// p.inOut2.addActionListener(this);

					pinComponentList.add(p);

					if (i < 2) {
						continue;
					}
					if (i < 14) { // digital pins -----------------
						int yOffSet = 0;
						if (i > 7)
							yOffSet = 18; // skip pin
						p.inOut.setBounds(406, 297 - 18 * i - yOffSet, 30, 15);
						imageMap.add(p.inOut, new Integer(2));
						p.onOff.setBounds(436, 297 - 18 * i - yOffSet, 30, 15);
						// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
						imageMap.add(p.onOff, new Integer(2));

						if (p.isPWM) {
							p.pwmSlider.setBounds(256, 297 - 18 * i - yOffSet, 90, 15);
							imageMap.add(p.pwmSlider, new Integer(2));
							p.data.setBounds(232, 297 - 18 * i - yOffSet, 32, 15);
							p.data.setForeground(Color.white);
							p.data.setBackground(Color.decode("0x0f7391"));
							p.data.setOpaque(true);
							imageMap.add(p.data, new Integer(2));
						}
					} else {
						// analog pins -----------------
						p.activeInActive.setBounds(11, 208 - 18 * (14 - i), 48, 15);
						imageMap.add(p.activeInActive, new Integer(2));
						p.data.setBounds(116, 205 - 18 * (14 - i), 32, 18);
						p.data.setForeground(Color.white);
						p.data.setBackground(Color.decode("0x0f7391"));
						p.data.setOpaque(true);
						imageMap.add(p.data, new Integer(2));
					}
				}

				JFrame top = myService.getFrame();
				tabs.insertTab("pins", null, imageMap, "pin panel", 0);
				GUIService gui = (GUIService) myService;// FIXME - bad bad bad
														// ...

				// FIXME TabControl2 - tabs.setTabComponentAt(0, new
				// TabControl(gui,
				// tabs, imageMap, boundServiceName, "pins"));
			}
		});
	}

	public void getOscopePanel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (oscopePanel != null) {
					tabs.remove(oscopePanel);
				}

				// CREATE SERVICE GUI !!! 
				oscopePanel = new JPanel(new GridBagLayout());
				GridBagConstraints opgc = new GridBagConstraints();

				JPanel tracePanel = new JPanel(new GridBagLayout());

				opgc.fill = GridBagConstraints.HORIZONTAL;
				opgc.gridx = 0;
				opgc.gridy = 0;
				float gradient = 1.0f / pinComponentList.size();

				// pinList.size() mega 60 deuo 20
				for (int i = 0; i < pinComponentList.size(); ++i) {
					PinComponent p = pinComponentList.get(i);
					if (!p.isAnalog) { // digital pins -----------------
						p.trace.setText("D " + (i));
						p.trace.onText = "D " + (i);
						p.trace.offText = "D " + (i);
					} else {
						// analog pins ------------------
						p.trace.setText("A " + (i - 14));
						p.trace.onText = "A " + (i - 14);
						p.trace.offText = "A " + (i - 14);
					}
					tracePanel.add(p.trace, opgc);
					Color hsv = new Color(Color.HSBtoRGB((i * (gradient)), 0.8f, 0.7f));
					p.trace.setBackground(hsv);
					p.trace.offBGColor = hsv;
					++opgc.gridy;
					if (opgc.gridy % 20 == 0) {
						opgc.gridy = 0;
						++opgc.gridx;
					}
				}

				opgc.gridx = 0;
				opgc.gridy = 0;

				oscope = new VideoWidget(boundServiceName, myService, tabs, false);
				oscope.init();
				sensorImage = new SerializableImage(new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB), "output");
				g = sensorImage.getImage().getGraphics();
				oscope.displayFrame(sensorImage);

				oscopePanel.add(tracePanel, opgc);
				++opgc.gridx;
				oscopePanel.add(oscope.display, opgc);

				JFrame top = myService.getFrame();
				tabs.insertTab("oscope", null, oscopePanel, "oscope panel", 0);
				tabs.setTabComponentAt(0, new TabControl2(self, tabs, oscopePanel, "oscope"));
				myService.getFrame().pack();
			}
		});
	}

	EditorArduino editor = null;

	public void getEditorPanel() {
		editor = new EditorArduino(boundServiceName, myService, tabs);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// if (editorPanel != null) {
				// tabs.remove(editorPanel);
				// }

				// editorPanel = new JPanel(new BorderLayout());

				
				editor.init();
				// editorPanel.add(editor.getDisplay());

				JFrame top = myService.getFrame();
				tabs.insertTab("editor", null, editor.getDisplay(), "editor", 0);
				GUIService gui = (GUIService) myService;// FIXME - bad bad bad
														// ...

				// FIXME TabControl2 - tabs.setTabComponentAt(0, new
				// TabControl(gui,
				// tabs, editor.getDisplay(), boundServiceName, "editor"));
				myService.getFrame().pack();
			}
		});
	}

	public void createSerialDeviceMenu(JMenu m) {
		for (int i = 0; i < myArduino.portNames.size(); ++i) {
			// m.add(a)
		}
	}

}
