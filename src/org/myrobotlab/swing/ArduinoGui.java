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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.swing.widget.DockableTabPane;
import org.myrobotlab.swing.widget.FileChooser;
import org.myrobotlab.swing.widget.Oscope;
import org.myrobotlab.swing.widget.PinGui;
import org.myrobotlab.swing.widget.PortGui;

public class ArduinoGui extends ServiceGui implements ActionListener, ItemListener, PortListener {

	static final long serialVersionUID = 1L;
	static final String[] BOARD_TYPES = new String[] { "uno", "mega", "megaadk", "nano" };

	JLabel status = new JLabel("disconnected");

	FileChooser chooser = null;

	PortGui portGui;

	JComboBox<String> boardTypes = new JComboBox<String>(BOARD_TYPES);

	Arduino myArduino;

	/**
	 * array list of graphical pin components built from pinList
	 */
	ArrayList<PinGui> pinGuiList = null;

	List<PinDefinition> pinList = null;
	ArduinoGui self;

	JMenuItem softReset = new JMenuItem("soft reset");

	DockableTabPane localTabs = new DockableTabPane();

	JButton openMrlComm = new JButton("Open in Arduino IDE");

	JTextField arduinoPath = new JTextField(20);

	JTextField boardType = new JTextField(5);
	JButton uploadMrlComm = new JButton("Upload MrlComm");
	JLabel uploadResult = new JLabel();

	transient TextEditorPane editor;

	public ArduinoGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);
		myArduino = (Arduino) Runtime.getService(boundServiceName);
		self = this;
		portGui = new PortGui(boundServiceName, myService);
		addTop(3, portGui.getDisplay());
		addTop(status);

		localTabs.setTabPlacementRight();

		// addOscopePanel();
		addMrlCommPanel();
		addPinPanel();
		Oscope oscope = new Oscope(boundServiceName, myService);
		oscope.addButtons(myArduino.getPinList());
		localTabs.addTab("oscope", oscope.getDisplay());

		add(localTabs.getTabs());

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

		if (o == softReset) {
			send("softReset");
			return;
		}

		// allow  hook ? - or just send directly !!
		// if (o instanceof PinGui) {
		//	PinGui p = (PinGui) o;
		//	send(p.getMethod(), p.getParams());
		//}

		if (o == openMrlComm) {
			send("setArduinoPath", arduinoPath.getText());
			send("openMrlComm");
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
			send("setArduinoPath", arduinoPath.getText());
			uploadResult.setText("Uploading Sketch");
			send("uploadSketch", arduinoPath.getText(), port, boardType.getText());
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
		subscribe("publishPinArray");
		subscribe("publishConnect");
		subscribe("publishDisconnect");
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("publishBoardInfo");
		unsubscribe("publishPinArray");
		unsubscribe("publishConnect");
		unsubscribe("publishDisconnect");
	}

	public void onBoardInfo(BoardInfo boardInfo) {
		status.setText(String.format("connected %s", boardInfo));
	}

	public void setPinTabUi() {

		JLayeredPane imageMap = new JLayeredPane();
		pinGuiList = new ArrayList<PinGui>();
		JLabel image = new JLabel();

		ImageIcon dPic = Util.getImageIcon("Arduino/uno.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));

		List<PinDefinition> pins = myArduino.getPinList();

		for (int i = 0; i < pins.size(); ++i) {

			PinGui p = new PinGui(myArduino, pins.get(i));

			// p.showName();
			
			// set up the listeners
			p.addActionListener(self);
			pinGuiList.add(p);

			if (i < 14) { // digital pins -----------------
				int yOffSet = 0;
				if (i > 7) {
					yOffSet = 13; // gap between pins
				}

				p.setBounds(552 - 20 * i - yOffSet, 18, 15, 15);  
				// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
				imageMap.add(p.getDisplay(), new Integer(2));

				/*
				 * if (p.isPWM) { p.pwmSlider.setBounds(256, 297 - 18 * i -
				 * yOffSet, 90, 15); imageMap.add(p.pwmSlider, new Integer(2));
				 * p.data.setBounds(232, 297 - 18 * i - yOffSet, 32, 15);
				 * p.data.setForeground(Color.white);
				 * p.data.setBackground(Color.decode("0x0f7391"));
				 * p.data.setOpaque(true); imageMap.add(p.data, new Integer(2));
				 * }
				 */
			} else {
				/*
				 * // analog pins -----------------
				 * p.activeInActive.setBounds(11, 208 - 18 * (14 - i), 48, 15);
				 * imageMap.add(p.activeInActive, new Integer(2));
				 * p.data.setBounds(116, 205 - 18 * (14 - i), 32, 18);
				 * p.data.setForeground(Color.white);
				 * p.data.setBackground(Color.decode("0x0f7391"));
				 * p.data.setOpaque(true); imageMap.add(p.data, new Integer(2));
				 */
			  p.setBounds(172 + 20 * i, 400, 15, 15);
			  imageMap.add(p.getDisplay(), new Integer(2));
			  
			}
		}
		localTabs.addTab("pin", imageMap);
	}

	public void addMrlCommPanel() {

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
		// editor.setPreferredSize(new Dimension(500, 400));
		editor.setText(mrlIno);
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
		editor.setCodeFoldingEnabled(true);
		editor.setAntiAliasingEnabled(true);
		editor.setEnabled(false);
		editor.setReadOnly(true);
		RTextScrollPane pane = new RTextScrollPane(editor);
		pane.setPreferredSize(new Dimension(500, 400));

		uploadPanel.add(pane, BorderLayout.CENTER);

		localTabs.addTab("upload", uploadPanel);

	}

	public void getMegaPanel() {
	
	}

	public void addPinPanel() {
		if (myArduino.getBoardType() != null && myArduino.getBoardType().toLowerCase().contains(" mega ")) {
			getMegaPanel();
			return;
		}
		setPinTabUi();
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
				log.debug("onState Arduino");
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

	public void onPinArray(final PinData[] pins) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

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
		return null;
	}

}
