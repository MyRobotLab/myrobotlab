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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class SwingOutMethodDialog extends JDialog implements ActionListener {

  class AnnotationComboBoxToolTipRenderer extends BasicComboBoxRenderer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("unchecked")
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
        if (-1 < index) {
          // list.setToolTipText(tooltips[index]);

          try {
            MethodData md = data.get(index);
            Class c = Class.forName(String.format("org.myrobotlab.service.%s", md.canonicalName));
            Method m = null;
            if (md.methodEntry.parameterTypes == null) {
              log.info("paramterType is null");
              m = c.getMethod(md.methodEntry.getName());
            } else {
              m = c.getMethod(md.methodEntry.getName(), md.methodEntry.parameterTypes);
            }
            ToolTip anno = m.getAnnotation(ToolTip.class);
            if (anno != null) {
              list.setToolTipText(anno.value());
            } else {
              list.setToolTipText("annotation not available");
            }
            // System.out.println(anno.stringValue() + " " +
            // anno.intValue());
          } catch (Exception e) {
            list.setToolTipText("method or class not available");
            log.error("{}", e);
          }

          // list.setToolTipText(data.get(index).toString());
          // list.setToolTipText(index + "");
          // list.setToolTipText(value.toString());
        }
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      setFont(list.getFont());
      setText((value == null) ? "" : value.toString());
      return this;
    }
  }

  public class MethodData {
    String canonicalName = null;
    MethodEntry methodEntry = null;

    public MethodData(MethodEntry me, String cn) {
      this.methodEntry = me;
      this.canonicalName = cn;
    }

    @Override
    public String toString() {
      return canonicalName + "." + methodEntry;
    }
  }

  public final static Logger log = LoggerFactory.getLogger(SwingOutMethodDialog.class);
  private static final long serialVersionUID = 1L;

  SwingGui myService = null;

  SwingGraphVertex v = null; // vertex who generated this dialog

  ArrayList<MethodData> data = new ArrayList<MethodData>();

  public SwingOutMethodDialog(SwingGui myService, String title, SwingGraphVertex v) {
    super(myService.getFrame(), title, true);
    this.v = v;
    this.myService = myService;
    JFrame parent = myService.getFrame();
    if (parent != null) {
      Dimension parentSize = parent.getSize();
      Point p = parent.getLocation();
      setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
    }

    // TreeMap<String,MethodEntry> m = new TreeMap<String,
    // MethodEntry>(myService.getHostCFG().getMethodMap(v.getName()));
    TreeMap<String, MethodEntry> m = new TreeMap<String, MethodEntry>(Runtime.getMethodMap(v.name));

    JComboBox<String> combo = new JComboBox<String>();
    combo.addActionListener(this);
    Iterator<String> sgi = m.keySet().iterator();
    // combo.addItem(""); // add empty
    while (sgi.hasNext()) {
      String methodName = sgi.next();
      MethodEntry me = m.get(methodName);
      data.add(new MethodData(me, v.canonicalName));
      combo.addItem(formatOutMethod(me));
    }

    combo.setRenderer(new AnnotationComboBoxToolTipRenderer());

    getContentPane().add(combo, BorderLayout.SOUTH);

    pack();
    setVisible(true);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JComboBox<Object> cb = (JComboBox<Object>) e.getSource();
    String method = (String) cb.getSelectedItem();
    log.error(method);
    myService.setSrcServiceName(v.name);
    myService.setPeriod0(".");
    myService.setSrcMethodName(method);
    myService.setArrow(" -> ");

    myService.setDstServiceName("");
    myService.setPeriod1("");
    myService.setDstMethodName("");

    // myService.srcMethodName = method.split(regex)
    // myService.parameterList =
    this.dispose();
  }

  public String formatOutMethod(MethodEntry me) {
    if (me.returnType == null || me.returnType == void.class) {
      return me.getName();
    } else {
      String p = me.returnType.getCanonicalName();
      String t[] = p.split("\\.");
      return (me.getName() + " -> " + t[t.length - 1]);
    }
  }

}
