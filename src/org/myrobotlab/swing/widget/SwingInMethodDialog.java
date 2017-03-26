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

package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.SwingGuiGui;
import org.slf4j.Logger;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

public class SwingInMethodDialog extends JDialog implements ActionListener {

  public final static Logger log = LoggerFactory.getLogger(SwingOutMethodDialog.class);

  private static final long serialVersionUID = 1L;

  SwingGui myService = null;
  SwingGraphVertex v = null; // vertex who generated this dialog

  public SwingInMethodDialog(SwingGui myService, String title, SwingGraphVertex v) {
    super(myService.getFrame(), title, true);
    this.v = v;
    this.myService = myService;
    JFrame parent = myService.getFrame();
    if (parent != null) {
      Dimension parentSize = parent.getSize();
      Point p = parent.getLocation();
      setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
    }

    TreeMap<String, MethodEntry> m = new TreeMap<String, MethodEntry>(Runtime.getMethodMap(v.name));

    JComboBox<String> combo = new JComboBox<String>();
    combo.addActionListener(this);
    Iterator<String> sgi = m.keySet().iterator();
    combo.addItem(""); // add empty
    while (sgi.hasNext()) {
      String methodName = sgi.next();
      MethodEntry me = m.get(methodName);
      combo.addItem(formatOutMethod(me));
    }

    getContentPane().add(combo, BorderLayout.SOUTH);

    pack();
    setVisible(true);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JComboBox<Object> cb = (JComboBox<Object>) e.getSource();
    String method = (String) cb.getSelectedItem();
    log.error("method is " + method);
    myService.setDstServiceName(v.name);
    myService.setPeriod0(".");
    myService.setDstMethodName(method);

    log.info("{}", e);

    // myService.srcMethodName = method.split(regex)
    // myService.parameterList =

    // TODO - send addListener !!!

    if (method != null && method.length() > 0) {
      // clean up methods (TODO - this is bad and should be done correctly
      // - at the source)
      // listener.getName() = myService.getDstServiceName();
      // listener.outMethod = myService.getSrcMethodName().split(" ")[0];
      // listener.inMethod = myService.getDstMethodName().split(" ")[0];
      MRLListener listener = new MRLListener(myService.getSrcMethodName().split(" ")[0], myService.getDstServiceName(), myService.getDstMethodName().split(" ")[0] // this
      // is
      // not
      // being
      // filled
      // in
      // -
      // TODO
      // -
      // fix
      // parameter
      // list
      );

      log.error("MRLListener !!! " + listener);
      /*
       * if (parameterType != null) { listener.paramTypes = new
       * Class[]{parameterType}; }
       */
      // send the notification of new route to the target system
      String srcService = myService.getSrcServiceName();
      myService.send(srcService, "addListener", listener);

      mxGraph graph = myService.getGraph();
      Object parent = graph.getDefaultParent();
      HashMap<String, mxCell> serviceCells = myService.getCells();
      graph.insertEdge(parent, null, SwingGuiGui.formatMethodString(listener.topicMethod, listener.callbackMethod), serviceCells.get(srcService),
          serviceCells.get(listener.callbackName));

      this.dispose();
    }
  }

  public String formatOutMethod(MethodEntry me) {
    StringBuffer ret = new StringBuffer();
    ret.append(me.getName());
    if (me.parameterTypes != null) {
      ret.append(" (");
      for (int i = 0; i < me.parameterTypes.length; ++i) {
        String p = me.parameterTypes[i].getCanonicalName();
        String t[] = p.split("\\.");
        ret.append(t[t.length - 1]);
        if (i < me.parameterTypes.length - 1) {
          ret.append(","); // TODO - NOT POSSIBLE TO CONNECT IN
          // SwingGui -
          // FILTER OUT?
        }
      }

      ret.append(")");
    }

    return ret.toString();
  }

}
