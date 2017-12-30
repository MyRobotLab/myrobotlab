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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.roomba.RoombaComm;
import org.myrobotlab.service.Roomba;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class RoombaGui extends ServiceGui implements ListSelectionListener, ActionListener, ChangeListener, KeyListener {

  public final static Logger log = LoggerFactory.getLogger(RoombaGui.class);

  static final long serialVersionUID = 1L;

  // private Roomba myRoomba = null;
  JPanel ctrlPanel, selectPanel, buttonPanel, displayPanel;
  JComboBox<String> portChoices;
  JComboBox<String> protocolChoices;
  JCheckBox handshakeButton;
  JTextArea displayText;
  JButton connectButton;
  JSlider speedSlider;
  JButton keyboardControl;

  // RoombaCommSerial roombacomm; // in MRL'land can't have direct access to
  // this - must message it
  Roomba roombacomm; // in MRL'land can't have direct access to this - must
  // message it

  /** Returns an ImageIcon, or null if the path was invalid. */
  protected static ImageIcon createImageIcon(String path, String description) {

    return Util.getImageIcon("Roomba/" + path);
  }

  public RoombaGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    roombacomm = (Roomba) Runtime.getService(boundServiceName);
    display.setLayout(new BorderLayout());

    makePanels();

    setPorts(roombacomm.getPortNames());
  }

  /** implement actionlistener */
  @Override
  public void actionPerformed(ActionEvent event) {
    String action = event.getActionCommand();
    if ("comboBoxChanged".equals(action)) {
      String portname = (String) portChoices.getSelectedItem();
      roombacomm.setPortname(portname);
      int i = protocolChoices.getSelectedIndex();
      roombacomm.setProtocol((i == 0) ? "SCI" : "OI");
      return;
    }
    updateDisplay(action + "\n");
    if ("connect".equals(action)) {
      connect();
      return;
    } else if ("disconnect".equals(action)) {
      disconnect();
      return;
    }

    // stop right here if we're not connected
    if (!roombacomm.connected()) {
      updateDisplay("not connected!\n");
      return;
    }

    if ("stop".equals(action)) {
      roombacomm.stop();
    } else if ("forward".equals(action)) {
      roombacomm.goForward();
    } else if ("backward".equals(action)) {
      roombacomm.goBackward();
    } else if ("spinleft".equals(action)) {
      roombacomm.spinLeft();
    } else if ("spinright".equals(action)) {
      roombacomm.spinRight();
    } else if ("turnleft".equals(action)) {
      roombacomm.turnLeft();
    } else if ("turnright".equals(action)) {
      roombacomm.turnRight();
    } else if ("test".equals(action)) {
      updateDisplay("Playing some notes\n");
      roombacomm.playNote(72, 10); // C
      Service.sleep(200);
      roombacomm.playNote(79, 10); // G
      Service.sleep(200);
      roombacomm.playNote(76, 10); // E
      Service.sleep(200);

      updateDisplay("Spinning left, then right\n");
      roombacomm.spinLeft();
      Service.sleep(1000);
      roombacomm.spinRight();
      Service.sleep(1000);
      roombacomm.stop();

      updateDisplay("Going forward, then backward\n");
      roombacomm.goForward();
      Service.sleep(1000);
      roombacomm.goBackward();
      Service.sleep(1000);
      roombacomm.stop();
    } else if ("reset".equals(action)) {
      roombacomm.stop();
      roombacomm.startup();
      roombacomm.control();
    } else if ("power-off".equals(action)) {
      roombacomm.powerOff();
    } else if ("wakeup".equals(action)) {
      roombacomm.wakeup();
    } else if ("beep-lo".equals(action)) {
      roombacomm.playNote(50, 32); // C1
      Service.sleep(200);
    } else if ("beep-hi".equals(action)) {
      roombacomm.playNote(90, 32); // C7
      Service.sleep(200);
    } else if ("clean".equals(action)) {
      roombacomm.clean();
    } else if ("spot".equals(action)) {
      roombacomm.spot();
    } else if ("vacuum-on".equals(action)) {
      roombacomm.vacuum(true);
    } else if ("vacuum-off".equals(action)) {
      roombacomm.vacuum(false);
    } else if ("blink-leds".equals(action)) {
      roombacomm.setLEDs(true, true, true, true, true, true, 255, 255);
      Service.sleep(300);
      roombacomm.setLEDs(false, false, false, false, false, false, 0, 128);
    } else if ("sensors".equals(action)) {
      if (roombacomm.updateSensors())
        updateDisplay(roombacomm.sensorsAsString() + "\n");
      else
        updateDisplay("couldn't read Roomba. Is it connected?\n");
    }
  }

  @Override
  public void subscribeGui() {
  }

  public boolean connect() {
    String portname = (String) portChoices.getSelectedItem();
    // roombacomm.debug=true;
    roombacomm.setWaitForDSR(handshakeButton.isSelected());
    int i = protocolChoices.getSelectedIndex();
    roombacomm.setProtocol((i == 0) ? "SCI" : "OI");

    connectButton.setText("connecting");
    try {
    	roombacomm.connect(portname);
    } catch(Exception e){
    	log.error("could not connect", e);
    	updateDisplay("Couldn't connect to " + portname + "\n");
        connectButton.setText("  connect  ");
        return false;
    }
    
    updateDisplay("Roomba startup\n");

    roombacomm.startup();
    roombacomm.control();
    roombacomm.playNote(72, 10); // C , test note
    Service.sleep(200);

    connectButton.setText("disconnect");
    connectButton.setActionCommand("disconnect");
    // roombacomm.debug=true;
    updateDisplay("Checking for Roomba... ");
    if (roombacomm.updateSensors())
      updateDisplay("Roomba found!\n");
    else
      updateDisplay("No Roomba. :(  Is it turned on?\n");

    return true;
  }

  @Override
  public void unsubscribeGui() {
  }

  /** */
  public void disconnect() {
    roombacomm.disconnect();
    connectButton.setText("  connect  ");
    connectButton.setActionCommand("connect");
  }

  // ripped from RoombacommPanel - 'thanks guys !

  public void onState(Roomba roomba) {

    if (roomba != null) {
      setPorts(roomba.getPortNames());
    }

  }


  /** Handle the key pressed event from the text field. */
  @Override
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    if (keyCode == KeyEvent.VK_SPACE) {
      updateDisplay("stop");
      roombacomm.stop();
    } else if (keyCode == KeyEvent.VK_UP) {
      updateDisplay("forward");
      roombacomm.goForward();
    } else if (keyCode == KeyEvent.VK_DOWN) {
      updateDisplay("backward");
      roombacomm.goBackward();
    } else if (keyCode == KeyEvent.VK_LEFT) {
      updateDisplay("spinleft");
      roombacomm.spinLeft();
    } else if (keyCode == KeyEvent.VK_RIGHT) {
      updateDisplay("spinright");
      roombacomm.spinRight();
    } else if (keyCode == KeyEvent.VK_COMMA) {
      updateDisplay("speed down");
      roombacomm.setSpeed(roombacomm.getSpeed() - 50);
    } else if (keyCode == KeyEvent.VK_PERIOD) {
      updateDisplay("speed up");
      roombacomm.setSpeed(roombacomm.getSpeed() + 50);
    } else if (keyCode == KeyEvent.VK_R) {
      updateDisplay("reset");
      roombacomm.reset();
      roombacomm.control();
    }
  }

  /** Handle the key released event from the text field. */
  @Override
  public void keyReleased(KeyEvent e) {
  }

  /** Handle the key typed event from the text field. */
  @Override
  public void keyTyped(KeyEvent e) {
  }

  /**
     *
     */
  void makeButtonPanel() {
    buttonPanel = new JPanel(new GridLayout(8, 2));
    buttonPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Commands"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

    JButton but_reset = new JButton("reset");
    buttonPanel.add(but_reset);
    but_reset.addActionListener(this);

    JButton but_test = new JButton("test");
    buttonPanel.add(but_test);
    but_test.addActionListener(this);

    JButton but_power = new JButton("power-off");
    buttonPanel.add(but_power);
    but_power.addActionListener(this);

    JButton but_wakeup = new JButton("wakeup");
    buttonPanel.add(but_wakeup);
    but_wakeup.addActionListener(this);

    JButton but_beeplo = new JButton("beep-lo");
    buttonPanel.add(but_beeplo);
    but_beeplo.addActionListener(this);

    JButton but_beephi = new JButton("beep-hi");
    buttonPanel.add(but_beephi);
    but_beephi.addActionListener(this);

    JButton but_clean = new JButton("clean");
    buttonPanel.add(but_clean);
    but_clean.addActionListener(this);

    JButton but_spot = new JButton("spot");
    buttonPanel.add(but_spot);
    but_spot.addActionListener(this);

    JButton but_vacon = new JButton("vacuum-on");
    buttonPanel.add(but_vacon);
    but_vacon.addActionListener(this);

    JButton but_vacoff = new JButton("vacuum-off");
    buttonPanel.add(but_vacoff);
    but_vacoff.addActionListener(this);

    JButton but_blinkleds = new JButton("blink-leds");
    buttonPanel.add(but_blinkleds);
    but_blinkleds.addActionListener(this);

    JButton but_sensors = new JButton("sensors");
    buttonPanel.add(but_sensors);
    but_sensors.addActionListener(this);

    keyboardControl = new JButton("keyboard control");
    keyboardControl.addKeyListener(this);
    buttonPanel.add(keyboardControl);
  }

  /** 
     * 
     */
  void makeCtrlPanel() {
    JPanel ctrlPanel1 = new JPanel(new GridLayout(3, 3));

    JButton but_turnleft = new JButton(createImageIcon("but_turnleft.png", "turnleft"));
    ctrlPanel1.add(but_turnleft);
    JButton but_forward = new JButton(createImageIcon("but_forward.png", "forward"));
    ctrlPanel1.add(but_forward);
    JButton but_turnright = new JButton(createImageIcon("but_turnright.png", "turnright"));
    ctrlPanel1.add(but_turnright);

    JButton but_spinleft = new JButton(createImageIcon("but_spinleft.png", "spinleft"));
    ctrlPanel1.add(but_spinleft);
    JButton but_stop = new JButton(createImageIcon("but_stop.png", "stop"));
    ctrlPanel1.add(but_stop);
    JButton but_spinright = new JButton(createImageIcon("but_spinright.png", "spinright"));
    ctrlPanel1.add(but_spinright);

    ctrlPanel1.add(new JLabel());
    JButton but_backward = new JButton(createImageIcon("but_backward.png", "backward"));
    ctrlPanel1.add(but_backward);
    ctrlPanel1.add(new JLabel());

    JLabel sliderLabel = new JLabel("speed (mm/s)", SwingConstants.CENTER);
    speedSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 500, 200);
    speedSlider.setPaintTicks(true);
    speedSlider.setMajorTickSpacing(100);
    speedSlider.setMinorTickSpacing(25);
    speedSlider.setPaintLabels(true);
    speedSlider.addChangeListener(this);

    ctrlPanel = new JPanel();
    ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS));

    ctrlPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Movement"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    ctrlPanel.add(ctrlPanel1);
    ctrlPanel.add(speedSlider);
    ctrlPanel.add(sliderLabel);

    but_turnleft.setActionCommand("turnleft");
    but_turnright.setActionCommand("turnright");
    but_spinleft.setActionCommand("spinleft");
    but_spinright.setActionCommand("spinright");
    but_forward.setActionCommand("forward");
    but_backward.setActionCommand("backward");
    but_stop.setActionCommand("stop");
    but_turnleft.addActionListener(this);
    but_turnright.addActionListener(this);
    but_spinleft.addActionListener(this);
    but_spinright.addActionListener(this);
    but_forward.addActionListener(this);
    but_backward.addActionListener(this);
    but_stop.addActionListener(this);
  }

  /**
     *
     */
  void makeDisplayPanel() {
    displayPanel = new JPanel();
    displayPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Display"), BorderFactory.createEmptyBorder(1, 1, 1, 1)));

    displayText = new JTextArea(5, 30);
    displayText.setLineWrap(true);
    DefaultCaret dc = new DefaultCaret();
    // only works on Java 1.5+
    // dc.setUpdatePolicy( DefaultCaret.ALWAYS_UPDATE );
    displayText.setCaret(dc);
    JScrollPane scrollPane = new JScrollPane(displayText, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    displayPanel.add(scrollPane);
  }

  /**
     *
     */
  void makePanels() {
    makeSelectPanel();
    display.add(selectPanel, BorderLayout.NORTH);

    makeCtrlPanel();
    display.add(ctrlPanel, BorderLayout.EAST);

    makeButtonPanel();
    display.add(buttonPanel, BorderLayout.CENTER);

    makeDisplayPanel();
    display.add(displayPanel, BorderLayout.SOUTH);

    // pack(); //setVisible(true);
    updateDisplay("RoombaComm, version " + RoombaComm.VERSION + "\n");
  }

  void makeSelectPanel() {
    selectPanel = new JPanel();

    // Create a combo box with protocols
    String[] protocols = { "Roomba 1xx-4xx (SCI)", "Roomba 5xx (OI)" };
    protocolChoices = new JComboBox<String>(protocols);
    String p = roombacomm.getProtocol();
    protocolChoices.setSelectedIndex(p.equals("SCI") ? 0 : 1);

    // Create a combo box with choices.
    String[] ports = roombacomm.listPorts();
    portChoices = new JComboBox<String>(ports);

    if (ports.length > 0) {
      portChoices.setSelectedIndex(0);
      for (int i = 0; i < ports.length; i++) {
        String s = ports[i];
        if (s.equals(roombacomm.getPortname())) {
          portChoices.setSelectedItem(s);
        }
      }
    } else {
      log.error("no ports found!");
    }
    connectButton = new JButton();
    connectButton.setText("  connect  ");
    connectButton.setActionCommand("connect");
    handshakeButton = new JCheckBox("<html>h/w<br>handshake</html>");

    // Add a border around the select panel.
    selectPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Select Roomba Type & Port"), BorderFactory.createEmptyBorder(1, 1, 1, 1)));

    selectPanel.add(protocolChoices);
    selectPanel.add(portChoices);
    selectPanel.add(connectButton);
    selectPanel.add(handshakeButton);

    // Listen to events from the combo box.
    portChoices.addActionListener(this);
    connectButton.addActionListener(this);
    protocolChoices.addActionListener(this);
  }

  /*
   * Play a (MIDI) note, that is, make the Roomba a musical instrument notenums
   * 32-127: notenum == corresponding note played thru beeper velocity ==
   * duration in number of 1/64s of a second (e.g. 64==1second) notenum 24:
   * notenum == main vacuum velocity == non-zero turns on, zero turns off
   * notenum 25: blink LEDs, velcoity is color of Power LED notenum 28 &amp; 29:
   * spin left &amp; spin right, velocity is speed
   * 
   */
  public void playMidiNote(int notenum, int velocity) {
    updateDisplay("play note: " + notenum + "," + velocity + "\n");
    if (!roombacomm.connected())
      return;

    if (notenum >= 31) { // G and above
      if (velocity == 0)
        return;
      if (velocity < 4)
        velocity = 4; // has problems at lower durations
      else
        velocity = velocity / 2;
      roombacomm.playNote(notenum, velocity);
    } else if (notenum == 24) { // C
      roombacomm.vacuum(!(velocity == 0));
    } else if (notenum == 25) { // C#
      boolean lon = (velocity != 0);
      int inten = (lon) ? 255 : 128; // either full bright or half bright
      roombacomm.setLEDs(lon, lon, lon, lon, lon, lon, velocity * 2, inten);
    } else if (notenum == 28) { // E
      if (velocity != 0)
        roombacomm.spinLeftAt(velocity * 2);
      else
        roombacomm.stop();
    } else if (notenum == 29) { // F
      if (velocity != 0)
        roombacomm.spinRightAt(velocity * 2);
      else
        roombacomm.stop();
    }
  }

  /**
   * setPorts is called by onState - which is called when the Arduino changes
   * port state is NOT called by the SwingGui component
   * 
   * @param p
   *          FIXME - there should be a corresponding gui element for the
   *          serial.Port ie serial.PortGUI such
   */
  public void setPorts(List<String> p) {
    portChoices.removeAllItems();

    portChoices.addItem(""); // the null port

    for (int i = 0; i < p.size(); ++i) {
      String n = p.get(i);
      log.info(n);
      portChoices.addItem(n);
    }

  }

  /*
   * Set to 'false' to hide the "h/w handshake" button, which seems to be only
   * needed on Windows
   */
  public void setShowHardwareHandhake(boolean b) {
    handshakeButton.setVisible(b);
  }

  /** implement ChangeListener, for the slider */
  @Override
  public void stateChanged(ChangeEvent e) {
    // System.err.println("stateChanged:"+e);
    JSlider src = (JSlider) e.getSource();
    if (!src.getValueIsAdjusting()) {
      int speed = src.getValue();
      speed = (speed < 1) ? 1 : speed; // don't allow zero speed
      updateDisplay("setting speed = " + speed + "\n");
      roombacomm.setSpeed(speed);
    }
  }

  public void updateDisplay(String s) {
    displayText.append(s);
    displayText.setCaretPosition(displayText.getDocument().getLength());
  }

  @Override
  public void valueChanged(ListSelectionEvent arg0) {
    // TODO Auto-generated method stub

  }
}