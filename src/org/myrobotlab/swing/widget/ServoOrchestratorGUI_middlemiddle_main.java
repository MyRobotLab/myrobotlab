package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.myrobotlab.swing.ServoOrchestratorGui;

/**
 * source modified from:
 * http://bryanesmith.com/docs/drag-and-drop-java-5/DragAndDropPanelsDemo.java
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestratorGUI_middlemiddle_main {

  // References to the panels
  public ServoOrchestratorGUI_middlemiddle_panel[][] panels;

  // This is the panel that will hold everything.
  private final ServoOrchestratorGUI_middlemiddle_rootpanel rootPanel;

  // "border"-panels
  public ServoOrchestratorGUI_middlemiddle_panel[] prep;

  // "main"-panel
  private final JPanel middlemiddle;

  public final ServoOrchestratorGui so_ref;

  public int panel_counter;

  /**
   * <p>
   * This represents the data that is transmitted in drag and drop.
   * </p>
   * <p>
   * In our limited case with only 1 type of dropped item, it will be a panel
   * object!
   * </p>
   * <p>
   * Note DataFlavor can represent more than classes -- easily text, images,
   * etc.
   * </p>
   */
  private static DataFlavor dragAndDropPanelDataFlavor = null;

  /**
   * <p>
   * Returns (creating, if necessary) the DataFlavor representing
   * ServoOrchestratorGUI_middlemiddle_panel
   * </p>
   * @return data flavor
   * @throws Exception e
   *
   */
  public static DataFlavor getDragAndDropPanelDataFlavor() throws Exception {
    // the commented (first one) is original and first it wotrked, then not
    // (???)
    // the second (uncommented) is my repair - don't know (???)
    // FIXME - maybe (???)
    // TODO - maybe a fix (???)

    // Lazy load/create the flavor
    if (dragAndDropPanelDataFlavor == null) {
      // dragAndDropPanelDataFlavor = new
      // DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
      // ";class=RandomDragAndDropPanel");
      dragAndDropPanelDataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
    }

    return dragAndDropPanelDataFlavor;
  }

  public ServoOrchestratorGUI_middlemiddle_main(final ServoOrchestratorGui so_reft) {

    so_ref = so_reft;

    middlemiddle = new JPanel();

    // Create the root panel and add to the main panel
    rootPanel = new ServoOrchestratorGUI_middlemiddle_rootpanel(ServoOrchestratorGUI_middlemiddle_main.this);
    rootPanel.setLayout(new GridBagLayout());
    middlemiddle.add(rootPanel);

    panel_counter = 0;

    // Create a list to hold all the panels
    panels = new ServoOrchestratorGUI_middlemiddle_panel[so_ref.sizex][so_ref.sizey];

    // "border"-panels
    prep = new ServoOrchestratorGUI_middlemiddle_panel[panels.length + panels[0].length];
    for (int i = 0; i < prep.length; i++) {
      if (i < panels[0].length) {
        final int fi = i;
        panel_counter++;
        prep[i] = new ServoOrchestratorGUI_middlemiddle_panel("channel", panel_counter);
        prep[i].channel_id.setText(prep[i].id + "");
        prep[i].channel_name.setText("Channel " + (i + 1));
        prep[i].channel_settings.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent ae) {
            so_ref.externalcall_loadsettings(fi);
          }
        });
      } else {
        panel_counter++;
        prep[i] = new ServoOrchestratorGUI_middlemiddle_panel("timesection", panel_counter);
        prep[i].timesection_id.setText(prep[i].id + "");
        prep[i].timesection_headline.setText("TIMEUNIT " + (i - panels[0].length + 1));
      }
    }
    prep[panels[0].length].setBackground(Color.red);

    // refresh the gui
    relayout();
  }

  // Button
  public void externalcall_addPanel() {
    // Add the new panel to the array (on the first free space)
    // after that - relayout!
    boolean found = false;
    for (int i1 = 0; i1 < panels.length; i1++) {
      if (found) {
        break;
      }
      for (int i2 = 0; i2 < panels[i1].length; i2++) {
        if (found) {
          break;
        }
        if (panels[i1][i2] == null) {
          panel_counter++;
          ServoOrchestratorGUI_middlemiddle_panel p = new ServoOrchestratorGUI_middlemiddle_panel("servo", panel_counter);
          panels[i1][i2] = p;

          boolean later_externalcall_servopanelsettostartpos = false;
          boolean withgoal = false;

          p.servo_id.setText(p.id + "");
          // TODO - make the channelid independent of the y-position
          // (i2)
          p.servo_channelid.setText("CH" + (i2 + 1));
          int start = -1;
          int searchpos = i1 - 1;
          while (searchpos >= 0) {
            if (panels[searchpos][i2] == null) {
              searchpos--;
            } else {
              start = Integer.parseInt(panels[searchpos][i2].servo_goal.getText());
              break;
            }
          }
          if (start == -1) {
            later_externalcall_servopanelsettostartpos = true;
            // it's the first panel in this row,
            // start changed with the externalcall below
          }
          p.servo_start.setText(start + "");
          p.servo_goal.setText(start + "");
          // this is probably useless
          int goal = -1;
          searchpos = i1 + 1;
          while (searchpos < panels.length) {
            if (panels[searchpos][i2] == null) {
              searchpos++;
            } else {
              goal = Integer.parseInt(panels[searchpos][i2].servo_start.getText());
              break;
            }
          }
          p.servo_goal.setText(goal + "");
          if (goal == -1) {
            p.servo_goal.setText(start + "");
            if (start == -1) {
              withgoal = true;
            }
          }
          // min is changed with the externalcall below
          // max is changed with the externalcall below
          // TODO - add remaining attributes
          // (only button left) (others only fixes) (I think)

          found = true;

          final int fi1 = i1;
          final int fi2 = i2;
          final ServoOrchestratorGUI_middlemiddle_panel fp = p;
          p.servo_goal.getDocument().addDocumentListener(new DocumentListener() {
            public void adjust() {
              int i1 = fi1;
              int i2 = fi2;
              ServoOrchestratorGUI_middlemiddle_panel p = fp;
              int searchpos = i1 + 1;
              while (searchpos < panels.length) {
                if (panels[searchpos][i2] == null) {
                  searchpos++;
                } else {
                  panels[searchpos][i2].servo_start.setText(p.servo_goal.getText() + "");
                  break;
                }
              }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
              adjust();

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
              adjust();

            }

            @Override
            public void removeUpdate(DocumentEvent e) {
              adjust();

            }
          });

          so_ref.externalcall_servopanelchangeinfo(i1, i2);
          if (later_externalcall_servopanelsettostartpos) {
            so_ref.externalcall_servopanelsettostartpos(i1, i2, withgoal);
          }
        }
      }
    }

    // Relayout the panels.
    relayout();
  }

  public JPanel externalcall_getmiddlemiddle() {
    return middlemiddle;
  }

  public void externallcall_refreshsize() {

    // Copy all panels, because the arrays need to be re-created
    // the old stuff should be in it
    ServoOrchestratorGUI_middlemiddle_panel[][] panelsold = new ServoOrchestratorGUI_middlemiddle_panel[panels.length][panels[0].length];
    for (int i = 0; i < panels.length; i++) {
      panelsold[i] = panels[i].clone();
    }

    ServoOrchestratorGUI_middlemiddle_panel[] prepold = prep.clone();

    // Create a list to hold all the panels
    panels = new ServoOrchestratorGUI_middlemiddle_panel[so_ref.sizex][so_ref.sizey];
    for (int i1 = 0; i1 < panels.length; i1++) {
      if (i1 >= panelsold.length) {
        continue;
      }
      for (int i2 = 0; i2 < panels[0].length; i2++) {
        if (i2 >= panelsold[0].length) {
          continue;
        }
        panels[i1][i2] = panelsold[i1][i2];
      }
    }

    // "border"-panels
    prep = new ServoOrchestratorGUI_middlemiddle_panel[panels.length + panels[0].length];
    for (int i = 0; i < prep.length; i++) {
      if (i < panels[0].length) {
        final int fi = i;
        panel_counter++;
        prep[i] = new ServoOrchestratorGUI_middlemiddle_panel("channel", panel_counter);
        prep[i].channel_id.setText(prep[i].id + "");
        prep[i].channel_name.setText("Channel " + (i + 1));
        prep[i].channel_settings.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent ae) {
            so_ref.externalcall_loadsettings(fi);
          }
        });
        prep[i].setBackground(Color.green);
      } else {
        panel_counter++;
        prep[i] = new ServoOrchestratorGUI_middlemiddle_panel("timesection", panel_counter);
        prep[i].timesection_id.setText(prep[i].id + "");
        prep[i].timesection_headline.setText("TIMEUNIT " + (i - panels[0].length + 1));
        prep[i].setBackground(Color.green);
      }
    }
    prep[panels[0].length].setBackground(Color.red);

    for (int i = 0; i < panelsold[0].length && i < panels[0].length; i++) {
      prep[i].channel_name.setText((prepold[i].channel_name.getText()));
    }

    // refresh the gui
    relayout();
  }

  /**
   * <p>
   * Removes all components from our root panel and re-adds them.
   * </p>
   * <p>
   * This is important for two things:
   * </p>
   * <ul>
   * <li>Adding a new panel (user clicks on button)</li>
   * <li>Re-ordering panels (user drags and drops a panel to acceptable drop
   * target region)</li>
   * </ul>
   */
  public void relayout() {

    // Create the constraints, and go ahead and set those
    // that don't change for components
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.weighty = 1.0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(0, 0, 0, 0);

    // Clear out all previously added items
    rootPanel.removeAll();

    // Add the panels, if any & the "border"-panels
    for (int i1 = 0; i1 < panels.length + 1; i1++) {
      for (int i2 = 0; i2 < panels[0].length + 1; i2++) {
        if (i1 == 0 || i2 == 0) {
          if (i1 != 0 || i2 != 0) {
            gbc.gridx = i1;
            gbc.gridy = i2;
            int num = 0;
            if (i1 == 0) {
              num = i2 - 1;
            } else {
              num = panels[0].length - 1 + i1;
            }
            rootPanel.add(prep[num], gbc);
          }
        } else {
          ServoOrchestratorGUI_middlemiddle_panel p = panels[i1 - 1][i2 - 1];
          gbc.gridx = i1;
          gbc.gridy = i2;
          if (p != null) {
            rootPanel.add(p, gbc);
          }
        }
      }
    }

    middlemiddle.validate();
    middlemiddle.repaint();
  }
}
