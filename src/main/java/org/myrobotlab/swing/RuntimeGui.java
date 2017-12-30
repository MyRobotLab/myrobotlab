/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.SystemResources;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.Category;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.ImageNameRenderer;
import org.myrobotlab.swing.widget.PossibleServicesRenderer;
import org.myrobotlab.swing.widget.ProgressDialog;
import org.slf4j.Logger;

public class RuntimeGui extends ServiceGui implements ActionListener, ListSelectionListener, KeyListener {

  class FilterListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent cmd) {
      log.info(cmd.getActionCommand());
      if ("all".equals(cmd.getActionCommand())) {
        getPossibleServices();
      } else {
        getPossibleServicesFromCategory(cmd.getActionCommand());
      }
    }
  }

  public final static Logger log = LoggerFactory.getLogger(RuntimeGui.class);
  static final long serialVersionUID = 1L;
  HashMap<String, ServiceInterface> nameToServiceEntry = new HashMap<String, ServiceInterface>();

  ArrayList<String> resolveErrors = null;
  boolean localRepoChange = false;
  int popupRow = 0;
  JMenuItem infoMenuItem = null;
  JMenuItem installMenuItem = null;

  JMenuItem startMenuItem = null;
  JMenuItem upgradeMenuItem = null;

  JMenuItem releaseMenuItem = null;
  String possibleServiceFilter = null;
  ProgressDialog progressDialog = null;

  JLabel freeMemory = new JLabel();
  JLabel usedMemory = new JLabel();
  JLabel maxMemory = new JLabel();
  JLabel totalMemory = new JLabel();
  JLabel totalPhysicalMemory = new JLabel();

  JTextField search = new JTextField();

  Runtime myRuntime = null;
  Repo myRepo = null;

  ServiceData serviceData = null;

  DefaultListModel<Category> categoriesModel = new DefaultListModel<Category>();
  // below should be String NOT ServiceInterface !!!
  DefaultListModel<ServiceInterface> currentServicesModel = new DefaultListModel<ServiceInterface>();

  JList<Category> categories = new JList<Category>(categoriesModel);
  JList<ServiceInterface> runningServices = new JList<ServiceInterface>(currentServicesModel);

  ImageNameRenderer imageNameRenderer = new ImageNameRenderer();

  FilterListener filterListener = new FilterListener();

  JPopupMenu popup = new JPopupMenu();

  ServiceInterface releasedTarget = null;

  DefaultTableModel possibleServicesModel = new DefaultTableModel() {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  };

  PossibleServicesRenderer cellRenderer = new PossibleServicesRenderer((Runtime) Runtime.getService(boundServiceName));

  JTable possibleServices = new JTable(possibleServicesModel) {
    private static final long serialVersionUID = 1L;

    @Override
    public JToolTip createToolTip() {
      JToolTip tooltip = super.createToolTip();
      return tooltip;
    }

    // column returns content type
    @Override
    public Class<?> getColumnClass(int column) {
      Object o = getValueAt(0, column);
      if (o == null) {
        return new Object().getClass();
      }
      return o.getClass();
    }
  };

  public RuntimeGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    // required - it "might" be a foreign Runtime...
    myRuntime = (Runtime) Runtime.getService(boundServiceName);
    myRepo = myRuntime.getRepo();
    serviceData = myRuntime.getServiceData();

    progressDialog = new ProgressDialog(this);
    progressDialog.setVisible(false);

    getCurrentServices();

    ArrayList<Category> c = serviceData.getCategories();
    for (int i = 0; i < c.size(); ++i) {
      categoriesModel.addElement(c.get(i));
    }
    categories.setCellRenderer(imageNameRenderer);
    categories.setFixedCellWidth(100);
    categories.addListSelectionListener(this);

    possibleServicesModel.addColumn("service");
    possibleServicesModel.addColumn("status");

    // possibleServices.setRowHeight(24);
    possibleServices.setIntercellSpacing(new Dimension(0, 0));
    // possibleServices.setSize(new Dimension(300, 400));
    possibleServices.setShowGrid(false);
    possibleServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // possibleServices.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    // possibleServices.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    // possibleServices.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    // possibleServices.setPreferredSize(new Dimension(200, 200));
    JScrollPane possible = new JScrollPane(possibleServices);
    possible.setPreferredSize(new Dimension(360, 400));

    TableColumnModel cm = possibleServices.getColumnModel();
    cm.getColumn(0).setMaxWidth(300);
    cm.getColumn(1).setMaxWidth(60);

    possibleServices.setDefaultRenderer(ImageIcon.class, cellRenderer);
    possibleServices.setDefaultRenderer(ServiceType.class, cellRenderer);
    possibleServices.setDefaultRenderer(String.class, cellRenderer);

    possibleServices.addMouseListener(new MouseAdapter() {
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
        JTable source = (JTable) e.getSource();
        popupRow = source.rowAtPoint(e.getPoint());
        ServiceType c = (ServiceType) possibleServicesModel.getValueAt(popupRow, 0);
        releaseMenuItem.setVisible(false);
        infoMenuItem.setVisible(true);
        if (!myRepo.isServiceTypeInstalled(c.getName())) {
          // need to install it
          installMenuItem.setVisible(true);
          startMenuItem.setVisible(false);
          upgradeMenuItem.setVisible(false);
        } else {
          // have it
          installMenuItem.setVisible(false);
          startMenuItem.setVisible(true);
        }

        int column = source.columnAtPoint(e.getPoint());

        if (!source.isRowSelected(popupRow))
          source.changeSelection(popupRow, column, false, false);

        popup.show(e.getComponent(), e.getX(), e.getY());

      }

    });

    runningServices.addMouseListener(new MouseAdapter() {
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
        JList source = (JList) e.getSource();
        int index = source.locationToIndex(e.getPoint());
        if (index >= 0) {
          releasedTarget = (ServiceInterface) source.getModel().getElementAt(index);
          log.info(String.format("right click on running service %s", releasedTarget.getName()));
          releaseMenuItem.setVisible(true);
          upgradeMenuItem.setVisible(false);
          installMenuItem.setVisible(false);
          startMenuItem.setVisible(false);
          infoMenuItem.setVisible(false);
        }
        popup.show(e.getComponent(), e.getX(), e.getY());

      }

    });

    infoMenuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
    infoMenuItem.setActionCommand("info");
    infoMenuItem.setIcon(Util.getImageIcon("help.png"));
    infoMenuItem.addActionListener(this);
    popup.add(infoMenuItem);

    installMenuItem = new JMenuItem("install");
    installMenuItem.addActionListener(this);
    installMenuItem.setIcon(Util.getImageIcon("install.png"));
    popup.add(installMenuItem);

    startMenuItem = new JMenuItem("start");
    startMenuItem.addActionListener(this);
    startMenuItem.setIcon(Util.getImageIcon("start.png"));
    popup.add(startMenuItem);

    upgradeMenuItem = new JMenuItem("upgrade");
    upgradeMenuItem.addActionListener(this);
    upgradeMenuItem.setIcon(Util.getImageIcon("upgrade.png"));
    popup.add(upgradeMenuItem);

    releaseMenuItem = new JMenuItem("release");
    releaseMenuItem.addActionListener(this);
    releaseMenuItem.setIcon(Util.getScaledIcon(Util.getImage("release.png"), 0.50));
    popup.add(releaseMenuItem);

    setTitle(myRuntime.getPlatform().toString());

    JPanel flow = new JPanel();
    flow.add(createCategories());
    JPanel border = new JPanel(new BorderLayout());
    search.addKeyListener(this);
    border.add(search, BorderLayout.NORTH);
    border.add(possible, BorderLayout.CENTER);
    flow.add(border);
    flow.add(createRunningServices());
    addLine(flow);

    // add(categories, possible, runningServices);

    addTopLine(createMenuBar());
    addBottom("memory physical ", totalPhysicalMemory, "  max ", maxMemory, "  total ", totalMemory, "  free ", freeMemory, "  used ", usedMemory);
    getPossibleServices();

  }

  public JPanel createRunningServices() {
    GridBagConstraints fgc = new GridBagConstraints();
    JPanel panel = new JPanel(new GridBagLayout());
    fgc.gridy = 0;
    fgc.gridx = 0;
    fgc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new JLabel("running services"), fgc);
    ++fgc.gridy;
    JScrollPane scroll = new JScrollPane(runningServices);
    runningServices.setVisibleRowCount(15);
    runningServices.setCellRenderer(imageNameRenderer);
    runningServices.setFixedCellWidth(100);
    panel.add(scroll, fgc);
    return panel;
  }

  public JPanel createCategories() {
    GridBagConstraints fgc = new GridBagConstraints();
    JPanel panel = new JPanel(new GridBagLayout());
    fgc.gridy = 0;
    fgc.gridx = 0;
    fgc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new JLabel("categories"), fgc);
    ++fgc.gridy;
    JScrollPane scroll = new JScrollPane(categories);
    categories.setVisibleRowCount(15);
    categories.setCellRenderer(imageNameRenderer);
    categories.setFixedCellWidth(160);
    panel.add(scroll, fgc);
    return panel;
  }

  public JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    JMenu system = new JMenu("system");
    menuBar.add(system);
    JMenu logging = new JMenu("logging");
    menuBar.add(logging);

    /*
    JMenuItem item = new JMenuItem("check for updates");
    item.addActionListener(this);
    system.add(item);
    */

    JMenuItem item = new JMenuItem("install all");
    item.addActionListener(this);
    system.add(item);
    
    item = new JMenuItem("record");
    item.addActionListener(this);
    system.add(item);
    
    item = new JMenuItem("restart");
    item.addActionListener(this);
    system.add(item);
    
    item = new JMenuItem("exit");
    item.addActionListener(this);
    system.add(item);


    JMenu m1 = new JMenu("level");
    logging.add(m1);
    buildLogLevelMenu(m1);
    return menuBar;
  }

  // zod
  @Override
  public void actionPerformed(ActionEvent event) {
    ServiceType c = (ServiceType) possibleServicesModel.getValueAt(popupRow, 0);
    String cmd = event.getActionCommand();
    Object o = event.getSource();

    if (releaseMenuItem == o) {
      myService.send(boundServiceName, "releaseService", releasedTarget.getName());
      return;
    }

    if ("info".equals(cmd)) {
      BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + c.getSimpleName());
    } else if ("install".equals(cmd)) {
      int selectedRow = possibleServices.getSelectedRow();

      ServiceType entry = ((ServiceType) possibleServices.getValueAt(selectedRow, 0));
      String n = entry.getName();
      Repo repo = myRuntime.getRepo();

      if (!repo.isServiceTypeInstalled(n)) {
        // dependencies needed !!!
        String msg = "<html>This Service has dependencies which are not yet loaded,<br>" + "do you wish to download them now?";
        JOptionPane.setRootFrame(myService.getFrame());
        int result = JOptionPane.showConfirmDialog(myService.getFrame(), msg, "alert", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.CANCEL_OPTION) {
          return;
        }
        // you say "install", i say "update", repo says "resolve"
        myService.send(boundServiceName, "install", n);
      } else {
        // no unfulfilled dependencies - good to go
        addNewService(n);
      }

    } else if ("start".equals(cmd)) {
      int selectedRow = possibleServices.getSelectedRow();
      ServiceType entry = ((ServiceType) possibleServices.getValueAt(selectedRow, 0));
      addNewService(entry.getName());

    } else if ("install all".equals(cmd)) {
      send("install");
    } else if ("restart".equals(cmd)) {
      Runtime.getInstance().restart();      
    } else if ("exit".equals(cmd)) {
      Runtime.shutdown();
    } else if ("check for updates".equals(cmd)) {
      send("checkForUpdates");
    } else if (cmd.equals(Level.DEBUG) || cmd.equals(Level.INFO) || cmd.equals(Level.WARN) || cmd.equals(Level.ERROR) || cmd.equals(Level.FATAL)) {
      send("setLogLevel", cmd);/*
      Logging logging = LoggingFactory.getInstance();
      logging.setLevel(cmd);*/
    } /*else if (cmd.equals(Appender.FILE)) {
      Logging logging = LoggingFactory.getInstance();
      logging.addAppender(Appender.FILE);
    } else if (cmd.equals(Appender.CONSOLE)) {
      Logging logging = LoggingFactory.getInstance();
      logging.addAppender(Appender.CONSOLE);
    } else if (cmd.equals(Appender.NONE)) {
      Logging logging = LoggingFactory.getInstance();
      logging.removeAllAppenders();

    }*/ else {
      log.error("unknown command " + cmd);
    }

    // end actionCmd

  }

  public void addNewService(String newService) {
    JFrame frame = new JFrame();
    frame.setTitle("add new service");
    String name = JOptionPane.showInputDialog(frame, "new service name");

    if (name != null && name.length() > 0) {
      myService.send(boundServiceName, "createAndStart", name, newService);

    }
  }

  /*
   * scheduled event of reporting on system resources
   */
  public void onSystemResources(SystemResources resources) {
    totalPhysicalMemory.setText(String.format("%d", resources.getTotalPhysicalMemory()));
    maxMemory.setText(String.format("%d", resources.getMaxMemory()));
    totalMemory.setText(String.format("%d", resources.getTotalMemory()));
    freeMemory.setText(String.format("%d", resources.getFreeMemory()));
    usedMemory.setText(String.format("%d", resources.getTotalMemory() - resources.getFreeMemory()));
  }

  @Override
  public void subscribeGui() {
    subscribe("registered");
    subscribe("released");
    subscribe("getSystemResources");
    subscribe("publishInstallProgress");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("registered");
    unsubscribe("released");
    subscribe("getSystemResources");
    unsubscribe("publishInstallProgress");
  }

  /**
   * Add all options to the Log Level menu.
   * 
   * @param parentMenu
   */
  private void buildLogLevelMenu(JMenu parentMenu) {
    ButtonGroup logLevelGroup = new ButtonGroup();

    String level = LoggingFactory.getInstance().getLevel();

    JRadioButtonMenuItem mi = new JRadioButtonMenuItem(Level.DEBUG);
    mi.setSelected(("DEBUG".equals(level)));
    mi.addActionListener(this);
    logLevelGroup.add(mi);
    parentMenu.add(mi);

    mi = new JRadioButtonMenuItem(Level.INFO);
    mi.setSelected(("INFO".equals(level)));
    mi.addActionListener(this);
    logLevelGroup.add(mi);
    parentMenu.add(mi);

    mi = new JRadioButtonMenuItem(Level.WARN);
    mi.setSelected(("WARN".equals(level)));
    mi.addActionListener(this);
    logLevelGroup.add(mi);
    parentMenu.add(mi);

    mi = new JRadioButtonMenuItem(Level.ERROR);
    mi.setSelected(("ERROR".equals(level)));
    mi.addActionListener(this);
    logLevelGroup.add(mi);
    parentMenu.add(mi);

    mi = new JRadioButtonMenuItem(Level.FATAL); // TODO - deprecate to WTF
    // :)
    mi.setSelected(("FATAL".equals(level)));
    mi.addActionListener(this);
    logLevelGroup.add(mi);
    parentMenu.add(mi);
  }

  public void checkingForUpdates() {
    // FIXME - if auto update or check on startup - we don't want a silly
    // dialog
    // we just want to be notified if there "is" an update - and whether or
    // not we
    // should apply it

    // FIXME - bypass is auto
    progressDialog.checkingForUpdates();
  }

  public void failedDependency(String dep) {
    JOptionPane.showMessageDialog(null, "<html>Unable to load Service...<br>" + dep + "</html>", "Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Add data to the list model for display
   */
  public void getCurrentServices() {
    // can't be static
    Map<String, ServiceInterface> services = Runtime.getRegistry();
    Map<String, ServiceInterface> sortedMap = null;
    sortedMap = new TreeMap<String, ServiceInterface>(services);
    Iterator<String> it = sortedMap.keySet().iterator();

    while (it.hasNext()) {
      String serviceName = it.next();
      ServiceInterface si = Runtime.getService(serviceName);
      currentServicesModel.addElement(si);
      nameToServiceEntry.put(serviceName, si);
    }
  }

  public void getPossibleServices() {
    getPossibleServicesFromCategory(null);
  }

  public void getPossibleServicesFromName(final String filter) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        String filtered = (filter == null) ? "" : filter.trim();
        // clear data
        for (int i = possibleServicesModel.getRowCount(); i > 0; --i) {
          possibleServicesModel.removeRow(i - 1);
        }
        // populate with serviceData
        ArrayList<ServiceType> possibleService = serviceData.getServiceTypes();
        for (int i = 0; i < possibleService.size(); ++i) {
          ServiceType serviceType = possibleService.get(i);
          if (filtered == "" || serviceType.getSimpleName().toLowerCase().indexOf(filtered.toLowerCase()) != -1) {
            if (serviceType.isAvailable()) {
              possibleServicesModel.addRow(new Object[] { serviceType, "" });
            }
          }
        }
        possibleServicesModel.fireTableDataChanged();
        possibleServices.invalidate();
      }
    });
  }

  /*
   * lame - deprecate - refactor - or better yet make webgui FIXME this should
   * rarely change .... remove getServiceTypeNames
   */
  public void getPossibleServicesFromCategory(final String filter) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // clear data
        for (int i = possibleServicesModel.getRowCount(); i > 0; --i) {
          possibleServicesModel.removeRow(i - 1);
        }

        Category category = serviceData.getCategory(filter);
        HashSet<String> filtered = null;
        if (category != null) {
          filtered = new HashSet<String>();
          ArrayList<String> f = category.serviceTypes;
          for (int i = 0; i < f.size(); ++i) {
            filtered.add(f.get(i));
          }
        }

        // populate with serviceData
        ArrayList<ServiceType> possibleService = serviceData.getServiceTypes();
        for (int i = 0; i < possibleService.size(); ++i) {
          ServiceType serviceType = possibleService.get(i);
          if (filtered == null || filtered.contains(serviceType.getName())) {
            if (serviceType.isAvailable()) {
              possibleServicesModel.addRow(new Object[] { serviceType, "" });
            }
          }
        }

        possibleServicesModel.fireTableDataChanged();
        possibleServices.invalidate();
      }
    });
  }

  public void onState(final Runtime runtime) {
    myRuntime = runtime;

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Platform platform = myRuntime.getPlatform();
        SystemResources resources = myRuntime.getSystemResources();
        totalMemory.setText(String.format("%d", resources.getTotalMemory()));
        freeMemory.setText(String.format("%d", resources.getFreeMemory()));
        totalPhysicalMemory.setText(String.format("%d", resources.getTotalPhysicalMemory()));

        // FIXME - change to "all" or "" - null is sloppy - system has
        // to upcast
        myService.pack();
      }
    });
  }

  /*
   * new Service has been created list it..
   */
  public ServiceInterface onRegistered(Service sw) {
    currentServicesModel.addElement(sw);
    return sw;
  }

  /*
   * a Service of this Runtime has been released
   */
  public ServiceInterface onReleased(Service sw) {
    currentServicesModel.removeElement(sw);
    return sw;
  }

  /**
   * a restart command will be sent to the appropriate runtime
   */
  public void restart() {
    // send("restart"); TODO - change back to restart - when it works
    send("shutdown");
  }

  public void onInstallProgress(Status status) {

    if (Repo.INSTALL_START.equals(status.key)) {
      progressDialog.beginUpdates();
    } else if (Repo.INSTALL_FINISHED.equals(status.key)) {
      progressDialog.finished();
    }

    progressDialog.addStatus(status);
  }

  /**
   * this is the beginning of the applyUpdates process
   */
  public void updatesBegin() {
    progressDialog.beginUpdates();
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    Object o = e.getSource();
    if (o == categories && !e.getValueIsAdjusting()) {
      Category category = categories.getSelectedValue();
      String categoryFilter = null;
      if (category != null) {
        categoryFilter = categories.getSelectedValue().getName();
        log.info("valueChanged {}", categoryFilter);
      } else {
        log.info("valueChanged null");
      }
      getPossibleServicesFromCategory(categoryFilter);
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
    getPossibleServicesFromName(search.getText() + e.getKeyChar());
  }

  @Override
  public void keyPressed(KeyEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void keyReleased(KeyEvent e) {
    // TODO Auto-generated method stub

  }

}