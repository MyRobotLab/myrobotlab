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

import static org.myrobotlab.opencv.VideoProcessor.INPUT_KEY;

import java.awt.BorderLayout;
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
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.VideoProcessor;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.myrobotlab.swing.opencv.ComboBoxModel2;
//import org.myrobotlab.swing.opencv.ComboBoxModel;
import org.myrobotlab.swing.opencv.OpenCVFilterGui;
import org.myrobotlab.swing.widget.OpenCVListAdapter;
import org.slf4j.Logger;

public class OpenCVGui extends ServiceGui implements ListSelectionListener, VideoGUISource, ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(OpenCVGui.class);

	final static String PREFIX = "OpenCVFilter";
	final static String FILTER_PACKAGE_NAME = "org.myrobotlab.swing.opencv.OpenCVFilter";

	String prefixPath = "org.bytedeco.javacv.";

	transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

	BasicArrowButton addFilterButton = new BasicArrowButton(BasicArrowButton.EAST);
	BasicArrowButton removeFilterButton = new BasicArrowButton(BasicArrowButton.WEST);

	OpenCVListAdapter popup = new OpenCVListAdapter(this);

	JList<String> possibleFilters;
	JList<OpenCVFilterGui> currentFilters;

	VideoWidget video0 = null;

	JButton capture = new JButton("capture");
	JCheckBox undock = new JCheckBox("undock");
	CanvasFrame cframe = null; // new

	// input
	JPanel captureCfg = new JPanel();
	JRadioButton fileRadio = new JRadioButton();
	JRadioButton cameraRadio = new JRadioButton();
	JTextField inputFile = new JTextField("");
	JLabel inputFileLable = new JLabel("file");
	JLabel cameraIndexLable = new JLabel("camera");
	JLabel modeLabel = new JLabel("mode");
	JButton inputFileButton = new JButton("open file");

	JComboBox<String> IPCameraType = new JComboBox<String>(new String[] { "foscam FI8918W" });
	DefaultComboBoxModel<String> pipelineHookModel = new DefaultComboBoxModel<String>();
	JComboBox<String> pipelineHook = new JComboBox<String>(pipelineHookModel);

	ButtonGroup groupRadio = new ButtonGroup();
	DefaultListModel<OpenCVFilterGui> currentFilterListModel = new DefaultListModel<OpenCVFilterGui>();

	JComboBox<String> kinectImageOrDepth = new JComboBox<String>(new String[] { "image", "depth", "interleave" });
	JComboBox<String> grabberTypeSelect = null;

	JComboBox<Integer> cameraIndex = new JComboBox<Integer>(new Integer[] { 0, 1, 2, 3, 4, 5 });
	
	// ComboBoxModel2 sources = new ComboBoxModel2();

	JPanel filterParameters = new JPanel();

	LinkedHashMap<String, OpenCVFilterGui> guiFilters = new LinkedHashMap<String, OpenCVFilterGui>();

	// output
	JButton recordButton = new JButton("record");
	JButton recordFrameButton = new JButton("record frame");

	OpenCV myOpenCV;
	final OpenCVGui self;

	private ActionListener captureListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			/**
			 * TODO - setState only done in Capture !!!!! TODO - setting all of
			 * OpenCV's actual variables should ONLY be done here otherwise
			 * invalid states may occur while a capture is running the model is
			 * to set all the data of the gui just before the capture request is
			 * sent
			 */
			VideoProcessor vp = myOpenCV.videoProcessor;

			String selected = (String) grabberTypeSelect.getSelectedItem();

			if ("IPCamera".equals(selected) || "Pipeline".equals(selected) || "ImageFile".equals(selected)
					|| "SlideShow".equals(selected) || "Sarxos".equals(selected) || "MJpeg".equals(selected)) {
				prefixPath = "org.myrobotlab.opencv.";
			} else {
				prefixPath = "org.bytedeco.javacv.";
			}

			vp.grabberType = prefixPath + (String) grabberTypeSelect.getSelectedItem() + "FrameGrabber";

			if (fileRadio.isSelected()) {
				String fileName = inputFile.getText();
				vp.inputFile = fileName;
				String extension = "";

				int i = fileName.lastIndexOf('.');
				if (i > 0) {
					extension = fileName.substring(i + 1);
				}

				File inputFile = new File(fileName);

				if (("jpg").equals(extension) || ("png").equals(extension)) {
					vp.inputSource = OpenCV.INPUT_SOURCE_IMAGE_FILE;
					vp.grabberType = "org.myrobotlab.opencv.ImageFileFrameGrabber";

				} else if (inputFile.isDirectory()) {
					// this
					// is
					// a
					// slide
					// show.
					vp.inputSource = OpenCV.INPUT_SOURCE_IMAGE_DIRECTORY;
					vp.grabberType = "org.myrobotlab.opencv.SlideShowFrameGrabber";
					send("setDirectory", inputFile);
				} else {
					vp.inputSource = OpenCV.INPUT_SOURCE_MOVIE_FILE;
				}

			} else if (cameraRadio.isSelected()) {
				vp.inputSource = OpenCV.INPUT_SOURCE_CAMERA;
				vp.cameraIndex = (Integer) cameraIndex.getSelectedItem();
			} else {
				log.error("input source is " + vp.inputSource);
			}

			if ("IPCamera".equals(selected) || "MJpeg".equals(selected)) {
				vp.inputSource = OpenCV.INPUT_SOURCE_NETWORK;
			}

			if ("Pipeline".equals(selected)) {
				vp.inputSource = OpenCV.INPUT_SOURCE_PIPELINE;
				vp.pipelineSelected = (String) pipelineHook.getSelectedItem();
			}

			send("setState", myOpenCV);

			// set
			// new
			// button
			// state
			if (("capture".equals(capture.getText()))) {
				send("capture");
				capture.setText("stop");
				// captureCfg.disable();
				setChildrenEnabled(captureCfg, false);
			} else {
				send("stopCapture");
				capture.setText("capture");
				setChildrenEnabled(captureCfg, true);
			}

		}
	};

	/**
	 * SwingGui defaults for grabber types
	 */
	private ActionListener grabberTypeListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			String type = (String) grabberTypeSelect.getSelectedItem();
			if ("OpenKinect".equals(type)) {
				cameraRadio.setSelected(true);
				cameraIndexLable.setVisible(true);
				cameraIndex.setVisible(true);
				modeLabel.setVisible(true);
				kinectImageOrDepth.setVisible(true);
				inputFileLable.setVisible(false);
				inputFile.setVisible(false);
				fileRadio.setVisible(false);

				IPCameraType.setVisible(false);
				pipelineHook.setVisible(false);
			}

			if ("OpenCV".equals(type) || "VideoInput".equals(type) || "FFmpeg".equals(type)) {
				// cameraRadio.setSelected(true);
				kinectImageOrDepth.setSelectedItem("image");
				// myOpenCV.format
				// =
				// "image";
				cameraIndexLable.setVisible(true);
				cameraIndex.setVisible(true);
				modeLabel.setVisible(false);
				kinectImageOrDepth.setVisible(false);
				inputFileLable.setVisible(true);
				inputFile.setVisible(true);

				fileRadio.setVisible(true);
				cameraRadio.setVisible(true);

				IPCameraType.setVisible(false);
				pipelineHook.setVisible(false);
			}

			if ("IPCamera".equals(type)) {
				// cameraRadio.setSelected(true);
				// kinectImageOrDepth.setSelectedItem("image");
				// myOpenCV.format
				// =
				// "image";
				cameraIndexLable.setVisible(false);
				cameraIndex.setVisible(false);
				modeLabel.setVisible(false);
				kinectImageOrDepth.setVisible(false);
				inputFileLable.setVisible(true);
				inputFile.setVisible(true);
				fileRadio.setSelected(true);

				fileRadio.setVisible(false);
				cameraRadio.setVisible(false);

				IPCameraType.setVisible(true);
				pipelineHook.setVisible(false);
			}

			if ("Pipeline".equals(type)) {
				// cameraRadio.setSelected(true);
				// kinectImageOrDepth.setSelectedItem("image");
				// myOpenCV.format
				// =
				// "image";
				cameraIndexLable.setVisible(false);
				cameraIndex.setVisible(false);
				modeLabel.setVisible(false);
				kinectImageOrDepth.setVisible(false);
				inputFileLable.setVisible(false);
				inputFile.setVisible(false);
				fileRadio.setSelected(true);

				fileRadio.setVisible(false);
				cameraRadio.setVisible(false);

				IPCameraType.setVisible(false);
				// this
				// has
				// static
				// /
				// global
				// internals
				// VideoSources vs = new VideoSources();
				// Set<String> p = vs.getKeySet();
				/*
				 * pipelineHookModel.removeAllElements(); for (String i : p) {
				 * pipelineHookModel.insertElementAt(i, 0); }
				 * pipelineHook.setVisible(true);
				 */
			}

		}
	};

	public OpenCVGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);
		self = this;

		video0 = new VideoWidget(boundServiceName, myService);

		undock.addActionListener(this);

		capture.addActionListener(captureListener);

		ArrayList<String> frameGrabberList = new ArrayList<String>();
		// Add all of the OpenCV defined FrameGrabbers
		for (int i = 0; i < FrameGrabber.list.size(); ++i) {
			String ss = FrameGrabber.list.get(i);
			String fg = ss.substring(ss.lastIndexOf(".") + 1);
			frameGrabberList.add(fg);
		}

		// Add the MRL Frame Grabbers
		frameGrabberList.add("IPCamera");
		frameGrabberList.add("Pipeline"); // service which implements
		// ImageStreamSource
		frameGrabberList.add("ImageFile");
		frameGrabberList.add("SlideShowFile");
		frameGrabberList.add("Sarxos");
		frameGrabberList.add("MJpeg");
		
		ComboBoxModel2.add(INPUT_KEY);

		// CanvasFrame cf = new CanvasFrame("hello");

		grabberTypeSelect = new JComboBox(frameGrabberList.toArray());

		kinectImageOrDepth.addActionListener(this);

		possibleFilters = new JList<String>(OpenCV.getPossibleFilters());
		possibleFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		possibleFilters.setSelectedIndex(0);
		possibleFilters.setVisibleRowCount(10);
		possibleFilters.setSize(140, 160);
		possibleFilters.addMouseListener(popup);

		currentFilters = new JList<OpenCVFilterGui>(currentFilterListModel);
		currentFilters.setFixedCellWidth(100);
		currentFilters.addListSelectionListener(this);
		currentFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		currentFilters.setSize(140, 160);
		currentFilters.setVisibleRowCount(10);

		JScrollPane currentFiltersScrollPane = new JScrollPane(currentFilters);
		JScrollPane possibleFiltersScrollPane = new JScrollPane(possibleFilters);

		JPanel videoPanel = new JPanel();
		videoPanel.add(video0.display);

		// build input begin ------------------
		JPanel input = new JPanel();

		TitledBorder title;
		title = BorderFactory.createTitledBorder("input");
		input.setBorder(title);

		groupRadio.add(cameraRadio);
		groupRadio.add(fileRadio);

		grabberTypeSelect.addActionListener(grabberTypeListener);

		// capture panel
		JPanel cpanel = new JPanel();
		cpanel.setBorder(BorderFactory.createEtchedBorder());
		cpanel.add(capture);
		cpanel.add(grabberTypeSelect);
		// cpanel.add(new JLabel(" canvas "));
		cpanel.add(undock);
		// build configuration for the various captures
		// non visible - when not applicable
		// disable when capturing
		captureCfg.setBorder(BorderFactory.createEtchedBorder());

		captureCfg.add(cameraRadio);
		captureCfg.add(cameraIndexLable);
		captureCfg.add(cameraIndex);
		captureCfg.add(modeLabel);
		captureCfg.add(kinectImageOrDepth);
		captureCfg.add(fileRadio);
		captureCfg.add(inputFileLable);
		captureCfg.add(inputFile);

		captureCfg.add(IPCameraType);
		captureCfg.add(pipelineHook);

		input.add(cpanel);

		input.add(captureCfg);

		Box inputOutput = Box.createVerticalBox();

		JPanel output = new JPanel();

		output.setBorder(BorderFactory.createTitledBorder("output"));

		output.add(recordButton);
		output.add(recordFrameButton);

		recordButton.addActionListener(this);
		recordFrameButton.addActionListener(this);

		// build input end ------------------

		// build filters begin ------------------
		addFilterButton.addActionListener(this);
		removeFilterButton.addActionListener(this);

		JPanel filterPanel = new JPanel();
		title = BorderFactory.createTitledBorder("filters: available - current");
		filterPanel.setBorder(title);
		filterPanel.add(possibleFiltersScrollPane);
		filterPanel.add(removeFilterButton);
		filterPanel.add(addFilterButton);
		filterPanel.add(currentFiltersScrollPane);

		title = BorderFactory.createTitledBorder("filter parameters");
		filterParameters.setBorder(title);
		// filterParameters.setPreferredSize(new Dimension(340, 360));
		// filterParameters.setPreferredSize(new Dimension(340, 400));
		Box box = Box.createVerticalBox();
		box.add(filterPanel);
		box.add(filterParameters);

		inputOutput.add(input);
		inputOutput.add(output);

		display.add(box, BorderLayout.EAST);
		display.add(videoPanel, BorderLayout.CENTER);
		display.add(input, BorderLayout.NORTH);
		display.add(output, BorderLayout.SOUTH);

		setCurrentFilterMouseListener();
		// build filters end ------------------

		// TODO - bury in framework?
		myOpenCV = (OpenCV) Runtime.getService(boundServiceName);

		// TODO - remove action listener?
		grabberTypeSelect.setSelectedItem("OpenCV");

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == addFilterButton) {
			addFilter();
		} else if (o == removeFilterButton) {
			OpenCVFilterGui filterGui = currentFilters.getSelectedValue();
			send("removeFilter", filterGui.toString());
			// TODO - block on response
			currentFilterListModel.removeElement(filterGui);
		} else if (o == kinectImageOrDepth) {
			// String mode = (String) kinectImageOrDepth.getSelectedItem();
			// TODO: not implemented.
			/*
			 * if ("depth".equals(mode)) { vp.format = "depth"; } else {
			 * vp.format = "image"; } // FIXME - broadcastState ???
			 */
		} else if (o == recordButton) {
			if (recordButton.getText().equals("record")) {
				// start recording
				send("recordOutput", true);
				recordButton.setText("stop recording");
			} else {
				// stop recording
				send("recordOutput", false);
				recordButton.setText("record");
			}
		} else if (o == recordFrameButton) {
			send("recordSingleFrame");
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

		// get a gui filter
		String guiType = FILTER_PACKAGE_NAME + type + "Gui";

		OpenCVFilterGui filtergui = null;

		// try creating one based on type
		filtergui = (OpenCVFilterGui) Instantiator.getNewInstance(guiType, name, boundServiceName, myService);
		if (filtergui == null) {
			log.info(String.format("filter %s does not have a gui defined", type));
			filtergui = (OpenCVFilterGui) Instantiator.getNewInstance(FILTER_PACKAGE_NAME + "DefaultGui", name,
					boundServiceName, myService);
			if (filtergui == null) {
				log.error("could not create default filter gui");
				return null;
			}
		}

		currentFilterListModel.addElement(filtergui);

		// add new input to sources
		ArrayList<String> newSources = filter.getPossibleSources();
		// DefaultComboBoxModel model = ComboBoxModel.getModel();
		for (int i = 0; i < newSources.size(); ++i) {
			ComboBoxModel2.add(String.format("%s.%s", boundServiceName, newSources.get(i)));
		}

		// set source of gui's input to
		filtergui.initFilterState(filter); // set the bound filter
		guiFilters.put(name, filtergui);
		currentFilters.setSelectedIndex(currentFilterListModel.size() - 1);
		return filtergui;
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
	
	public void onKeys(Set<String> sources){
		log.info("here");
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

	@Override
	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video0; // else return video1
	}

	/*
	 * onState is an interface function which allow the interface of the
	 * SwingGui Bound service to update graphical portions of the SwingGui based
	 * on data changes.
	 * 
	 * The entire service is sent and it is this functions responsibility to
	 * update all of the gui components based on data elements and/or method of
	 * the service.
	 * 
	 * onState get's its Service directly if the gui is operating "in process".
	 * If the gui is operating "out of process" a serialized (zombie) process is
	 * sent to provide the updated state information. Typically "publishState"
	 * is the function which provides the event for onState.
	 */
	public void onState(final OpenCV opencv) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				VideoProcessor vp = opencv.videoProcessor;

				// seems pretty destructive :P
				currentFilterListModel.clear();
				// add new filters from service into gui
				for (OpenCVFilter f : opencv.getFiltersCopy()) {
				  ComboBoxModel2.removeSource(boundServiceName+"."+f.name);
					addFilterToGui(f);
				}

				currentFilters.repaint();

				for (int i = 0; i < grabberTypeSelect.getItemCount(); ++i) {
					String currentObject = prefixPath + grabberTypeSelect.getItemAt(i) + "FrameGrabber";
					if (currentObject.equals(vp.grabberType)) {
						grabberTypeSelect.setSelectedIndex(i);
						break;
					}
				}

				if (opencv.capturing) {
					capture.setText("stop");
				} else {
					capture.setText("capture");
				}

				inputFile.setText(vp.inputFile);
				cameraIndex.setSelectedIndex(vp.cameraIndex);
				String inputSource = opencv.videoProcessor.inputSource;
				if (OpenCV.INPUT_SOURCE_CAMERA.equals(inputSource)) {
					cameraRadio.setSelected(true);
				} else if (OpenCV.INPUT_SOURCE_CAMERA.equals(inputSource)) {
					fileRadio.setSelected(true);
				} else if (OpenCV.INPUT_SOURCE_PIPELINE.equals(inputSource)) {
					// grabberTypeSelect.removeActionListener(grabberTypeListener);
					grabberTypeSelect.setSelectedItem("Pipeline");
					// grabberTypeSelect.addActionListener(grabberTypeListener);
					pipelineHook.setSelectedItem(vp.pipelineSelected);
				} else if (OpenCV.INPUT_SOURCE_IMAGE_FILE.equals(inputSource)
						|| OpenCV.INPUT_SOURCE_IMAGE_DIRECTORY.equals(inputSource)) {
					// the file input should be enabled if we are file or
					// directory.
					fileRadio.setSelected(true);
				}

				currentFilters.removeListSelectionListener(self);
				currentFilters.setSelectedValue(vp.displayFilterName, true);// .setSelectedIndex(index);
				currentFilters.addListSelectionListener(self);

				if (opencv.undockDisplay == true) {
					cframe = new CanvasFrame("canvas frame");
				} else {
					if (cframe != null) {
						cframe.dispose();
						cframe = null;
					}
				}

				// changing a filter "broadcastState()"
				// which might change dimension of video feed
				// which might need to re-pack & re-paint components ...
				myService.pack();
			} // end run()
		});

	}

	public void onOpenCVData(OpenCVData data) {
		// Needed to avoid null pointer exception when
		// using RemoteAdapter
		if (cframe != null) {
			cframe.showImage(converter.convert(data.getImage()));
		} else {
			video0.displayFrame(new SerializableImage(data.getDisplayBufferedImage(), data.getDisplayFilterName()));
		}
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
						System.out.println("Double-clicked on: " + o.toString());
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
	public void valueChanged(ListSelectionEvent e) {
		// log.debug(e);
		if (!e.getValueIsAdjusting()) {
			OpenCVFilterGui filter = currentFilters.getSelectedValue();
			log.info("gui valuechange setting to {}", filter);
			if (filter != null) {
				send("setDisplayFilter", filter.toString());
				filterParameters.removeAll();
				filterParameters.add(filter.getDisplay());
				filterParameters.repaint();
				filterParameters.validate();

			} else {
				send("setDisplayFilter", INPUT_KEY);
				// TODO - send message to OpenCV - that no filter should be sent
				// to publish
				filterParameters.removeAll();
				filterParameters.add(new JLabel("no filter selected"));
				filterParameters.repaint();
				filterParameters.validate();
			}

			// TODO - if filterName = null - it has been "un"selected ctrl-click

		}
	}

}
