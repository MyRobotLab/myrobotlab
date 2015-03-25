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
 * References :
 * 	http://libjgraphx-java.sourcearchive.com/documentation/1.7.0.7/classcom_1_1mxgraph_1_1util_1_1mxConstants.html
 * */

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.myrobotlab.control.GUIServiceGraphVertex.Type;
import org.myrobotlab.control.widget.Style;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.w3c.dom.Document;

import com.mxgraph.io.mxCellCodec;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;

public class GUIDynamicGUI extends GUIServiceGUI {

	class ButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JButton b = (JButton) e.getSource();
			if (b == rebuildButton) {
				rebuildGraph();
			} else if (b == hideRoutesButton) {
				if (b.getText().equals("show routes")) {
					b.setText("hide routes");
					showRoutes = true;
				} else {
					b.setText("show routes");
					showRoutes = false;
				}
				rebuildGraph();
			} else if (b == showRouteLabelsButton) {
				if (b.getText().equals("show route labels")) {
					b.setText("hide route labels");
					showRouteLabels = true;
				} else {
					b.setText("show route labels");
					showRouteLabels = false;
				}
				rebuildGraph();
			} else if (b == accessURLButton) {
				if (b.getText().equals("show access URLs")) {
					b.setText("hide access URLs");
					showAccessURLs = true;
				} else {
					b.setText("show access URLs");
					showAccessURLs = false;
				}
				rebuildGraph();
			} else if (b == dumpButton) {
				try {
					FileIO.stringToFile(String.format("serviceRegistry.%s.txt", Runtime.getInstance().getName()), Runtime.dump());
					FileIO.stringToFile(String.format("notifyEntries.%s.xml", Runtime.getInstance().getName()), Runtime.dumpNotifyEntries());
				} catch (Exception ex) {
					Logging.logError(ex);
				}
			}
		}
	}

	static final long serialVersionUID = 1L;

	final int PORT_DIAMETER = 20;

	final int PORT_RADIUS = PORT_DIAMETER / 2;
	// addListener structure begin -------------
	public JLabel srcServiceName = new JLabel("             ");
	public JLabel srcMethodName = new JLabel("             ");
	public JLabel parameterList = new JLabel("             ");
	public JLabel dstMethodName = new JLabel();
	public JLabel dstServiceName = new JLabel();
	public JLabel period0 = new JLabel(" ");
	public JLabel period1 = new JLabel(" ");

	public JLabel arrow0 = new JLabel(" ");

	// public JLabel arrow1 = new JLabel(" ");
	// addListener structure end -------------
	ButtonListener buttonListener = new ButtonListener();
	boolean showRoutes = false; // DEPRICATE - ITS NOT NORMALIZED !!!!
	boolean showRouteLabels = false;
	boolean showAccessURLs = false;
	public HashMap<String, mxCell> serviceCells = new HashMap<String, mxCell>();
	public mxGraph graph = null;
	mxCell currentlySelectedCell = null;

	mxGraphComponent graphComponent = null;
	JButton rebuildButton = new JButton("rebuild");
	JButton hideRoutesButton = new JButton("show routes");
	JButton accessURLButton = new JButton("show access URLs");
	JButton showRouteLabelsButton = new JButton("show route labels");

	JButton dumpButton = new JButton("dump");

	public static String formatMethodString(String out, Class<?>[] paramTypes, String in) {
		// test if outmethod = in
		String methodString = out;
		// if (methodString != in) {
		methodString += "->" + in;
		// }

		// TODO FYI - depricate MRLListener use MethodEntry
		// These parameter types could always be considered "inbound" ? or
		// returnType
		// TODO - view either full named paths or shortnames

		methodString += "(";

		if (paramTypes != null) {
			for (int j = 0; j < paramTypes.length; ++j) {
				// methodString += paramTypes[j].getCanonicalName();
				Class c = paramTypes[j];
				String t[] = c.getCanonicalName().split("\\.");
				methodString += t[t.length - 1];

				if (j < paramTypes.length - 1) {
					methodString += ",";
				}
			}
		}

		methodString += ")";

		return methodString;
	}

	public GUIDynamicGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
	}

	@Override
	public void attachGUI() {
	}

	@Override
	public void buildGraph() {
		log.info("buildGraph");
		// -------------------------BEGIN PURE JGRAPH
		// ----------------------------

		if (myService.getGraphXML() == null || myService.getGraphXML().length() == 0) {
			if (graph == null) {
				graph = getNewMXGraph();
			}

			// new graph !
			graph.getModel().beginUpdate();
			try {
				buildLocalServiceGraph();
				if (showRoutes) {
					buildLocalServiceRoutes();
				}
			} finally {
				graph.getModel().endUpdate();
			}

		} else {
			// we have serialized version of graph...
			// de-serialize it

			// register
			mxCodecRegistry.addPackage("org.myrobotlab.control");
			mxCodecRegistry.register(new mxCellCodec(new org.myrobotlab.control.GUIServiceGraphVertex()));
			mxCodecRegistry.register(new mxCellCodec(Type.INPORT));

			// load
			Document document = mxUtils.parseXml(myService.getGraphXML());

			mxCodec codec2 = new mxCodec(document);
			graph = getNewMXGraph();
			codec2.decode(document.getDocumentElement(), graph.getModel());

			Object parent = graph.getDefaultParent();
			Object services[] = graph.getChildVertices(parent);
			log.info("serviceCount " + services.length);

			for (int i = 0; i < services.length; ++i) {
				// serviceCells
				Object s = services[i];
				log.info("service {}", s);

				mxCell m = (mxCell) services[i];
				GUIServiceGraphVertex v = (GUIServiceGraphVertex) m.getValue();
				log.info(v.name);
				serviceCells.put(v.name, m);
				// serviceCells.put(arg0, s.);
			}
		}

		// TODO - get # of services to set size?
		graph.setMinimumGraphSize(new mxRectangle(0, 0, 600, 400));

		// Sets the default edge style
		// list of styles -
		// http://libjgraphx-java.sourcearchive.com/documentation/1.7.0.7/classcom_1_1mxgraph_1_1util_1_1mxConstants.html
		Map<String, Object> style = graph.getStylesheet().getDefaultEdgeStyle();
		style.put(mxConstants.STYLE_EDGE, mxEdgeStyle.EntityRelation);// .ElbowConnector
		style.put(mxConstants.STYLE_STROKECOLOR, "black");// .ElbowConnector
		style.put(mxConstants.STYLE_EDITABLE, "0");// .ElbowConnector
		style.put(mxConstants.STYLE_MOVABLE, "0");// .ElbowConnector

		// creating JComponent
		if (graphComponent == null) {
			graphComponent = new mxGraphComponent(graph);
			// graphPanel.add(graphComponent);
			display.add(graphComponent, BorderLayout.CENTER);
			// graphComponent.addKeyListener(this);

			// graphComponent.getGraphControl().addMouseListener(this);

			graphComponent.getGraphControl().addMouseMotionListener(new MouseMotionListener() {

				@Override
				public void mouseDragged(MouseEvent e) {
					Object cell = graphComponent.getCellAt(e.getX(), e.getY());
					// too chatty log.info("dragged cell " + cell + " " +
					// e.getX() + "," + e.getY());
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					Object cell = graphComponent.getCellAt(e.getX(), e.getY());
					// too chatty log.info("dragged - mouseMoved - cell " + cell
					// + " " + e.getX() + "," + e.getY());
				}
			});

			graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

				@Override
				public void mouseReleased(MouseEvent e) {
					Object cell = graphComponent.getCellAt(e.getX(), e.getY());
					// too chatty log.info("cell " + e.getX() + "," + e.getY());
					currentlySelectedCell = (mxCell) cell;

					if (cell != null) {
						mxCell m = (mxCell) cell;
						// too chatty log.info("cell=" + graph.getLabel(cell) +
						// ", " + m.getId() + ", "
						// + graph.getLabel(m.getParent()));
						if (m.isVertex()) {
							// TODO - edges get filtered through here too - need
							// to process - (String) type
							GUIServiceGraphVertex v = (GUIServiceGraphVertex) m.getValue();
							if (v.displayName.equals("out")) {
								new GUIServiceOutMethodDialog(myService, "out method", v);
							} else if (v.displayName.equals("in")) {
								new GUIServiceInMethodDialog(myService, "in method", v);
							}
						} else if (m.isEdge()) {
							log.info("isEdge");
						}

					}
				}
			});

			graphComponent.setToolTips(true);

		}

		// -------------------------END PURE
		// JGRAPH--------------------------------------

	}

	@Override
	public void buildLocalServiceGraph() {

		log.info("buildLocalServiceGraph-begin");
		HashMap<String, ServiceInterface> services = Runtime.getRegistry();
		log.info("GUIServiceGUI service count " + Runtime.getRegistry().size());

		TreeMap<String, ServiceInterface> sortedMap = new TreeMap<String, ServiceInterface>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		int x = 20;
		int y = 20;

		Object parent = graph.getDefaultParent();
		serviceCells.clear();

		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceInterface sw = services.get(serviceName);
			String displayName;
			String toolTip;
			String canonicalName;

			if (sw == null) {
				canonicalName = "unknown";
				displayName = serviceName + "\n" + "unknown";
				toolTip = "";
			} else {
				canonicalName = sw.getSimpleName();
				displayName = serviceName + "\n\n\n\n\n.";// +
				// sw.get().getSimpleName();
				toolTip = sw.getDescription();
			}

			String blockColor = null;

			if (sw.getInstanceId() == null) {
				blockColor = mxUtils.getHexColorString(Style.background);
			} else {
				blockColor = mxUtils.getHexColorString(Style.remoteBackground);
			}

			if (showAccessURLs) {
				displayName = sw.getInstanceId() + "\n" + displayName;
			}

			mxCell v1 = (mxCell) graph.insertVertex(parent, null, new GUIServiceGraphVertex(serviceName, canonicalName, displayName, toolTip, GUIServiceGraphVertex.Type.SERVICE),
					x, y, 100, 50, "shape=image;image=/resource/" + canonicalName + ".png");
			// "ROUNDED;fillColor=" + blockColor);

			// graphComponent.getGraphControl().scrollRectToVisible(new
			// Rectangle(0, 0, 900, 800), true);

			serviceCells.put(serviceName, v1);

			v1.setConnectable(false);
			mxGeometry geo = graph.getModel().getGeometry(v1);
			// The size of the rectangle when the minus sign is clicked
			geo.setAlternateBounds(new mxRectangle(20, 20, 100, 50));

			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			// Because the origin is at upper left corner, need to translate to
			// position the center of port correctly
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);

			mxCell inport = new mxCell(new GUIServiceGraphVertex(serviceName, canonicalName, "in", toolTip, GUIServiceGraphVertex.Type.INPORT), geo1,
					"shape=ellipse;perimter=ellipsePerimeter;fillColor=" + blockColor);

			inport.setVertex(true);

			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);

			mxCell outport = new mxCell(new GUIServiceGraphVertex(serviceName, canonicalName, "out", toolTip, GUIServiceGraphVertex.Type.OUTPORT), geo2,
					"shape=ellipse;perimter=ellipsePerimeter;fillColor=" + blockColor);

			outport.setVertex(true);

			graph.addCell(inport, v1);
			graph.addCell(outport, v1);

			x += 150;
			if (x > 800) {
				y += 150;
				x = 20;
			}
		}

		log.info("buildLocalServiceGraph-end");
	}

	// FIXME - return a "copy" of registry ????
	// versus sunchronize on it?
	@Override
	public synchronized void buildLocalServiceRoutes() {
		Iterator<String> it = Runtime.getRegistry().keySet().iterator();
		Object parent = graph.getDefaultParent();

		// FIXME either getServiceWrapper & getNotifyList need to return copies
		// - or they need
		// to be implemented with type safe collections -
		// "copies are probably preferred"
		while (it.hasNext()) {
			String serviceName = it.next();

			ServiceInterface s = Runtime.getService(serviceName);
			if (s != null) {
				Iterator<String> ri = s.getNotifyListKeySet().iterator();
				while (ri.hasNext()) {
					ArrayList<MRLListener> nl = s.getNotifyList(ri.next());
					for (int i = 0; i < nl.size(); ++i) {
						MRLListener listener = nl.get(i);

						// ROUTING LABELS
						if (showRouteLabels) {
							mxCell c = (mxCell) graph.insertEdge(parent, null, formatMethodString(listener.outMethod, listener.paramTypes, listener.inMethod),
									serviceCells.get(s.getName()), serviceCells.get(listener.name));
						} else {
							mxCell c = (mxCell) graph.insertEdge(parent, null, "", serviceCells.get(s.getName()), serviceCells.get(listener.name));
						}
					}
				}
			} else {
				log.error("can not add graphic routes, since " + serviceName + "'s type is unknown");
			}
		}
	}

	@Override
	public void clearGraph() {
		graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
		buildGraph();
	}

	@Override
	public void detachGUI() {
	}

	@Override
	public mxGraph getNewMXGraph() {
		mxGraph g = new mxGraph() {

			// Implements a tooltip that shows the actual
			// source and target of an edge
			@Override
			public String getToolTipForCell(Object cell) {
				if (model.isEdge(cell)) {
					return convertValueToString(model.getTerminal(cell, true)) + " -> " + convertValueToString(model.getTerminal(cell, false));
				}

				mxCell m = (mxCell) cell;

				GUIServiceGraphVertex sw = (GUIServiceGraphVertex) m.getValue();
				if (sw != null) {
					return sw.toolTip;
				} else {
					return "<html>port node<br>click to drag and drop static routes</html>";
				}
			}

			// Removes the folding icon and disables any folding
			@Override
			public boolean isCellFoldable(Object cell, boolean collapse) {
				// return true;
				return false;
			}

			// Ports are not used as terminals for edges, they are
			// only used to compute the graphical connection point
			@Override
			public boolean isPort(Object cell) {
				mxGeometry geo = getCellGeometry(cell);

				return (geo != null) ? geo.isRelative() : false;
			}
		};

		return g;
	}

	// FIXME - no longer needed (remove)
	public JButton getRefreshServicesButton() {
		JButton button = new JButton("refresh services");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
			}

		});

		return button;

	}

	@Override
	public void init() {

		display.setLayout(new BorderLayout());

		JPanel top = new JPanel();
		JPanel newRoute = new JPanel(new FlowLayout());
		newRoute.setBorder(BorderFactory.createTitledBorder("new message route"));
		newRoute.add(srcServiceName);
		newRoute.add(period0);
		newRoute.add(srcMethodName);
		newRoute.add(arrow0);
		newRoute.add(dstServiceName);
		newRoute.add(period1);
		newRoute.add(dstMethodName);

		buildGraph();

		// begin graph view buttons
		JPanel filters = new JPanel();
		filters.add(rebuildButton);
		filters.add(hideRoutesButton);
		filters.add(showRouteLabelsButton);
		filters.add(accessURLButton);
		filters.add(dumpButton);

		top.add(newRoute);
		top.add(filters);

		display.add(top, BorderLayout.PAGE_START);

		accessURLButton.addActionListener(buttonListener);
		rebuildButton.addActionListener(buttonListener);
		hideRoutesButton.addActionListener(buttonListener);
		showRouteLabelsButton.addActionListener(buttonListener);
		dumpButton.addActionListener(buttonListener);
	}

	@Override
	public void rebuildGraph() {
		graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
		buildGraph();
	}

}
