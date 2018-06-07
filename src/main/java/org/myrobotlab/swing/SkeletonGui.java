package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Skeleton;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

/**
 * TODO : show attached servoGui here to group them ( tabs ) if attached
 * just list them for now...
 */
public class SkeletonGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SkeletonGui.class);

  JList servoList = new JList();
  DefaultListModel listModel = new DefaultListModel();

  public SkeletonGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    servoList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    servoList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    servoList.setVisibleRowCount(-1);

    add(servoList);

  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

  }

  @Override
  public void subscribeGui() {

  }

  @Override
  public void unsubscribeGui() {

  }

  public void onState(Skeleton sk) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        listModel.clear();
        for (int i = 0; i < sk.getServos().size(); i++) {
          listModel.addElement(sk.getServos().get(i));
        }
        servoList.setModel(listModel);

      }
    });
  }

}
