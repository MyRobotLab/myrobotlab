package org.myrobotlab.swing.widget;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * source modified from:
 * http://bryanesmith.com/docs/drag-and-drop-java-5/DragAndDropPanelsDemo.java
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestratorGUI_middlemiddle_draggablemouselistener extends MouseAdapter {

  @Override()
  public void mousePressed(MouseEvent e) {
    JComponent c = (JComponent) e.getSource();
    TransferHandler handler = c.getTransferHandler();
    handler.exportAsDrag(c, e, TransferHandler.COPY);
  }
}
