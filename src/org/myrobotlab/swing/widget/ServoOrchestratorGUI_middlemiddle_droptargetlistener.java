package org.myrobotlab.swing.widget;

import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import org.myrobotlab.logging.Logging;

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
   * Two cursors with which we are primarily interested while dragging:
   * </p>
   * <ul>
   * <li>Cursor for droppable condition</li>
   * <li>Cursor for non-droppable consition</li>
   * </ul>
   * <p>
   * After drop, we manually change the cursor back to default, though does this
   * anyhow -- just to be complete.
   * </p>
   */
  private static final Cursor droppableCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), notDroppableCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  public ServoOrchestratorGUI_middlemiddle_droptargetlistener(ServoOrchestratorGUI_middlemiddle_rootpanel sheet) {
    this.rootPanel = sheet;
  }

  // Could easily find uses for these, like cursor changes, etc.
  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
    this.rootPanel.setCursor(notDroppableCursor);
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
    if (!this.rootPanel.getCursor().equals(droppableCursor)) {
      this.rootPanel.setCursor(droppableCursor);
    }
  }

  /**
   * <p>
   * The user drops the item. Performs the drag and drop calculations and
   * layout.
   * </p>
   */
  @Override
  public void drop(DropTargetDropEvent dtde) {
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
      // DropTargetContext c = dtde.getDropTargetContext();

      // What does the Transferable support
      if (transferable.isDataFlavorSupported(dragAndDropPanelFlavor)) {
        transferableObj = dtde.getTransferable().getTransferData(dragAndDropPanelFlavor);
      }

    } catch (Exception ex) {
      Logging.logError(ex);
    }

    // If didn't find an item, bail
    if (transferableObj == null) {
      return;
    }

    // Cast it to the panel. By this point, we have verified it is
    // a ServoOrchestratorGUI_middlemiddle_panel.
    ServoOrchestratorGUI_middlemiddle_panel droppedPanel = (ServoOrchestratorGUI_middlemiddle_panel) transferableObj;

    // Get y and y locations
    final int dropXLoc = dtde.getLocation().x;
    final int dropYLoc = dtde.getLocation().y;

    int width = droppedPanel.getWidth();
    int height = droppedPanel.getHeight();

    // calculate x and y position - 1
    int restx = dropXLoc % width;
    int resty = dropYLoc % height;

    // calculate x and y position - 2
    int posx = ((dropXLoc - restx) / width) - 1;
    int posy = ((dropYLoc - resty) / height) - 1;

    // Getting the ID of the panel
    int dpid = droppedPanel.id;

    int sizemax = rootPanel.getDragAndDropPanelMain().panels.length + rootPanel.getDragAndDropPanelMain().panels[0].length;
    if (dpid <= sizemax) {
      return;
    }

    int f1 = 0;
    int f2 = 0;

    // searching for it's orign
    for (int i1 = 0; i1 < rootPanel.getDragAndDropPanelMain().panels.length; i1++) {
      for (int i2 = 0; i2 < rootPanel.getDragAndDropPanelMain().panels[i1].length; i2++) {
        ServoOrchestratorGUI_middlemiddle_panel p = rootPanel.getDragAndDropPanelMain().panels[i1][i2];
        if (p != null && dpid == p.id) {
          f1 = i1;
          f2 = i2;
        }
      }
    }

    if (posx == -1 && posy == -1) {
      // Deleting position - delete
      rootPanel.getDragAndDropPanelMain().panels[f1][f2] = null;
    } else {
      // Move the panel
      ServoOrchestratorGUI_middlemiddle_panel pold = rootPanel.getDragAndDropPanelMain().panels[posx][posy];

      rootPanel.getDragAndDropPanelMain().panels[posx][posy] = droppedPanel;

      rootPanel.getDragAndDropPanelMain().panels[f1][f2] = pold;

      boolean later_externalcall_servopanelsettostartpos = false;
      boolean later_externalcall_servopanelsettostartpos2 = false;
      int otherpanelx = 0;

      // TODO - change other attributes, too (all done, or?)
      // TODO - make the channelid independent of the y-position (posy)
      droppedPanel.servo_channelid.setText("CH" + (posy + 1));
      // min is changed with the externalcall below
      // max is changed with the externalcall below

      int start = -1;
      int searchpos = posx - 1;
      while (searchpos >= 0) {
        if (rootPanel.getDragAndDropPanelMain().panels[searchpos][posy] == null) {
          searchpos--;
        } else {
          start = Integer.parseInt(rootPanel.getDragAndDropPanelMain().panels[searchpos][posy].servo_goal.getText());
          break;
        }
      }
      if (start == -1) {
        later_externalcall_servopanelsettostartpos = true;
        // it's the first panel in this row,
        // start changed with the externalcall below
      }
      droppedPanel.servo_start.setText(start + "");
      searchpos = posx + 1;
      while (searchpos < rootPanel.getDragAndDropPanelMain().panels.length) {
        if (rootPanel.getDragAndDropPanelMain().panels[searchpos][posy] == null) {
          searchpos++;
        } else {
          int goal = Integer.parseInt(rootPanel.getDragAndDropPanelMain().panels[searchpos][posy].servo_start.getText());
          droppedPanel.servo_goal.setText(goal + "");
          break;
        }
      }

      // change attributes where the panel left
      int start2 = -1;
      searchpos = posx - 1;
      while (searchpos >= 0) {
        if (rootPanel.getDragAndDropPanelMain().panels[searchpos][f2] == null) {
          searchpos--;
        } else {
          start2 = Integer.parseInt(rootPanel.getDragAndDropPanelMain().panels[searchpos][f2].servo_goal.getText());
          break;
        }
      }

      searchpos = posx + 1;
      while (searchpos < rootPanel.getDragAndDropPanelMain().panels.length) {
        if (rootPanel.getDragAndDropPanelMain().panels[searchpos][f2] == null) {
          searchpos++;
        } else {
          rootPanel.getDragAndDropPanelMain().panels[searchpos][f2].servo_start.setText(start2 + "");
          if (start2 == -1) {
            later_externalcall_servopanelsettostartpos2 = true;
            // it's the first panel in this row,
            // start changed with the externalcall below
            otherpanelx = searchpos;
          }
          break;
        }
      }

      rootPanel.getDragAndDropPanelMain().so_ref.externalcall_servopanelchangeinfo(posx, posy);
      if (later_externalcall_servopanelsettostartpos) {
        rootPanel.getDragAndDropPanelMain().so_ref.externalcall_servopanelsettostartpos(posx, posy, false);
      }
      if (later_externalcall_servopanelsettostartpos2) {
        rootPanel.getDragAndDropPanelMain().so_ref.externalcall_servopanelsettostartpos(otherpanelx, f2, false);
      }
    }
    this.rootPanel.getDragAndDropPanelMain().relayout();
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
  }
}
