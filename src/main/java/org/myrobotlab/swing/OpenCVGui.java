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

import static org.myrobotlab.service.OpenCV.INPUT_KEY;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.myrobotlab.swing.opencv.ComboBoxModel2;
import org.myrobotlab.swing.opencv.OpenCVFilterGui;
import org.myrobotlab.swing.widget.OpenCVListAdapter;
import org.slf4j.Logger;

public class OpenCVGui extends ServiceGui implements ListSelectionListener, VideoGUISource, ActionListener {

  static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVGui.class);

  final static String FILTER_PACKAGE_NAME = "org.myrobotlab.swing.opencv.OpenCVFilter";
  final static String PREFIX = "OpenCVFilter";
  final static String PREFIX_PATH = "org.bytedeco.javacv.";

  BasicArrowButton addFilterButton = new BasicArrowButton(BasicArrowButton.EAST);
  JComboBox<Integer> cameraIndex = new JComboBox<Integer>(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 });
  JRadioButton cameraRadio = new JRadioButton();
  JButton capture = new JButton("capture");
  JButton pause = new JButton("pause");
  JPanel captureCfg = new JPanel();
  CanvasFrame cframe = null;
  transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
  DefaultListModel<OpenCVFilterGui> currentFilterListModel = new DefaultListModel<>();
  JList<OpenCVFilterGui> currentFilters;
  JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
  JRadioButton fileRadio = new JRadioButton();
  JPanel filterGuiDisplay = new JPanel();
  JComboBox<String> grabberTypeSelect = new JComboBox<String>();
  LinkedHashMap<String, OpenCVFilterGui> guiFilters = new LinkedHashMap<>();
  JTextField inputFile = new JTextField("");

  JButton open = new JButton("open");
  DefaultComboBoxModel<String> pipelineHookModel = new DefaultComboBoxModel<>();
  JComboBox<String> pipelineHook = new JComboBox<>(pipelineHookModel);
  OpenCVListAdapter popup = new OpenCVListAdapter(this);
  JList<String> possibleFilters;
  JButton recordButton = new JButton("record");
  JCheckBox recordFrames = new JCheckBox("frames");
  JButton recordFrameButton = new JButton("record frame");
  BasicArrowButton removeFilterButton = new BasicArrowButton(BasicArrowButton.WEST);
  final OpenCVGui self;
  JCheckBox undock = new JCheckBox("undock");
  JButton url = new JButton("url");
  VideoWidget video0 = null;

  public OpenCVGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    self = this;
    Runtime myRuntime = (Runtime) Runtime.getInstance();
    OpenCV opencv = (OpenCV) myRuntime.getService(boundServiceName);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setDialogTitle("open file");

    video0 = new VideoWidget(boundServiceName, myService);
    ComboBoxModel2.add(INPUT_KEY);

    grabberTypeSelect.addItem(null);
    for (String type : OpenCV.getGrabberTypes()) {
      grabberTypeSelect.addItem(type);
    }

    possibleFilters = new JList<>(OpenCV.getPossibleFilters());
    possibleFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    possibleFilters.setSelectedIndex(0);
    possibleFilters.setVisibleRowCount(10);
    possibleFilters.setSize(140, 160);
    possibleFilters.addMouseListener(popup);

    currentFilters = new JList<>(currentFilterListModel);

    // Show if filter is enabled..
    currentFilters.setCellRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof OpenCVFilterGui) {
          OpenCVFilterGui filter = (OpenCVFilterGui) value;

          if (opencv != null && opencv.getFilter(filter.name) != null && opencv.getFilter(filter.name).isEnabled()) {
            setBackground(Color.GREEN);
          } else {
            setBackground(Color.WHITE);
          }
        }
        return c;
      }

    });
    // end

    currentFilters.setFixedCellWidth(100);
    currentFilters.addListSelectionListener(this);
    currentFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    currentFilters.setSize(140, 160);
    currentFilters.setVisibleRowCount(10);

    JPanel videoPanel = new JPanel();
    videoPanel.add(video0.display);

    ButtonGroup groupRadio = new ButtonGroup();
    groupRadio.add(cameraRadio);
    groupRadio.add(fileRadio);

    // capture panel
    JPanel cpanel = new JPanel();
    cpanel.setBorder(BorderFactory.createEtchedBorder());
    cpanel.add(capture);
    cpanel.add(pause);
    // cpanel.add(grabberTypeSelect);
    cpanel.add(undock);

    captureCfg.setBorder(BorderFactory.createEtchedBorder());
    captureCfg.add(grabberTypeSelect);
    captureCfg.add(cameraRadio);
    captureCfg.add(new JLabel("camera"));
    captureCfg.add(cameraIndex);
    captureCfg.add(fileRadio);
    captureCfg.add(new JLabel("file"));
    // captureCfg.add(inputFile);
    // captureCfg.add(pipelineHook);
    captureCfg.add(open);
    captureCfg.add(url);

    JPanel input = new JPanel();
    input.setBorder(BorderFactory.createTitledBorder("input"));
    input.add(cpanel);
    input.add(captureCfg);

    JPanel output = new JPanel();
    output.setBorder(BorderFactory.createTitledBorder("output"));
    output.add(recordButton);
    output.add(recordFrames);
    output.add(recordFrameButton);

    JPanel filterPanel = new JPanel();
    filterPanel.setBorder(BorderFactory.createTitledBorder("filters: available - current"));
    filterPanel.add(new JScrollPane(possibleFilters));
    filterPanel.add(removeFilterButton);
    filterPanel.add(addFilterButton);
    filterPanel.add(new JScrollPane(currentFilters));

    filterGuiDisplay.setBorder(BorderFactory.createTitledBorder("filter parameters"));
    Box box = Box.createVerticalBox();
    box.add(filterPanel);
    box.add(filterGuiDisplay);

    Box inputOutput = Box.createVerticalBox();
    inputOutput.add(input);
    inputOutput.add(output);

    display.add(box, BorderLayout.EAST);
    display.add(videoPanel, BorderLayout.CENTER);
    display.add(input, BorderLayout.NORTH);
    display.add(output, BorderLayout.SOUTH);

    setCurrentFilterMouseListener();
    enableListeners(true);
  }

  // gui events handled - never "set" status of a ui component here ...
  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == capture) {
      if (("capture".equals(capture.getText()))) {
        send("capture");
      } else {
        send("stopCapture");
      }
      send("broadcastState");
    } else if (o == pause) {
      send("pauseCapture");
      if (("pause".equals(pause.getText()))) {
        send("pauseCapture");
      } else {
        send("resumeCapture");
      }
    } else if (o == cameraIndex || o == cameraRadio) {
      send("setInputSource", OpenCV.INPUT_SOURCE_CAMERA);
      send("setCameraIndex", cameraIndex.getSelectedItem());

    } else if (o == fileRadio) {
      send("setInputSource", OpenCV.INPUT_SOURCE_FILE);

    } else if (o == open) {
      int returnValue = fc.showOpenDialog(null);
      if (returnValue == JFileChooser.APPROVE_OPTION) {
        send("setInputFileName", fc.getSelectedFile().getAbsolutePath());
        send("setInputSource", OpenCV.INPUT_SOURCE_FILE);
      }

    } else if (o == url) {
      String urlStr = JOptionPane.showInputDialog(null, "url");
      send("setInputFileName", urlStr);
      send("setInputSource", OpenCV.INPUT_SOURCE_FILE);

    } else if (o == addFilterButton) {
      addFilter();

    } else if (o == removeFilterButton) {
      OpenCVFilterGui filterGui = currentFilters.getSelectedValue();
      send("removeFilter", filterGui.name);
      // TODO - block on response
      // currentFilterListModel.removeElement(filterGui);
    } else if (o == recordButton) {
      if (recordButton.getText().equals("record")) {
        if (recordFrames.isSelected()) {
          send("recordFrames");
        } else {
          send("record");
        }
      } else {
        send("stopRecording");
      }
    } else if (o == grabberTypeSelect) {
      String type = (String) grabberTypeSelect.getSelectedItem();
      /**
       * <pre>
       * ALL FRAME GRABBER LOGIC IS DONE IN OpenCV.getGrabber() !!! 1 place of
       * chaotic logic to rule them all !
       * 
       * if (type != null && type.startsWith("OpenKinect") ||
       * type.equals("PS3Eye") || type.equals("Sarxos") ||
       * type.equals("VideoInput")|| type.equals("FlyCapture")) { // cuz these
       * are all cameras ... log.warn("setting as camera as source");
       * send("setInputSource", OpenCV.INPUT_SOURCE_CAMERA); }
       */
      send("setGrabberType", type);
    } else if (o == recordFrameButton) {
      send("recordFrame");
    } else if (o == undock) {
      if (undock.isSelected()) {
        if (cframe != null) {
          cframe.dispose();
        }
        cframe = new CanvasFrame("canvas");
      } else {
        if (cframe != null) {
          cframe.dispose();
          cframe = null;
        }
      }
    } else if (o == fileRadio) {
      send("setInputSource", OpenCV.INPUT_SOURCE_FILE);
    }
  }

  public void addFilter() {
    JFrame frame = new JFrame();
    frame.setTitle("add new filter");
    String name = JOptionPane.showInputDialog(frame, "new filter name");
    String type = possibleFilters.getSelectedValue();
    send("addFilter", name, type);
  }

  public OpenCVFilterGui addFilterToGui(OpenCVFilter filter) {

    String name = filter.name;
    String type = filter.getClass().getSimpleName();
    type = type.substring(PREFIX.length());

    String guiType = FILTER_PACKAGE_NAME + type + "Gui";

    OpenCVFilterGui filtergui = null;

    // try creating one based on type
    filtergui = (OpenCVFilterGui) Instantiator.getNewInstance(guiType, name, boundServiceName, swingGui);
    if (filtergui == null) {
      log.info("filter {} does not have a gui defined", type);
      filtergui =

          (OpenCVFilterGui) Instantiator.getNewInstance(FILTER_PACKAGE_NAME + "DefaultGui", name, boundServiceName, swingGui);
    }

    currentFilterListModel.addElement(filtergui);

    // add new input to sources
    ArrayList<String> newSources = filter.getPossibleSources();
    for (int i = 0; i < newSources.size(); ++i) {
      ComboBoxModel2.add(String.format("%s.%s", boundServiceName, newSources.get(i)));
    }

    // set source of gui's input to
    filtergui.initFilterState(filter); // set the bound filter
    guiFilters.put(name, filtergui);
    currentFilters.setSelectedIndex(currentFilterListModel.size() - 1);
    return filtergui;
  }

  protected ImageIcon createImageIcon(String path, String description) {
    java.net.URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL, description);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

  public void displayFrame(SerializableImage frame) {
    video0.displayFrame(frame);
  }

  private void enableListeners(boolean b) {
    if (b) {
      // add listeners
      addFilterButton.addActionListener(this);
      cameraIndex.addActionListener(this);
      cameraRadio.addActionListener(this);
      capture.addActionListener(this);
      pause.addActionListener(this);
      fileRadio.addActionListener(this);
      grabberTypeSelect.addActionListener(this);
      open.addActionListener(this);
      recordButton.addActionListener(this);
      recordFrameButton.addActionListener(this);
      removeFilterButton.addActionListener(this);
      url.addActionListener(this);
      undock.addActionListener(this);
    } else {
      // remove listeners
      addFilterButton.removeActionListener(this);
      cameraIndex.removeActionListener(this);
      cameraRadio.removeActionListener(this);
      capture.removeActionListener(this);
      pause.removeActionListener(this);
      fileRadio.removeActionListener(this);
      grabberTypeSelect.removeActionListener(this);
      open.removeActionListener(this);
      recordButton.removeActionListener(this);
      recordFrameButton.removeActionListener(this);
      removeFilterButton.removeActionListener(this);
      url.removeActionListener(this);
      undock.removeActionListener(this);
    }
  }

  @Override
  public VideoWidget getLocalDisplay() {
    return video0;
  }

  public void onOpenCVData(OpenCVData data) {
    // Needed to avoid null pointer exception when
    // using RemoteAdapter
    if (cframe != null) {
      cframe.showImage(converter.convert(data.getImage()));
    } else {
      video0.displayFrame(new SerializableImage(data.getDisplay(), data.getSelectedFilter()));
    }
  }

  /**
   * onState is an interface function which allow the interface of the SwingGui
   * Bound service to update graphical portions of the SwingGui based on data
   * changes.
   * 
   * The entire service is sent and it is this functions responsibility to
   * update all of the gui components based on data elements and/or method of
   * the service.
   * 
   * onState get's its Service directly if the gui is operating "in process". If
   * the gui is operating "out of process" a serialized (zombie) process is sent
   * to provide the updated state information. Typically "publishState" is the
   * function which provides the event for onState.
   * 
   *  @param opencv - the OpenCV service
   *  
   */  
  public void onState(final OpenCV opencv) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        enableListeners(false);

        // seems pretty destructive :P
        currentFilterListModel.clear();
        // add new filters from service into gui
        for (OpenCVFilter f : opencv.getFilters()) {
          ComboBoxModel2.removeSource(boundServiceName + "." + f.name);
          addFilterToGui(f);
        }

        currentFilters.repaint();
        grabberTypeSelect.setSelectedItem(opencv.getGrabberType());

        if (opencv.isCapturing()) {
          capture.setText("stop");
          pause.setText("pause");
          // pause.setVisible(true);
          setChildrenEnabled(captureCfg, false);
        } else {
          capture.setText("capture");
          pause.setText("resume");
          // pause.setVisible(false);
          setChildrenEnabled(captureCfg, true);
        }

        if (opencv.isRecording()) {
          recordButton.setText("stop recording");
        } else {
          recordButton.setText("record");
        }

        inputFile.setText(opencv.getInputFile());
        cameraIndex.setSelectedIndex(opencv.getCameraIndex());
        String inputSource = opencv.getInputSource();
        if (OpenCV.INPUT_SOURCE_CAMERA.equals(inputSource)) {
          cameraRadio.setSelected(true);
        } else if (OpenCV.INPUT_SOURCE_CAMERA.equals(inputSource)) {
          fileRadio.setSelected(true);
        } else if (OpenCV.INPUT_SOURCE_PIPELINE.equals(inputSource)) {
          grabberTypeSelect.setSelectedItem("Pipeline");
          pipelineHook.setSelectedItem(inputSource);
        } else if (OpenCV.INPUT_SOURCE_FILE.equals(inputSource)) {
          fileRadio.setSelected(true);
        }

        currentFilters.setSelectedValue(opencv.getDisplayFilter(), true);

        if (opencv.isUndocked() == true) {
          cframe = new CanvasFrame("canvas frame");
        } else {
          if (cframe != null) {
            cframe.dispose();
            cframe = null;
          }
        }
        enableListeners(true);

        // changing a filter "broadcastState()"
        // which might change dimension of video feed
        // which might need to re-pack & re-paint components ...
        swingGui.pack();
      } // end run()
    });
  }

  public void removeAllFiltersFromGUI() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        currentFilterListModel.removeAllElements();
      }
    });
  }

  public void removeFilterFromGui(final String name) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        currentFilterListModel.removeElement(name);
      }
    });
  }

  // TODO - put in util class
  private void setChildrenEnabled(Container container, boolean enabled) {
    for (int i = 0; i < container.getComponentCount(); i++) {
      Component comp = container.getComponent(i);
      comp.setEnabled(enabled);
      if (comp instanceof Container)
        setChildrenEnabled((Container) comp, enabled);
    }
  }

  // MouseListener mouseListener = new MouseAdapter() {
  public void setCurrentFilterMouseListener() {
    MouseListener mouseListener = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        JList theList = (JList) mouseEvent.getSource();
        if (mouseEvent.getClickCount() == 2) {
          int index = theList.locationToIndex(mouseEvent.getPoint());
          if (index >= 0) {
            Object o = theList.getModel().getElementAt(index);
            log.info("Double-clicked on: {} Toggling filter enabled.", o);
            send("toggleFilter", o.toString());
          }
        }
      }
    };
    currentFilters.addMouseListener(mouseListener);
  }

  public void setFilterState(FilterWrapper filterData) {
    if (guiFilters.containsKey(filterData.name)) {
      OpenCVFilterGui gui = guiFilters.get(filterData.name);
      gui.getFilterState(filterData);
    } else {
      log.error(filterData.name + " does not contain a gui");
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("publishOpenCVData");
    subscribe("getKeys");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishOpenCVData");
    unsubscribe("getKeys");
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (!e.getValueIsAdjusting()) {
      OpenCVFilterGui filter = currentFilters.getSelectedValue();
      log.info("gui valuechange setting to {}", filter);
      if (filter != null) {
        send("setDisplayFilter", filter.name);
        filterGuiDisplay.removeAll();
        filterGuiDisplay.add(filter.getDisplay());
        filterGuiDisplay.repaint();
        filterGuiDisplay.validate();
      } else {
        send("setDisplayFilter", INPUT_KEY);
        filterGuiDisplay.removeAll();
        filterGuiDisplay.add(new JLabel("no filter selected"));
        filterGuiDisplay.repaint();
        filterGuiDisplay.validate();
      }
    }
  }

}
