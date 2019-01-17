package org.myrobotlab.swing.widget;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceMotionListener;

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
public class ServoOrchestratorGUI_middlemiddle_transferhandler extends TransferHandler implements DragSourceMotionListener {

  private static final long serialVersionUID = 1L;

  public ServoOrchestratorGUI_middlemiddle_transferhandler() {
    super();
  }

  /**
   * <p>
   * This creates the Transferable object. In our case,
   * ServoOrchestratorGUI_middlemiddle_panel implements Transferable, so this
   * requires only a type cast.
   * </p>
   */
  @Override()
  public Transferable createTransferable(JComponent c) {
    // TaskInstancePanel implements Transferable
    if (c instanceof ServoOrchestratorGUI_middlemiddle_panel) {
      Transferable tip = (ServoOrchestratorGUI_middlemiddle_panel) c;
      return tip;
    }

    // Not found
    return null;
  }

  @Override
  public void dragMouseMoved(DragSourceDragEvent dsde) {
  }

  /**
   * <p>
   * This is queried to see whether the component can be copied, moved, both or
   * neither. We are only concerned with copying.
   * </p>
   */
  @Override()
  public int getSourceActions(JComponent c) {
    if (c instanceof ServoOrchestratorGUI_middlemiddle_panel) {
      return TransferHandler.COPY;
    }

    return TransferHandler.NONE;
  }
}
