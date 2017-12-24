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

package org.myrobotlab.swing;

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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.Style;
import org.myrobotlab.swing.widget.SwingGraphVertex;
import org.myrobotlab.swing.widget.SwingGraphVertex.Type;
import org.myrobotlab.swing.widget.SwingInMethodDialog;
import org.myrobotlab.swing.widget.SwingOutMethodDialog;
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

public class SwingGuiGui extends ServiceGui implements ActionListener {

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
  public HashMap<String, mxCell> serviceCells = new HashMap<String, mxCell>();
  public mxGraph graph = null;
  mxCell currentlySelectedCell = null;

  mxGraphComponent graphComponent = null;

  JComboBox<String> desktops = new JComboBox<String>();
  JButton setDesktop = new JButton("set");
  JButton explode = new JButton("explode");
  JButton collapse = new JButton("collapse");
  JButton fullscreen = new JButton("fullscreen");

  JButton rebuild = new JButton("rebuild");
  JButton hideRoutes = new JButton("show routes");
  JButton showUrls = new JButton("show access URLs");
  JButton showRouteLabels = new JButton("show route labels");

  JButton dump = new JButton("dump");

  public static String formatMethodString(String out, String in) {
    // test if outmethod = in
    String methodString = out;
    methodString += "->" + in;
    methodString += "(";
    methodString += ")";

    return methodString;
  }

  public SwingGuiGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    JPanel newRoute = new JPanel(new FlowLayout());
    newRoute.setBorder(BorderFactory.createTitledBorder("new message route"));
    newRoute.add(srcServiceName);
    newRoute.add(period0);
    newRoute.add(srcMethodName);
    newRoute.add(arrow0);
    newRoute.add(dstServiceName);
    newRoute.add(period1);
    newRoute.add(dstMethodName);

    // myService.getDesktops();
    desktops.addItem("default");
    desktops.setEditable(true);
    desktops.addActionListener(this);

    buildGraph();

    addTop("desktop   ", desktops, setDesktop, explode, collapse, fullscreen);
    addBottomGroup(null, newRoute, rebuild, hideRoutes, showRouteLabels, showUrls, dump);

    explode.addActionListener(this);
    collapse.addActionListener(this);
    fullscreen.addActionListener(this);
    showUrls.addActionListener(this);
    rebuild.addActionListener(this);
    hideRoutes.addActionListener(this);
    showRouteLabels.addActionListener(this);
    dump.addActionListener(this);
    setDesktop.addActionListener(this);

  }

  // FIXME - should it hook to the Runtime ???
  @Override
  public void subscribeGui() {
  }

  /**
   * builds the graph
   */
  public void buildGraph() {
    log.debug("buildGraph");

    if (myService.getGraphXML() == null || myService.getGraphXML().length() == 0) {
      if (graph == null) {
        graph = getNewMXGraph();
      }

      // new graph !
      graph.getModel().beginUpdate();
      try {
        buildLocalServiceGraph();
        if (hideRoutes.getText().equals("show routes")) {
          buildLocalServiceRoutes();
        }
      } finally {
        graph.getModel().endUpdate();
      }

    } else {
      // we have serialized version of graph...
      // de-serialize it

      // register
      mxCodecRegistry.addPackage("org.myrobotlab.swing.widget");
      mxCodecRegistry.register(new mxCellCodec(new org.myrobotlab.swing.widget.SwingGraphVertex()));
      mxCodecRegistry.register(new mxCellCodec(Type.INPORT));

      // load
      Document document = mxUtils.parseXml(myService.getGraphXML());

      mxCodec codec2 = new mxCodec(document);
      graph = getNewMXGraph();
      codec2.decode(document.getDocumentElement(), graph.getModel());

      Object parent = graph.getDefaultParent();
      Object services[] = graph.getChildVertices(parent);

      for (int i = 0; i < services.length; ++i) {
        // serviceCells
        Object s = services[i];
        log.debug("service {}", s);

        mxCell m = (mxCell) services[i];
        SwingGraphVertex v = (SwingGraphVertex) m.getValue();
        log.debug(v.name);
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
      add(graphComponent);
      // graphComponent.addKeyListener(this);

      // graphComponent.getGraphControl().addMouseListener(this);

      graphComponent.getGraphControl().addMouseMotionListener(new MouseMotionListener() {

        @Override
        public void mouseDragged(MouseEvent e) {
          // TODO: this doesn't do anything.
          // Object cell = graphComponent.getCellAt(e.getX(),
          // e.getY());
          // too chatty log.debug("dragged cell " + cell + " " +
          // e.getX() + "," + e.getY());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          // TODO: this doesn't do anything.
          // Object cell = graphComponent.getCellAt(e.getX(),
          // e.getY());
          // too chatty log.debug("dragged - mouseMoved - cell " +
          // cell
          // + " " + e.getX() + "," + e.getY());
        }
      });

      graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

        /*
         * protected void mouseLocationChanged(MouseEvent e) {
         * log.debug(e.getX() + ", " + e.getY()); }
         * 
         * public void mouseDragged(MouseEvent e) { // http://forum.jgraph
         * .com/questions/1343/mouse-coordinates-at-drop-event Object cell =
         * graphComponent.getCellAt(e.getX(), e.getY()); log.debug(e.getX() +
         * "," + e.getY()); }
         */

        @Override
        public void mouseReleased(MouseEvent e) {
          Object cell = graphComponent.getCellAt(e.getX(), e.getY());
          // too chatty log.debug("cell " + e.getX() + "," +
          // e.getY());
          currentlySelectedCell = (mxCell) cell;

          if (cell != null) {
            mxCell m = (mxCell) cell;
            // too chatty log.debug("cell=" + graph.getLabel(cell) +
            // ", " + m.getId() + ", "
            // + graph.getLabel(m.getParent()));
            if (m.isVertex()) {
              // TODO - edges get filtered through here too - need
              // to process - (String) type
              SwingGraphVertex v = (SwingGraphVertex) m.getValue();
              if (v.displayName.equals("out")) {
                new SwingOutMethodDialog(myService, "out method", v);
              } else if (v.displayName.equals("in")) {
                new SwingInMethodDialog(myService, "in method", v);
              }
            } else if (m.isEdge()) {
              log.error("isEdge");
            }

          }
        }
      });

      graphComponent.setToolTips(true);

    }

    // -------------------------END PURE
    // JGRAPH--------------------------------------

  }

  public void buildLocalServiceGraph() {

    log.debug("buildLocalServiceGraph-begin");
    Map<String, ServiceInterface> services = Runtime.getRegistry();
    log.debug("SwingGUI service count " + Runtime.getRegistry().size());

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

      canonicalName = sw.getSimpleName();
      displayName = serviceName + "\n\n\n\n\n.";// +
      // sw.get().getSimpleName();
      toolTip = sw.getDescription();

      String blockColor = null;

      if (sw.getInstanceId() == null) {
        blockColor = mxUtils.getHexColorString(Style.background);
      } else {
        blockColor = mxUtils.getHexColorString(Style.remoteBackground);
      }

      if (!showUrls.getText().equals("show access URLs")) {
        displayName = sw.getInstanceId() + "\n" + displayName;
      }

      mxCell v1 = (mxCell) graph.insertVertex(parent, null, new SwingGraphVertex(serviceName, canonicalName, displayName, toolTip, SwingGraphVertex.Type.SERVICE), x, y, 100, 50,
          "shape=image;image=/resource/" + canonicalName + ".png");
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

      mxCell inport = new mxCell(new SwingGraphVertex(serviceName, canonicalName, "in", toolTip, SwingGraphVertex.Type.INPORT), geo1,
          "shape=ellipse;perimter=ellipsePerimeter;fillColor=" + blockColor);

      inport.setVertex(true);

      mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
      geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
      geo2.setRelative(true);

      mxCell outport = new mxCell(new SwingGraphVertex(serviceName, canonicalName, "out", toolTip, SwingGraphVertex.Type.OUTPORT), geo2,
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

    log.debug("buildLocalServiceGraph-end");
  }

  // FIXME - return a "copy" of registry ????
  // versus sunchronize on it?
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
            if (showRouteLabels.getText().equals("show route labels")) {
              graph.insertEdge(parent, null, formatMethodString(listener.topicMethod, listener.callbackMethod), serviceCells.get(s.getName()),
                  serviceCells.get(listener.callbackName));
            } else {
              graph.insertEdge(parent, null, "", serviceCells.get(s.getName()), serviceCells.get(listener.callbackName));
            }
          }
        }
      } else {
        log.error("can not add graphic routes, since " + serviceName + "'s type is unknown");
      }
    }
  }

  public void clearGraph() {
    graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
    buildGraph();
  }

  @Override
  public void unsubscribeGui() {
  }

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

        SwingGraphVertex sw = (SwingGraphVertex) m.getValue();
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

  public void rebuildGraph() {
    graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
    buildGraph();
  }

  /*
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState. This event handler is
   * typically used when data or state information in the service has changed,
   * and the UI should update to reflect this changed state.
   */
  public void onState(SwingGui gui) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();

    if (o == setDesktop) {
      send("setDesktop", desktops.getSelectedItem());
    }

    if (o == explode) {
      send("explode");
    }

    if (o == collapse) {
      send("collapse");
    }

    if (o == fullscreen) {
      if (fullscreen.getText().equals("fullscreen")) {
        send("fullscreen", true);
        fullscreen.setText("exit fullscreen");
      } else {
        send("fullscreen", false);
        fullscreen.setText("fullscreen");
      }
    }

    if (o == rebuild) {
      rebuildGraph();
    } else if (o == hideRoutes) {
      if (hideRoutes.getText().equals("show routes")) {
        hideRoutes.setText("hide routes");
      } else {
        hideRoutes.setText("show routes");
      }
      rebuildGraph();
    } else if (o == showUrls) {
      if (showUrls.getText().equals("show access URLs")) {
        showUrls.setText("hide access URLs");
      } else {
        showUrls.setText("show access URLs");
      }
      rebuildGraph();
    } else if (o == dump) {
      Runtime.dump();
    }
  }

}
