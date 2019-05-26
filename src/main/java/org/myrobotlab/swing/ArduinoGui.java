/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PortListener;
import org.myrobotlab.swing.widget.DockableTab;
import org.myrobotlab.swing.widget.DockableTabPane;
import org.myrobotlab.swing.widget.FileChooser;
import org.myrobotlab.swing.widget.Oscope;
import org.myrobotlab.swing.widget.PinGui;
import org.myrobotlab.swing.widget.PortGui;

public class ArduinoGui extends ServiceGui implements ActionListener, ItemListener, PortListener {
  static final long serialVersionUID = 1L;

  JLabel status = new JLabel("disconnected");

  FileChooser chooser = null;

  PortGui portGui;

  PinArrayControl myArduinox;

  /**
   * array list of graphical pin components built from pinList
   */
  ArrayList<PinGui> pinGuiList = null;

  List<PinDefinition> pinList = null;

  JMenuItem softReset = new JMenuItem("soft reset");

  DockableTabPane localTabs = new DockableTabPane();

  JButton openMrlComm = new JButton("Open in Arduino IDE");

  JTextField arduinoPath = new JTextField(20);

  JTextField boardType = new JTextField(5);

  JButton uploadMrlComm = new JButton("Upload MrlComm");

  JComboBox<BoardType> boardTypes = new JComboBox<BoardType>();
  Map<String, BoardType> boardToBoardType = null;

  transient TextEditorPane editor;

  /**
   * current board type of the arduino
   */
  String board = null;

  String pinTabBoard = null;

  JTextArea uploadResults = new JTextArea(5, 30);

  Arduino myArduino;

  JButton virtual = new JButton("virtual");
  JButton enableBoardInfo = new JButton("heartbeat");

  transient Oscope oscope;

  JLabel pathToMrlComm = new JLabel("                             ");

  public ArduinoGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    myArduino = (Arduino) Runtime.getService(boundServiceName);
    self = this;

    portGui = new PortGui(boundServiceName, myService);
    addTop(portGui.getDisplay(), boardTypes, virtual);
    addTop(2, status);

    localTabs.setTabPlacementRight();

    // addOscopePanel();
    uploadResults.setEditable(false);
    addMrlCommPanel();
    updatePinTab(myArduino);

    oscope = new Oscope(boundServiceName, myService);
    oscope.setPins(myArduino.getPinList());

    localTabs.addTab("oscope", oscope.getDisplay());

    add(localTabs.getTabs());

    // configuration - there are only a small number of layouts for
    // a large number of board types - for example : mega1280 pin layout
    // is the same as mega2480 only different is memory
    // "upload" and gcc parameters may be different, but for board layout
    // we have config of all the different board types to the more simplistic
    // board layouts

  }

  void addListeners() {
    softReset.addActionListener(this);
    boardTypes.addItemListener(this);
    openMrlComm.addActionListener(this);
    uploadMrlComm.addActionListener(this);
    virtual.addActionListener(this);
  }

  void removeListeners() {
    softReset.removeActionListener(this);
    boardTypes.removeItemListener(this);
    openMrlComm.removeActionListener(this);
    uploadMrlComm.removeActionListener(this);
    virtual.removeActionListener(this);
  }

  /**
   * The guts of the business logic of handling all the graphical components and
   * their relations with each other.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();

    if (o == softReset) {
      send("softReset");
      return;
    }

    if (o == virtual) {
      Color c = virtual.getBackground();
      if (virtual.getText().equals("virtual")) {
        virtual.setText("virtualized");
        // virtual.setBackground(Color.PINK);
        send("setVirtual", true);
      } else {
        virtual.setText("virtualized");
        // virtual.setBackground(Color.PINK);
        send("setVirtual", false);
      }
    }

    if (o == openMrlComm) {
      send("openMrlComm", arduinoPath.getText());
    }

    if (o == uploadMrlComm) {
      String path = arduinoPath.getText();
      String port = portGui.getSelected();
      if (port == null || port.equals("")) {
        error("please set port");
        return;
      }
      uploadResults.setText("Uploading Sketch");
      send("uploadSketch", path, port);
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

  public void updatePinTab(Arduino arduino) {

    // determine board def
    // this is definition of the pins on the board
    // currently only 2 (I think they may be based on processor)
    // uno & mega

    // ALL BOARD HAVING 20 pins 14 digital 6 analog etc..
    String boardLayout = "uno";

    if (arduino.getBoard().contains("mega")) {
      // ALL BOARDS HAVIN 72 pins .. blah blah blah ...
      boardLayout = "mega";
    }

    if (boardLayout.equals(pinTabBoard)) { // FIXME - check this ?
      // no changes - just return
      return;
    }

    pinTabBoard = boardLayout;

    JLayeredPane imageMap = new JLayeredPane();
    pinGuiList = new ArrayList<PinGui>();
    JLabel image = new JLabel();

    ImageIcon dPic = Util.getImageIcon(String.format("Arduino/%s.png", pinTabBoard));
    image.setIcon(dPic);
    Dimension s = image.getPreferredSize();
    image.setBounds(0, 0, s.width, s.height);
    imageMap.add(image, new Integer(1));

    List<PinDefinition> pins = myArduino.getPinList();

    if (boardLayout.equals("uno")) {
      createUnoPinDisplay(imageMap, pins);
    } else if (boardLayout.equals("micro")) {
      createMicroPinDisplay(imageMap, pins);
    } else {
      createMegaPinDisplay(imageMap, pins);
    }

    DockableTab dockableTab = localTabs.get("pin");
    if (dockableTab == null) {
      localTabs.addTab("pin", imageMap);
    } else {
      dockableTab.setTab("pin", imageMap);
    }
  }

  private void createMicroPinDisplay(JLayeredPane imageMap, List<PinDefinition> pins) {
    // TODO Auto-generated method stub
    
  }

  private void createUnoPinDisplay(JLayeredPane imageMap, List<PinDefinition> pins) {
    for (int i = 0; i < pins.size(); ++i) {

      PinGui p = new PinGui(myArduino, pins.get(i));
      // p.showName();

      // set up the listeners
      p.addActionListener(this);
      pinGuiList.add(p);

      if (i < 14) { // digital pins -----------------
        int yOffSet = 0;
        if (i > 7) {
          yOffSet = 13; // gap between pins
        }

        // p.setBounds(552 - 20 * i - yOffSet, 18, 15, 15);
        p.setLocation(552 - 20 * i - yOffSet, 18);
        imageMap.add(p.getDisplay(), new Integer(2));
      } else {

        p.setLocation(172 + 20 * i, 400);/* , 15, 15); */
        imageMap.add(p.getDisplay(), new Integer(2));
      }
    }
  }

  private void createMegaPinDisplay(JLayeredPane imageMap, List<PinDefinition> pins) {
    for (int i = 0; i < pins.size(); ++i) {

      PinGui p = new PinGui(myArduino, pins.get(i));
      // p.showName();

      // set up the listeners
      p.addActionListener(this);
      pinGuiList.add(p);

      if (i < 14) { // digital pins -----------------
        int yOffSet = 0;
        if (i > 7) {
          yOffSet = 13; // gap between pins
        }

        p.setLocation(508 - 19 * i - yOffSet, 18);
        imageMap.add(p.getDisplay(), new Integer(2));
      } else if (i < 22) {
        p.setLocation(260 + 20 * i, 18);
        imageMap.add(p.getDisplay(), new Integer(2));
      } else if (i < 54) {
        if (i % 2 != 0) {
          p.setLocation(737 + 20, 8 + (int) (9.5 * (i - 20)));
          imageMap.add(p.getDisplay(), new Integer(2));
        } else {
          p.setLocation(737, 8 + (int) (9.5 * (i - 19)));
          imageMap.add(p.getDisplay(), new Integer(2));
        }
      } else if (i < 62) {
        p.setLocation(19 * i - 612, 380);
        imageMap.add(p.getDisplay(), new Integer(2));
      } else {
        p.setLocation(19 * i - 591, 380);
        imageMap.add(p.getDisplay(), new Integer(2));
      }
    }
  }

  public void addMrlCommPanel() {

    JPanel uploadPanel = new JPanel(new BorderLayout());
    String mrlIno = null;
    try {
      pathToMrlComm.setText(Util.getResourceDir() + "/Arduino/MrlComm/MrlComm.ino");
      mrlIno = FileIO.toString(pathToMrlComm.getText());
    } catch (Exception e) {
      log.warn("MrlComm.ino not found", e);
    }

    JPanel top = new JPanel(new GridLayout(0, 1));
    
    JPanel firstLine = new JPanel(new BorderLayout());
    firstLine.add(new JLabel("Arduino IDE Path "), BorderLayout.WEST);
    firstLine.add(arduinoPath, BorderLayout.CENTER);

    JPanel flow = new JPanel();
    if (chooser == null) {
      chooser = new FileChooser("browse", arduinoPath);
      chooser.filterDirsOnly();
    }
    flow.add(chooser);
    flow.add(openMrlComm);
    firstLine.add(flow, BorderLayout.EAST);
    
    top.add(firstLine);
    
    JPanel secondLine = new JPanel(new BorderLayout());
    secondLine.add(new JLabel("MrlComm.ino location :"), BorderLayout.WEST);    
    top.add(secondLine);
    
    top.add(pathToMrlComm);
    
    uploadPanel.add(top, BorderLayout.NORTH);

    editor = new TextEditorPane();
    editor.setText(mrlIno);
    editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
    editor.setCodeFoldingEnabled(true);
    editor.setAntiAliasingEnabled(true);
    editor.setEnabled(false);
    editor.setReadOnly(true);
    RTextScrollPane pane = new RTextScrollPane(editor);
    pane.setPreferredSize(new Dimension(500, 400));

    uploadPanel.add(pane, BorderLayout.CENTER);

    JScrollPane pane2 = new JScrollPane(uploadResults);
    uploadPanel.add(pane2, BorderLayout.SOUTH);

    localTabs.addTab("upload", uploadPanel);

  }

  /**
   * updates ui - called from both initialization &amp; onState
   * 
   */
  public void update(final Arduino arduino) { // TODO - all onState data
    // should be final
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        removeListeners();

        if (boardToBoardType == null) {
          boardToBoardType = new TreeMap<String, BoardType>();
          List<BoardType> arduinoBoardTypes = arduino.getBoardTypes();
          for (BoardType bt : arduinoBoardTypes) {
            boardTypes.addItem(bt);
            boardToBoardType.put(bt.getBoard(), bt);
          }
        }

        boardTypes.setSelectedItem(boardToBoardType.get(arduino.getBoard()));

        // check if boardType has changed ..
        // if so - we need to recreate pins pinlists oscope views etc..
        if (!arduino.getBoard().equals(board)) {
          pinList = arduino.getPinList();
          board = arduino.getBoard();
          boardTypes.setSelectedItem(board);
          oscope.setPins(pinList);
        }

        if (arduino.isConnected()) {
          status.setText(String.format("connected %s", arduino.getBoardInfo()));
        } else {
          status.setText("disconnected");
        }

        updatePinTab(myArduino);

        arduinoPath.setText(arduino.getArduinoPath());
        uploadResults.setText(arduino.uploadSketchResult);

        addListeners();
      }
    });

  }

  /**
   * onState is called when the Arduino service changes state information FIXME
   * - this method is often called by other threads so gui - updates must be
   * done in the swing post method way
   *
   * @param arduino
   *          - the arduino service to update state from
   */
  public void onState(final Arduino arduino) {
    myArduino = arduino;
    update(arduino);
  }

  @Override
  public void itemStateChanged(ItemEvent event) {
    Object o = event.getSource();
    if (o == boardTypes && event.getStateChange() == ItemEvent.SELECTED) {
      BoardType boardType = (BoardType) boardTypes.getSelectedItem();
      if (boardType != null) {
        send("setBoard", boardType.getBoard());
      }
    }
  }

  public void onPinArray(PinData[] pinArray) {
    oscope.onPinArray(pinArray);
  }

  @Override
  public String getName() {
    return null;
  }

}
