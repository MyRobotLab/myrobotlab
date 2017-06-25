package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.myrobotlab.logging.Logging;

/**
 * source modified from:
 * http://bryanesmith.com/docs/drag-and-drop-java-5/DragAndDropPanelsDemo.java
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestratorGUI_middlemiddle_panel extends JPanel implements Transferable {

  private static final long serialVersionUID = 1L;

  public String type;

  public int id = 0;

  JPanel timesection_panel;
  public JLabel timesection_headline;
  JLabel timesection_id;

  JPanel channel_panel;
  public JLabel channel_name;
  JButton channel_mute;
  JLabel channel_id;
  JButton channel_solo;
  public JButton channel_settings;

  JPanel servo_panel;
  public JTextField servo_start;
  public JLabel servo_channelid;
  public JTextField servo_goal;
  public JLabel servo_min;
  JLabel servo_id;
  public JLabel servo_max;
  JButton servo_more;

  // JPanel stepper_panel;
  public ServoOrchestratorGUI_middlemiddle_panel(String mode, int num) {
    type = mode;

    // Add the listener which will export this panel for dragging
    this.addMouseListener(new ServoOrchestratorGUI_middlemiddle_draggablemouselistener());

    // Add the handler, which negotiates between drop target and this
    // draggable panel
    this.setTransferHandler(new ServoOrchestratorGUI_middlemiddle_transferhandler());

    // Create the ID of this panel
    id = num;

    // Style it a bit to set apart from container
    this.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

    // "Timesection" - Panel
    timesection_panel = new JPanel();
    timesection_panel.setLayout(new GridBagLayout());

    timesection_headline = new JLabel("HEADLINE");
    timesection_id = new JLabel("ID##");

    // x y w h
    timesection_panel.add(timesection_headline, gridbaglayout_set(0, 0, 3, 1));
    timesection_panel.add(timesection_id, gridbaglayout_set(1, 2, 1, 1));

    this.add(timesection_panel);

    // "Channel" - Panel
    channel_panel = new JPanel();
    channel_panel.setLayout(new GridBagLayout());

    channel_name = new JLabel("NAME");
    channel_mute = new JButton("M");
    channel_id = new JLabel("ID##");
    channel_solo = new JButton("S");
    channel_settings = new JButton("SETTINGS");
    // TODO - add ActionListener for "mute"
    // TODO - add ActionListener for "solo"

    // x y w h
    channel_panel.add(channel_name, gridbaglayout_set(0, 0, 3, 1));
    channel_panel.add(channel_mute, gridbaglayout_set(0, 1, 1, 1));
    channel_panel.add(channel_id, gridbaglayout_set(1, 1, 1, 1));
    channel_panel.add(channel_solo, gridbaglayout_set(2, 1, 1, 1));
    channel_panel.add(channel_settings, gridbaglayout_set(0, 2, 3, 1));

    this.add(channel_panel);

    // "Servo" - Panel
    servo_panel = new JPanel();
    servo_panel.setLayout(new GridBagLayout());

    servo_start = new JTextField("STAR");
    servo_start.setColumns(3);
    servo_start.setEditable(false);
    servo_channelid = new JLabel("CHID");
    servo_goal = new JTextField("GOAL");
    servo_goal.setColumns(3);
    servo_min = new JLabel("MIN#");
    servo_id = new JLabel("ID##");
    servo_max = new JLabel("MAX#");
    servo_more = new JButton("MORE");
    // TODO - ActionListener for "more"

    // x y w h
    servo_panel.add(servo_start, gridbaglayout_set(0, 0, 1, 1));
    servo_panel.add(servo_channelid, gridbaglayout_set(1, 0, 1, 1));
    servo_panel.add(servo_goal, gridbaglayout_set(2, 0, 1, 1));
    servo_panel.add(servo_min, gridbaglayout_set(0, 1, 1, 1));
    servo_panel.add(servo_id, gridbaglayout_set(1, 1, 1, 1));
    servo_panel.add(servo_max, gridbaglayout_set(2, 1, 1, 1));
    servo_panel.add(servo_more, gridbaglayout_set(0, 2, 3, 1));

    this.add(servo_panel);

    switch (type) {
      case "timesection":
        timesection_panel.setVisible(true);
        channel_panel.setVisible(false);
        servo_panel.setVisible(false);
        this.setBackground(Color.GREEN);
        break;
      case "channel":
        timesection_panel.setVisible(false);
        channel_panel.setVisible(true);
        servo_panel.setVisible(false);
        this.setBackground(Color.GREEN);
        break;
      case "servo":
        timesection_panel.setVisible(false);
        channel_panel.setVisible(false);
        servo_panel.setVisible(true);
        this.setBackground(Color.YELLOW);
        break;
    }

    // This won't take the entire width for easy drag and drop
    final Dimension d = new Dimension(130, 80);
    this.setPreferredSize(d);
    this.setMinimumSize(d);
  }

  public ServoOrchestratorGUI_middlemiddle_panel(String[] data) {
    this(data[0], Integer.parseInt(data[1]));

    if (type.equals("channel")) {
      channel_id.setText(id + "");
      channel_name.setText(data[2]);
    } else if (type.equals("timesection")) {
      timesection_id.setText(id + "");
    } else if (type.equals("servo")) {
      servo_id.setText(id + "");
      servo_start.setText(data[3]);
      servo_channelid.setText(data[4]);
      servo_goal.setText(data[5]);
      servo_min.setText(data[6]);
      servo_max.setText(data[7]);
    }
  }

  /**
   * <p>
   * One of three methods defined by the Transferable interface.
   * </p>
   * <p>
   * If multiple DataFlavor's are supported, can choose what Object to return.
   * </p>
   * <p>
   * In this case, we only support one: the actual JPanel.
   * </p>
   * <p>
   * Note we could easily support more than one. For example, if supports text
   * and drops to a JTextField, could return the label's text or any arbitrary
   * text.
   * </p>
   */
  @Override
  public Object getTransferData(DataFlavor flavor) {
    DataFlavor thisFlavor = null;

    try {
      thisFlavor = ServoOrchestratorGUI_middlemiddle_main.getDragAndDropPanelDataFlavor();
    } catch (Exception ex) {
      Logging.logError(ex);
      Logging.stackToString(ex);
      return null;
    }

    // For now, assume wants this class... see loadDnD
    if (thisFlavor != null && flavor.equals(thisFlavor)) {
      return ServoOrchestratorGUI_middlemiddle_panel.this;
    }

    return null;
  }

  /**
   * <p>
   * One of three methods defined by the Transferable interface.
   * </p>
   * <p>
   * Returns supported DataFlavor. Again, we're only supporting this actual
   * Object within the JVM.
   * </p>
   * <p>
   * For more information, see the JavaDoc for DataFlavor.
   * </p>
   *
   */
  @Override
  public DataFlavor[] getTransferDataFlavors() {

    DataFlavor[] flavors = { null };

    try {
      flavors[0] = ServoOrchestratorGUI_middlemiddle_main.getDragAndDropPanelDataFlavor();
    } catch (Exception ex) {
      Logging.logError(ex);
      Logging.stackToString(ex);
      return null;
    }

    return flavors;
  }

  public GridBagConstraints gridbaglayout_set(int x, int y, int w, int h) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.weighty = 1.0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(0, 0, 0, 0);

    gbc.gridx = x;
    gbc.gridy = y;

    gbc.gridwidth = w;
    gbc.gridheight = h;

    return gbc;
  }

  /**
   * <p>
   * One of three methods defined by the Transferable interface.
   * </p>
   * <p>
   * Determines whether this object supports the DataFlavor. In this case, only
   * one is supported: for this object itself.
   * </p>
   *
   * @return True if DataFlavor is supported, otherwise false.
   */
  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    DataFlavor[] flavors = { null };
    try {
      flavors[0] = ServoOrchestratorGUI_middlemiddle_main.getDragAndDropPanelDataFlavor();
    } catch (Exception ex) {
      Logging.logError(ex);
      Logging.stackToString(ex);
      return false;
    }

    for (DataFlavor f : flavors) {
      if (f.equals(flavor)) {
        return true;
      }
    }

    return false;
  }
}
