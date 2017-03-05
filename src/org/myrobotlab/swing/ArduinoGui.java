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

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.swing.widget.DigitalButton;
import org.myrobotlab.swing.widget.FileChooser;
import org.myrobotlab.swing.widget.PortGui;

public class ArduinoGui extends ServiceGui
		implements ActionListener, TabControlEventHandler, ItemListener, PortListener {

	static final long serialVersionUID = 1L;

	static final String[] BOARD_TYPES = new String[] { "uno", "mega", "megaadk", "nano" };

	// FIXME wigetize oscope
	// separate sub-tabs

	JLabel status = new JLabel("disconnected");

	// FIXME - double buffer 2 JPanels
	// FIXME - wigetize
	class TraceData {
		Color color = null;
		String controllerName;
		int data[] = new int[DATA_WIDTH];
		int index = 0;
		String label;
		int max = 0;
		int mean = 0;
		int min = 1024; // TODO - user input on min/max
		int pin;
		int sum = 0;
		int total = 0;
		int traceStart = 0;
	}

	FileChooser chooser = null;

	PortGui portGui;

	JComboBox<String> boardTypes = new JComboBox<String>(BOARD_TYPES);

	int DATA_WIDTH = 400;
	int DATA_HEIGHT = 512;

	Graphics g = null;

	int lastTraceXPos = 0;
	Arduino myArduino;

	VideoWidget oscope = null;

	/**
	 * array list of graphical pin components built from pinList
	 */
	ArrayList<PinComponent> pinComponentList = null;

	List<PinDefinition> pinList = null;
	ArduinoGui self;

	SerializableImage sensorImage = null;

	Dimension size = new Dimension(DATA_WIDTH, DATA_HEIGHT);

	JMenuItem softReset = new JMenuItem("soft reset");

	JTabbedPane localTabs = new JTabbedPane();

	HashMap<Integer, TraceData> traceData = new HashMap<Integer, TraceData>();

	JButton openMrlComm = new JButton("Open in Arduino IDE");

	JTextField arduinoPath = new JTextField(20);

	JTextField boardType = new JTextField(5);
	JButton uploadMrlComm = new JButton("Upload MrlComm");
	JLabel uploadResult = new JLabel();

	transient TextEditorPane editor;

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

	public ArduinoGui(final String boundServiceName, final SwingGui myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		self = this;

		// FIXME - this should be done in ServiceGui ! - it should auto-update
		// onState in ServiceGui
		// and ServiceGui should call overloaded method
		Arduino arduino = (Arduino) Runtime.getService(boundServiceName);
		SerialDevice serial = arduino.getSerial();
		portGui = new PortGui(serial.getName(), myService, tabs);

		addTop(3, portGui.getDisplay());

		// addTop(" board:", boardTypes);
		addTop(status);

		localTabs.setTabPlacement(SwingConstants.RIGHT);
		localTabs.setPreferredSize(new Dimension(300, 300));

		addPinPanel();
		addOscopePanel();
		addMrlCommPanel();

		addLine(localTabs);

		softReset.addActionListener(self);
		boardTypes.addItemListener(self);
		openMrlComm.addActionListener(self);
		uploadMrlComm.addActionListener(self);
	}

	/**
	 * The guts of the business logic of handling all the graphical components
	 * and their relations with each other.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		String cmd = e.getActionCommand();

		if (o == softReset) {
			myService.send(boundServiceName, "softReset");
			return;
		}

		// buttons
		if (DigitalButton.class == o.getClass()) {
			DigitalButton b = (DigitalButton) o;

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
					myService.send(boundServiceName, "enablePin", address);
					b.toggle();
				} else if ("in".equals(cmd)) {
					myService.send(boundServiceName, "disablePin", address);
					b.toggle();
				} else {
					log.error(String.format("unknown digital pin cmd %s", cmd));
				}
			} else if (b.type == PinComponent.TYPE_TRACE || b.type == PinComponent.TYPE_ACTIVEINACTIVE) {

				// digital pin
				if (!pin.isAnalog) {
					if (!pin.inOut.isOn) { // pin is off turn it on
						myService.send(boundServiceName, "enablePin", address);
						pin.inOut.setOn(); // in
						b.setOn();
					} else {
						myService.send(boundServiceName, "diablePin", address);
						pin.inOut.setOff();// out
						b.setOff();
					}
				} else {
					value = PinComponent.INPUT;
					myService.send(boundServiceName, "pinMode", address, value);
					// analog pin
					if (pin.activeInActive.isOn) {
						myService.send(boundServiceName, "enablePin", address);
						pin.activeInActive.setOff();
						pin.trace.setOff();
						b.setOff();
					} else {
						myService.send(boundServiceName, "diablePin", address);
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
		if (o == openMrlComm) {
			myService.send(boundServiceName, "setArduinoPath", arduinoPath.getText());
			myService.send(boundServiceName, "openMrlComm");
		}

		if (o == uploadMrlComm) {
			// step 1 check to see if all input fields are valid
			String path = arduinoPath.getText();
			File dir = new File(path);
			if (!dir.exists() || !dir.isDirectory()) {
				error(String.format("%s is not a valid directory", path));
				return;
			}
			String port = portGui.getSelected();
			if (port == null || port.equals("")) {
				error("please set port");
				return;
			}
			myService.send(boundServiceName, "setArduinoPath", arduinoPath.getText());
			uploadResult.setText("Uploading Sketch");
			myService.send(boundServiceName, "uploadSketch", arduinoPath.getText(), port, boardType.getText());
		}
	}

	public void onDisconnect(String portName) {
		openMrlComm.setEnabled(true);
		arduinoPath.setText(myArduino.arduinoPath);
		status.setText("disconnected");
	}

	public void onConnect(String portName) {
		openMrlComm.setEnabled(false);
	}

	@Override
	public void subscribeGui() {
		subscribe("publishBoardInfo");
		subscribe("publishPin", "publishPin");
		subscribe("publishConnect");
		subscribe("publishDisconnect");
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("publishBoardInfo");
		unsubscribe("publishPin", "publishPin");
		unsubscribe("publishConnect");
		unsubscribe("publishDisconnect");
	}

	public void onBoardInfo(BoardInfo boardInfo) {
		status.setText(String.format("connected %s", boardInfo));
	}

	public void clearScreen() // TODO - static - put in oscope/image package
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, DATA_WIDTH, DATA_HEIGHT); // TODO - ratio - to expand
		// or reduce view
	}

	// FIXME - scroll line & blackness ! (blit offscreen?)
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

	public void getDuemilanovePanel() {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JLayeredPane imageMap = new JLayeredPane();
				pinComponentList = new ArrayList<PinComponent>();

				JLabel image = new JLabel();

				ImageIcon dPic = Util.getImageIcon("Arduino/uno.png");
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
				localTabs.insertTab("pins", null, imageMap, "pin panel", 0);
			}
		});
	}

	public void addMrlCommPanel() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JPanel uploadPanel = new JPanel(new BorderLayout());

				String pathToMrlComm = null;
				String mrlIno = null;
				try {
					pathToMrlComm = "resource/Arduino/MrlComm/MrlComm.ino";
					mrlIno = FileIO.toString(pathToMrlComm);
				} catch (Exception e) {
				}
				try {
					if (mrlIno == null) {
						pathToMrlComm = "src/resource/Arduino/MrlComm/MrlComm.ino";
						mrlIno = FileIO.toString(pathToMrlComm);
					}
				} catch (Exception e) {
				}

				// JPanel north = createFlowPanel(null, openMrlComm, "Arduino
				// IDE Path ", arduinoPath, uploadMrlComm, "MrlComm.ino location
				// ", pathToMrlComm);

				GridBagConstraints gc = new GridBagConstraints();
				gc.gridx = 0;
				gc.gridy = 0;
				JPanel top = new JPanel(new GridBagLayout());
				top.add(new JLabel("Arduino IDE Path "), gc);

				gc.gridx++;
				top.add(arduinoPath, gc);
				gc.gridx++;
				if (chooser == null) {
					chooser = new FileChooser("browse", arduinoPath);
					chooser.filterDirsOnly();
				}
				top.add(chooser, gc);
				gc.gridx++;
				top.add(openMrlComm, gc);
				gc.gridx++;
				top.add(uploadMrlComm, gc);
				gc.gridx++;
				gc.gridx = 0;
				gc.gridy = 1;
				gc.gridwidth = 2;
				top.add(new JLabel("MrlComm.ino location "), gc);
				gc.gridx += 2;
				top.add(new JLabel(pathToMrlComm), gc);

				uploadPanel.add(top, BorderLayout.NORTH);

				editor = new TextEditorPane();
				editor.setText(mrlIno);
				editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
				editor.setCodeFoldingEnabled(true);
				editor.setAntiAliasingEnabled(true);
				editor.setEnabled(false);
				editor.setReadOnly(true);

				uploadPanel.add(new RTextScrollPane(editor), BorderLayout.CENTER);
				localTabs.insertTab("upload", null, uploadPanel, "upload", 0);
				localTabs.setTabComponentAt(0, new TabControl2(self, localTabs, uploadPanel, "upload"));
			}
		});
	}

	public void getMegaPanel() {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				pinComponentList = new ArrayList<PinComponent>();
				JLayeredPane imageMap = new JLayeredPane();
				imageMap.setPreferredSize(size);
				JLabel image = new JLabel();
				ImageIcon dPic = Util.getImageIcon("Arduino/mega.png");
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

				localTabs.insertTab("pins", null, imageMap, "pin panel", 0);
				localTabs.setTabComponentAt(0, new TabControl2(self, localTabs, imageMap, "pins"));

			}
		});
	}

	public void addOscopePanel() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				JPanel oscopePanel = null;

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

				oscope = new VideoWidget(boundServiceName, myService, localTabs, false);
				oscope.init(null);
				sensorImage = new SerializableImage(
						new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB), "output");
				g = sensorImage.getImage().getGraphics();
				oscope.displayFrame(sensorImage);

				oscopePanel.add(tracePanel, opgc);
				++opgc.gridx;
				oscopePanel.add(oscope.display, opgc);

				localTabs.insertTab("oscope", null, oscopePanel, "oscope panel", 0);
				localTabs.setTabComponentAt(0, new TabControl2(self, localTabs, oscopePanel, "oscope"));
				myService.getFrame().pack();
			}
		});
	}

	// public

	public void addPinPanel() {

		if (myArduino != null && myArduino.getBoardType() != null
				&& myArduino.getBoardType().toLowerCase().contains(" mega ")) {
			getMegaPanel();
			return;
		}
		getDuemilanovePanel();
	}

	/**
	 * onState is called when the Arduino service changes state information
	 * FIXME - this method is often called by other threads so gui - updates
	 * must be done in the swing post method way
	 * 
	 * @param arduino
	 */
	public void onState(final Arduino arduino) { // TODO - all onState data
		// should be final
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.info("onState Arduino");
				if (arduino != null) {
					myArduino = arduino; // FIXME - super updates registry state
					pinList = myArduino.getPinList();

					if (arduino.isConnected()) {
						status.setText(String.format("connected %s", arduino.getBoardInfo()));
					} else {
						status.setText("disconnected");						
					}

					arduinoPath.setText(myArduino.getArduinoPath());

					// update panels based on state change
					// TODO - check what state the panels are to see if a
					// change is needed
					uploadResult.setText(myArduino.uploadSketchResult);
				}
			}
		});

	}

	public void publishPin(final Pin pin) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				log.info("PinData:{}", pin);

				if (!traceData.containsKey(pin.pin)) {
					TraceData td = new TraceData();
					float gradient = 1.0f / pinComponentList.size();
					Color color = new Color(Color.HSBtoRGB((pin.pin * (gradient)), 0.8f, 0.7f));
					td.color = color;
					traceData.put(pin.pin, td);
					td.index = lastTraceXPos;
				}

				int value = pin.value / 2;

				TraceData t = traceData.get(pin.pin);
				t.index++;
				lastTraceXPos = t.index;
				t.data[t.index] = value;
				++t.total;
				t.sum += value;
				t.mean = t.sum / t.total;

				g.setColor(t.color);
				if (pin.type == Pin.DIGITAL_VALUE || pin.type == Pin.PWM_VALUE) {
					int yoffset = pin.pin * 15 + 35;
					int quantum = -10;
					g.drawLine(t.index, t.data[t.index - 1] * quantum + yoffset, t.index, value * quantum + yoffset);
				} else if (pin.type == Pin.ANALOG_VALUE) {
					g.drawLine(t.index, DATA_HEIGHT - t.data[t.index - 1], t.index, DATA_HEIGHT - value);
					// log.info("" + (value / 2));
				} else {
					log.error("dont know how to display pin data method");
				}

				// computer min max and mean
				// if different then blank & post to screen
				if (value > t.max)
					t.max = value;
				if (value < t.min)
					t.min = value;

				if (t.index < DATA_WIDTH - 1) {
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

	@Override
	public void itemStateChanged(ItemEvent event) {
		Object o = event.getSource();
		if (o == boardTypes && event.getStateChange() == ItemEvent.SELECTED) {
			String type = (String) boardTypes.getSelectedItem();
			if (type != null && type.length() > 0) { // &&
														// type.toUpperCase().contains("MEGA")
				send("setBoard", type);
			}
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
