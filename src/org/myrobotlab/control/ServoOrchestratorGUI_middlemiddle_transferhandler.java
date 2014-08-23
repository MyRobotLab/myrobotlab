package org.myrobotlab.control;

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
public class ServoOrchestratorGUI_middlemiddle_transferhandler extends
		TransferHandler implements DragSourceMotionListener {

	public ServoOrchestratorGUI_middlemiddle_transferhandler() {
		super();
	}

	/**
	 * <p>
	 * This creates the Transferable object. In our case,
	 * ServoOrchestratorGUI_middlemiddle_panel implements Transferable, so this
	 * requires only a type cast.
	 * </p>
	 *
	 * @param c
	 * @return
	 */
	@Override()
	public Transferable createTransferable(JComponent c) {

		System.out
				.println("Step 3 of 7: Casting the RandomDragAndDropPanel as Transferable. The Transferable RandomDragAndDropPanel will be queried for acceptable DataFlavors as it enters drop targets, as well as eventually present the target with the Object it transfers.");

		// TaskInstancePanel implements Transferable
		if (c instanceof ServoOrchestratorGUI_middlemiddle_panel) {
			Transferable tip = (ServoOrchestratorGUI_middlemiddle_panel) c;
			return tip;
		}

		// Not found
		return null;
	}

	public void dragMouseMoved(DragSourceDragEvent dsde) {
	}

	/**
	 * <p>
	 * This is queried to see whether the component can be copied, moved, both
	 * or neither. We are only concerned with copying.
	 * </p>
	 *
	 * @param c
	 * @return
	 */
	@Override()
	public int getSourceActions(JComponent c) {

		System.out
				.println("Step 2 of 7: Returning the acceptable TransferHandler action. Our RandomDragAndDropPanel accepts Copy only.");

		if (c instanceof ServoOrchestratorGUI_middlemiddle_panel) {
			return TransferHandler.COPY;
		}

		return TransferHandler.NONE;
	}
}
