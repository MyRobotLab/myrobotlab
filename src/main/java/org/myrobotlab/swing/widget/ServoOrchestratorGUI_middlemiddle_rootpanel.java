package org.myrobotlab.swing.widget;

import java.awt.dnd.DropTarget;

import javax.swing.JPanel;

/**
 * source modified from:
 * http://bryanesmith.com/docs/drag-and-drop-java-5/DragAndDropPanelsDemo.java
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestratorGUI_middlemiddle_rootpanel extends JPanel {

  private static final long serialVersionUID = 1L;

  private final ServoOrchestratorGUI_middlemiddle_main demo;

  ServoOrchestratorGUI_middlemiddle_rootpanel(ServoOrchestratorGUI_middlemiddle_main demo) {
    super();

    // Need to keep reference so can later communicate with drop listener
    this.demo = demo;

    // Again, needs to negotiate with the draggable object
    this.setTransferHandler(new ServoOrchestratorGUI_middlemiddle_transferhandler());

    // Create the listener to do the work when dropping on this object!
    this.setDropTarget(new DropTarget(ServoOrchestratorGUI_middlemiddle_rootpanel.this,
        new ServoOrchestratorGUI_middlemiddle_droptargetlistener(ServoOrchestratorGUI_middlemiddle_rootpanel.this)));
  }

  public ServoOrchestratorGUI_middlemiddle_main getDragAndDropPanelMain() {
    return demo;
  }
}
