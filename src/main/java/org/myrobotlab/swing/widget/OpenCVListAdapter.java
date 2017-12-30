package org.myrobotlab.swing.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.swing.OpenCVGui;
import org.slf4j.Logger;

public class OpenCVListAdapter extends MouseAdapter implements ActionListener {

  public final static Logger log = LoggerFactory.getLogger(OpenCVListAdapter.class);

  JPopupMenu popup = new JPopupMenu();
  JMenuItem infoMenuItem = new JMenuItem("info");
  JMenuItem addMenuItem = new JMenuItem("add");
  OpenCVGui opencvgui;
  // JList myList;

  JList mylist;

  String mySelectedItem;

  public OpenCVListAdapter(OpenCVGui opencvgui) {
    super();
    this.opencvgui = opencvgui;
    infoMenuItem.addActionListener(this);
    addMenuItem.addActionListener(this);
    infoMenuItem.setIcon(Util.getScaledIcon(Util.getImage("info.png"), 0.50));
    addMenuItem.setIcon(Util.getScaledIcon(Util.getImage("add.png"), 0.50));
    popup.add(infoMenuItem);
    popup.add(addMenuItem);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    // ServiceEntry c = (ServiceEntry)
    // possibleServicesModel.getValueAt(popupRow, 0);
    if (o == infoMenuItem) {
      // String filterType = (String)mylist.getSelectedValue();
      BareBonesBrowserLaunch.openURL(String.format("http://myrobotlab.org/service/OpenCV#%s", mySelectedItem));
    } else if (o == addMenuItem) {
      opencvgui.addFilter();
    }

  }

  // isPopupTrigger over OSs - use masks
  @Override
  public void mouseReleased(MouseEvent e) {
    log.debug("mouseReleased");

    if (SwingUtilities.isRightMouseButton(e)) {
      log.debug("mouseReleased - right");
      popUpTrigger(e);
    }
  }

  public void popUpTrigger(MouseEvent e) {
    log.info("******************popUpTrigger*********************");
    JList list = (JList) e.getSource();
    mylist = list;
    mySelectedItem = (String) list.getSelectedValue();
    infoMenuItem.setText(String.format("%s info", mySelectedItem));
    int index = list.locationToIndex(e.getPoint());
    if (index >= 0) {
      // releasedTarget = (ServiceEntry)
      // source.getModel().getElementAt(index);
      // log.info(String.format("right click on running service %s",
      // releasedTarget.name));
      infoMenuItem.setVisible(true);
    }
    popup.show(e.getComponent(), e.getX(), e.getY());

  }

}
