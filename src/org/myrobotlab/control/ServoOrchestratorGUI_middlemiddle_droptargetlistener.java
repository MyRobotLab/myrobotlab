package org.myrobotlab.control;

import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

/**
 * source modified from:
 * http://bryanesmith.com/docs/drag-and-drop-java-5/DragAndDropPanelsDemo.java
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestratorGUI_middlemiddle_droptargetlistener implements DropTargetListener {

    private final ServoOrchestratorGUI_middlemiddle_rootpanel rootPanel;

    /**
     * <p>
     * Two cursors with which we are primarily interested while dragging:</p>
     * <ul>
     * <li>Cursor for droppable condition</li>
     * <li>Cursor for non-droppable consition</li>
     * </ul>
     * <p>
     * After drop, we manually change the cursor back to default, though does
     * this anyhow -- just to be complete.</p>
     */
    private static final Cursor droppableCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
            notDroppableCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    public ServoOrchestratorGUI_middlemiddle_droptargetlistener(ServoOrchestratorGUI_middlemiddle_rootpanel sheet) {
        this.rootPanel = sheet;
    }

    // Could easily find uses for these, like cursor changes, etc.
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
        if (!this.rootPanel.getCursor().equals(droppableCursor)) {
            this.rootPanel.setCursor(droppableCursor);
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
        this.rootPanel.setCursor(notDroppableCursor);
    }

    /**
     * <p>
     * The user drops the item. Performs the drag and drop calculations and
     * layout.</p>
     *
     * @param dtde
     */
    public void drop(DropTargetDropEvent dtde) {

        System.out.println("Step 5 of 7: The user dropped the panel. The drop(...) method will compare the drops location with other panels and reorder the panels accordingly.");

        // Done with cursors, dropping
        this.rootPanel.setCursor(Cursor.getDefaultCursor());

        // Just going to grab the expected DataFlavor to make sure
        // we know what is being dropped
        DataFlavor dragAndDropPanelFlavor = null;

        Object transferableObj = null;
        Transferable transferable = null;

        try {
            // Grab expected flavor
            dragAndDropPanelFlavor = ServoOrchestratorGUI_middlemiddle_main.getDragAndDropPanelDataFlavor();

            transferable = dtde.getTransferable();
            DropTargetContext c = dtde.getDropTargetContext();

            // What does the Transferable support
            if (transferable.isDataFlavorSupported(dragAndDropPanelFlavor)) {
                transferableObj = dtde.getTransferable().getTransferData(dragAndDropPanelFlavor);
            }

        } catch (Exception ex) {
        }

        // If didn't find an item, bail
        if (transferableObj == null) {
            return;
        }

        // Cast it to the panel. By this point, we have verified it is 
        // a ServoOrchestratorGUI_middlemiddle_panel.
        ServoOrchestratorGUI_middlemiddle_panel droppedPanel = (ServoOrchestratorGUI_middlemiddle_panel) transferableObj;

        //Get y and y locations
        final int dropXLoc = dtde.getLocation().x;
        final int dropYLoc = dtde.getLocation().y;

        int width = droppedPanel.getWidth();
        int height = droppedPanel.getHeight();

        //calculate x and y position - 1
        int restx = dropXLoc % width;
        int resty = dropYLoc % height;

        //calculate x and y position - 2
        int posx = ((dropXLoc - restx) / width) - 1;
        int posy = ((dropYLoc - resty) / height) - 1;

        //Getting the ID of the panel
        int dpid = droppedPanel.id;

        int sizemax = rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels().length + rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[0].length;
        if (dpid <= sizemax) {
            return;
        }

        int f1 = 0;
        int f2 = 0;

        //searching for it's orign
        for (int i1 = 0; i1 < rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels().length; i1++) {
            for (int i2 = 0; i2 < rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[i1].length; i2++) {
                ServoOrchestratorGUI_middlemiddle_panel p = rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[i1][i2];
                if (p != null && dpid == p.id) {
                    f1 = i1;
                    f2 = i2;
                }
            }
        }

        if (posx == -1 && posy == -1) {
            //Deleting position - delete
            rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[f1][f2] = null;
        } else {
            //Move the panel
            ServoOrchestratorGUI_middlemiddle_panel pold = rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[posx][posy];

            //TODO - change other attributes, too
            //TODO - make the channelid independent of the y-position (posy)
            droppedPanel.servo_channelid.setText("CH" + (posy + 1));
            rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[posx][posy] = droppedPanel;

            rootPanel.getDragAndDropPanelsDemo().getRandomDragAndDropPanels()[f1][f2] = pold;
        }

        this.rootPanel.getDragAndDropPanelsDemo().relayout();
    }
}
