/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.AppenderType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.Gateway;
import org.myrobotlab.swing.ServiceGui;
import org.myrobotlab.swing.SwingGuiGui;
import org.myrobotlab.swing.Welcome;
import org.myrobotlab.swing.widget.AboutDialog;
import org.myrobotlab.swing.widget.Console;
import org.myrobotlab.swing.widget.DockableTab;
import org.myrobotlab.swing.widget.DockableTabPane;
import org.myrobotlab.swing.widget.FileUtil;
import org.slf4j.Logger;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

/**
 * SwingGui - This is the java swing based GUI for MyRobotLab. This service
 * allows other services control features to be displayed. It is the service
 * which you "see" when you start MyRobotLab. It provides a service tab for
 * other services. With its own tab it provides a map of message routes and
 * icons of currently running services.
 * 
 * SwingGui -&gt; Look at service registry SwingGui -&gt; attempt to create a
 * panel for each registered service SwingGui -&gt; create panel SwingGui -&gt;
 * panel.init(this, serviceName); panel.send(Notify, someoutputfn, GUIName,
 * panel.inputfn, data);
 *
 * serviceName (source) --&gt; SwingGui-&gt; msg Arduino arduino01 -&gt; post
 * message -&gt; outbox -&gt; outbound -&gt; notifyList -&gt; reference of
 * sender? (NO) will not transport across process boundry
 * 
 * serviceGUI needs a Runtime Arduino arduin-&gt; post back (data) --&gt;
 * SwingGui - look up serviceGUI by senders name ServiceGUI-&gt;invoke(data)
 * 
 * References :
 * http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components-To-The-
 * Tabs-On-JTabbedPaneI-Now-A-breeze
 * 
 */
public class SwingGui extends Service implements Gateway, WindowListener, ActionListener, Serializable, DocumentListener {

  private static final long serialVersionUID = 1L;
  transient public final static Logger log = LoggerFactory.getLogger(SwingGui.class);

  String graphXML = "";

  boolean fullscreen;
  public int closeTimeout = 0;

  // TODO - make MTOD !! from internet
  // TODO - spawn thread callback / subscribe / promise - for new version

  transient JFrame frame;
  transient DockableTabPane tabs;// is loaded = new DockableTabPane(this);
  transient SwingGuiGui guiServiceGui;
  transient JPanel tabPanel;
  Map<String, String> userDefinedServiceTypeColors = new HashMap<String, String>();
  /**
   * the all important 2nd stage routing map after the message gets back to the
   * gui service the 'rest' of the callback is handled with this data structure
   * 
   * <pre>
   *     "serviceName.method" --&gt; List<ServiceGui>
   *     Map<{name}.{method}, List<ServiceGui>>> nameMethodCallbackMap
   * 
   * </pre>
   * 
   * The same mechanism is employed in mrl.js to handle all call-backs to
   * ServiceGui.js derived panels.
   * 
   * FIXME / TODO ? - "Probably" should be Map<{name}, Map<{method}, List
   * <ServiceGui>>>
   *
   */

  transient Map<String, List<ServiceGui>> nameMethodCallbackMap = new HashMap<String, List<ServiceGui>>();

  /**
   * normalized set of ServiceGui(s)
   */
  transient Map<String, ServiceGui> serviceGuis = new TreeMap<>();

  transient JTextField status = new JTextField("status:");
  transient JButton statusClear = new JButton("clear");

  transient final JTextField search = new JTextField();

  boolean active = false;

  /**
   * used for "this" reference in anonymous swing utilities calls
   */
  transient SwingGui self;
  transient private JMenu export;
  transient private JMenu import_;
  transient private JMenu refresh;
  private String guiId;

  static public void attachJavaConsole() {
    JFrame j = new JFrame("Java Console");
    j.setSize(500, 550);
    Console c = new Console();
    j.add(c.getScrollPane());
    j.setVisible(true);
    c.startLogging();
  }

  static public void console() {
    attachJavaConsole();
  }

  public static List<Component> getAllComponents(final Container c) {
    Component[] comps = c.getComponents();
    List<Component> compList = new ArrayList<Component>();
    for (Component comp : comps) {
      compList.add(comp);
      if (comp instanceof Container)
        compList.addAll(getAllComponents((Container) comp));
    }
    return compList;
  }

  public void setColor(String serviceType, String hexColor) {
    userDefinedServiceTypeColors.put(serviceType, hexColor);
  }

  public static Color getColorFromURI(Object uri) {
    StringBuffer sb = new StringBuffer(String.format("%d", Math.abs(uri.hashCode())));
    Color c = new Color(Color.HSBtoRGB(Float.parseFloat("0." + sb.reverse().toString()), 0.8f, 0.7f));
    return c;
  }

  public Color getColorHash(String uri) {
    if (userDefinedServiceTypeColors.containsKey(uri)) {
      // e.g. "#FFCCEE"
      return Color.decode(userDefinedServiceTypeColors.get(uri));
    }
    StringBuffer sb = new StringBuffer(String.format("%d", Math.abs(uri.hashCode())));
    Color c = new Color(Color.HSBtoRGB(Float.parseFloat("0." + sb.reverse().toString()), 0.4f, 0.95f));
    return c;
  }

  static public void restart() {
    JFrame frame = new JFrame();
    frame.setLocationByPlatform(true);

    int ret = JOptionPane.showConfirmDialog(frame, "<html>New components have been added,<br>" + " it is necessary to restart in order to use them.</html>", "restart",
        JOptionPane.YES_NO_OPTION);
    if (ret == JOptionPane.OK_OPTION) {
      log.info("restarting");
      // Runtime.restart(restartScript);
      Runtime.getInstance().restart(); // <-- FIXME WRONG need to send
      // message - may be remote !!
    } else {
      log.info("chose not to restart");
      return;
    }
  }

  public SwingGui(String n, String id) {
    super(n, id);
    this.self = this;
    guiId = "gui-" + getId();

    status.setEditable(false);
    if (tabs == null) {
      tabs = new DockableTabPane(this);
    } else {
      tabs.setStateSaver(this);
    }

    // setup the virtual reflection
    try {
      connect("gui://local/messages");
    } catch (Exception e) {
    }

    log.info("tabs size {}", tabs.size());

    for (String title : tabs.keySet()) {
      DockableTab tab = tabs.get(title);
      log.info("{} ({},{}) w={} h={}", title, tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight());
    }
    // subscribe to services being added and removed
    // we want to know about new services registered or released
    // we create explicit mappings vs just [
    // subscribe(Runtime.getRuntimeName(), "released") ]
    // because we would 'mask' the Runtime's service subscriptions -
    // intercept and mask them
    // new service --go--> addTab
    // remove service --go--> removeTab
    subscribe("runtime", "released", getName(), "removeTab");
    // subscribe("runtime", "registered", getName(), "addTab");
    subscribeToRuntime("registered");
  }
  
  public void onRegistered(Registration registration) {
    addTab(registration.service);
  }

  public void about() {
    new AboutDialog(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    Object o = e.getSource();

    if (statusClear == o) {
      status.setForeground(Color.black);
      status.setOpaque(false);
      status.setText("status:");
    }

    if ("unhide all".equals(cmd)) {
      unhideAll();
    } else if ("hide all".equals(cmd)) {
      hideAll();
    } else if (cmd.equals(AppenderType.FILE)) {
      Logging logging = LoggingFactory.getInstance();
      logging.addAppender(AppenderType.FILE);
    } else if (cmd.equals(AppenderType.NONE)) {
      Logging logging = LoggingFactory.getInstance();
      logging.addAppender(AppenderType.NONE);
    } else if ("explode".equals(cmd)) {
      explode();
    } else if ("collapse".equals(cmd)) {
      collapse();
    } else if ("about".equals(cmd)) {
      new AboutDialog(this);
      // display();
    } else if ("refresh".equals(cmd)) {
      send(getSelected(), "broadcastState");
    } else if ("refresh all".equals(cmd)) {
      for (String serviceName : Runtime.getServiceNames()) {
        send(serviceName, "broadcastState");
      }
    } else if ("load".equals(cmd)) {
      // get current focus
      String tabName = getSelected();
      ServiceInterface si = Runtime.getService(tabName);
      if (si == null) {
        log.info("%s is not a service - please select the service to save", tabName);
      } else if (si.isLocal()) {
        si.load();
      } else {
        send(tabName, "load");
      }
      info("loaded %s", tabName);
    } else if ("load all".equals(cmd)) {
      String[] services = Runtime.getServiceNames();
      for (String service : services) {
        info("loading %s", service);
        ServiceInterface si = Runtime.getService(service);
        if (si == null) {
          log.info("%s is not a service - please select the service to save", service);
        } else if (si.isLocal()) {
          si.load();
        } else {
          send(service, "load");
        }
      }
      info("loaded all services");
    } else if ("export".equals(cmd)) {
      // get current focus
      String tabName = getSelected();
      ServiceInterface si = Runtime.getService(tabName);
      if (si == null) {
        log.info("%s is not a service - please select the service to save", tabName);
        return;
      }
      // send(tabName, "save"); save has just become serialize
      String newFile = FileUtil.saveAsFileName(getFrame(), String.format("%s.py", tabName));
      try {
        if (newFile != null) {
          export(newFile, newFile);
        }
      } catch (IOException e1) {
        log.error("could not export {} to {}", tabName, newFile);
      }
      info("exported %s", tabName);
    } else if ("export all".equals(cmd)) {

      String newFile = FileUtil.saveAsFileName(getFrame(), "export.py");
      try {
        if (newFile != null) {
          exportAll(newFile);
        }
      } catch (IOException e1) {
        log.error("could not export all to {}", newFile);
      }

      info("saved all exported");
    } else {
      log.info("unknown command {}", cmd);
    }
  }

  /**
   * add a service tab to the SwingGui
   * 
   * @param sw
   *          - name of service to add
   * 
   *          FIXME - full parameter of addTab(final String serviceName, final
   *          String serviceType, final String lable) then overload
   * 
   *          FIXME - this method should be named onRegistered
   */
  synchronized public void addTab(final ServiceInterface sw) {

    if (Runtime.isHeadless()) {
      log.warn("{} SwingGui is in headless environment", getName());
      return;
    }

    // let all current "tabs" handle a new service - possibly by refreshing sets
    // of controllers, encoders or other possible "attachables"
    /// xxx for (ServiceGui serviceGuis)

    // scroll.addItem(sw.getName());

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

    	// FIXME - this will be an issue of name collision in distributed mrl !!!
        String name = sw.getFullName();// sw.getName();

        // change tab color based on name
        // it is better to add a new interfaced method I think ?

        String guiClass = String.format("org.myrobotlab.swing.%sGui", sw.getSimpleName());

        log.debug("createTab {} {}", name, guiClass);
        ServiceGui newGui = null;

        try {
          newGui = (ServiceGui) Instantiator.getThrowableNewInstance(null, guiClass, name, self);
        } catch (Exception e) {
          log.info("could not create a {}", guiClass);
        }

        if (newGui == null) {
          log.info("could not construct a {} object - creating generic template", guiClass);
          newGui = (ServiceGui) Instantiator.getNewInstance("org.myrobotlab.swing.NoGui", name, self);
        }

        newGui.subscribeGui();

        // state and status are both subscribed for the service here
        // these are messages going to the services of interest
        subscribe(name, "publishStatus");
        subscribe(name, "publishState");

        // this is preparing our routing map for callback
        // so when we receive our callback message we know where to route it
        subscribeToServiceMethod(String.format("%s.%s", name, CodecUtils.getCallbackTopicName("publishStatus")), newGui);
        subscribeToServiceMethod(String.format("%s.%s", name, CodecUtils.getCallbackTopicName("publishState")), newGui);

        // send a publishState to the service
        // to initialize the ServiceGui - good for remote stuffs
        send(name, "publishState");

        if (getName().equals(name) && guiServiceGui == null && newGui instanceof SwingGuiGui) {
          guiServiceGui = (SwingGuiGui) newGui;
          guiServiceGui.rebuildGraph();
        }

        tabs.addTab(name, newGui.getDisplay(), sw.getDescription());
        tabs.getTabs().setBackgroundAt(tabs.size() - 1, getColorHash(sw.getClass().getSimpleName()));
        tabs.get(name).transitDockedColor = tabs.getTabs().getBackgroundAt(tabs.size() - 1);
        // pack(); FIXED THE EVIL BLACK FROZEN GUI ISSUE !!!!

        // spin through all service guis
        for (ServiceGui sg : serviceGuis.values()) {
          sg.onRegistered(sw);
        }

        // simple normalized data structure
        serviceGuis.put(name, newGui);
        pack();
      }
    });
  }

  public JFrame createJFrame(boolean fullscreen) {
    if (Runtime.isHeadless()) {
      log.warn("{} SwingGui is in headless environment", getName());
      return null;
    }
    if (frame != null) {
      frame.dispose();
    }
    frame = new JFrame();

    if (!fullscreen) {
      frame.addWindowListener(this);
      frame.setTitle("myrobotlab - " + Platform.getLocalInstance().getId() + " " + Runtime.getVersion().trim());

      frame.add(tabPanel);
      String logoFile = Util.getResourceDir() + File.separator + "mrl_logo_36_36.png";
      Toolkit kit = Toolkit.getDefaultToolkit();
      Image img = kit.createImage(logoFile);
      frame.setIconImage(img);

      // menu
      frame.setJMenuBar(createMenu());
      frame.setVisible(true);
      frame.pack();
    } else {
      frame.add(tabPanel);
      frame.setJMenuBar(createMenu());

      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      frame.setUndecorated(true);
      frame.setVisible(true);
    }
    return frame;
  }

  public void maximize() {
    frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
  }

  /*
   * Build the menu for display.
   */
  public JMenuBar createMenu() {
    JMenuBar menuBar = new JMenuBar();

    menuBar.add(search);
    menuBar.add(Box.createHorizontalGlue());

    export = new JMenu("export");
    JMenuItem mi = new JMenuItem("export");
    mi.addActionListener(this);
    export.add(mi);
    mi = new JMenuItem("export all");
    mi.addActionListener(this);
    export.add(mi);

    menuBar.add(export);

    import_ = new JMenu("import");
    mi = new JMenuItem("import");
    mi.addActionListener(this);
    import_.add(mi);
    mi = new JMenuItem("import all");
    mi.addActionListener(this);
    import_.add(mi);
    menuBar.add(import_);

    refresh = new JMenu("refresh");
    mi = new JMenuItem("refresh");
    mi.addActionListener(this);
    refresh.add(mi);
    mi = new JMenuItem("refresh all");
    mi.addActionListener(this);
    refresh.add(mi);
    menuBar.add(refresh);

    JMenu help = new JMenu("help");
    JMenuItem about = new JMenuItem("about");
    about.addActionListener(this);
    help.add(about);
    menuBar.add(help);

    search.getDocument().addDocumentListener(this);
    // search.addActionListener(this);
    return menuBar;
  }

  /**
   * This method puts unique service.method key and ServiceGui into map also in
   * mrl.js
   * 
   * the format of the key needs to be {name}.method
   * 
   */
  public void subscribeToServiceMethod(String key, ServiceGui sg) {
    List<ServiceGui> list = null;
    if (nameMethodCallbackMap.containsKey(key)) {
      list = nameMethodCallbackMap.get(key);
    } else {
      list = new ArrayList<ServiceGui>();
      nameMethodCallbackMap.put(key, list);
    }

    boolean found = false;
    for (int i = 0; i < list.size(); ++i) {
      ServiceGui existingSg = list.get(i);
      if (existingSg == sg) {
        found = true;
      }
    }
    if (!found) {
      list.add(sg); // that was easy ;)
    }
  }

  /*
   * closes window and puts the panel back into the tabbed pane
   */
  public void dockTab(final String title) {
    tabs.dockTab(title);
  }

  public HashMap<String, mxCell> getCells() {
    return guiServiceGui.serviceCells;
  }

  public String getDstMethodName() {
    return guiServiceGui.dstMethodName.getText();
  }

  public String getDstServiceName() {
    return guiServiceGui.dstServiceName.getText();
  }

  public JFrame getFrame() {
    return frame;
  }

  public mxGraph getGraph() {
    return guiServiceGui.graph;
  }

  public String getGraphXML() {
    return graphXML;
  }

  public String getSrcMethodName() {
    return guiServiceGui.srcMethodName.getText();
  }

  public String getSrcServiceName() {
    return guiServiceGui.srcServiceName.getText();
  }

  /**
   * set the main status bar with Status information
   * 
   * @param inStatus
   */
  public void setStatus(Status inStatus) {

    if (inStatus.isError()) {
      status.setOpaque(true);
      status.setForeground(Color.white);
      status.setBackground(Color.red);
    } else if (inStatus.isWarn()) {
      status.setOpaque(true);
      status.setForeground(Color.black);
      status.setBackground(Color.yellow);
    } else {
      status.setForeground(Color.black);
      status.setOpaque(false);
    }

    if (inStatus.detail != null && inStatus.detail.length() > 128) {
      status.setText(inStatus.detail.substring(128));
    } else {
      status.setText(inStatus.detail);
    }
  }

  public void hideAll() {
    log.info("hideAll");
    for (String key : tabs.keySet()) {
      hideTab(key);
    }
  }

  public void hideTab(final String title) {
    tabs.hideTab(title);
  }

  public void noWorky() {
    // String img =
    // SwingGui.class.getResource(Util.getRessourceDir() +
    // "/expert.jpg").toString();
    String logon = (String) JOptionPane.showInputDialog(getFrame(),
        "<html>This will send your myrobotlab.log file<br><p align=center>to our crack team of experts,<br> please type your myrobotlab.org user</p></html>", "No Worky!",
        JOptionPane.WARNING_MESSAGE, Util.getResourceIcon("expert.jpg"), null, null);
    if (logon == null || logon.length() == 0) {
      return;
    }

    try {
      if (Runtime.noWorky(logon).isInfo()) {
        JOptionPane.showMessageDialog(getFrame(), "log file sent, Thank you", "Sent !", JOptionPane.INFORMATION_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(getFrame(), "could not send log file :(", "DOH !", JOptionPane.ERROR_MESSAGE);
      }
    } catch (Exception e1) {
      JOptionPane.showMessageDialog(getFrame(), Service.stackToString(e1), "DOH !", JOptionPane.ERROR_MESSAGE);
    }

  }

  public void pack() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (frame != null) {
          frame.pack();
          frame.repaint();
        }
      }
    });
  }

  @Override
  public boolean preProcessHook(Message m) {
    // FIXME - problem with collisions of this service's methods
    // and dialog methods ?!?!?

    // if the method name is == to a method in the SwingGui
    if (methodSet.contains(m.method)) {
      // process the message like a regular service
      return true;
    }

    // otherwise send the message to the dialog with the senders name
    // key is now for callback is {name}.method
    String key = String.format("%s.%s", m.sender, m.method);
    List<ServiceGui> sgs = nameMethodCallbackMap.get(key);
    if (sgs == null) {
      log.error("attempting to update derived ServiceGui with - callback " + key + " not available in map " + getName());
    } else {
      
      for (int i = 0; i < sgs.size(); ++i) {
        ServiceGui sg = sgs.get(i);
        invokeOn(sg, m.method, m.data);
      }
    }

    return false;
  }
  
  public void onReleased(String serviceName) {
    removeTab(serviceName);
  }

  // FIXME - when a service is 'being' released Runtime should
  // manage the releasing of the subscriptions !!!
  synchronized public void removeTab(final String serviceName) {

    serviceGuis.remove(serviceName);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        String name = serviceName;
        tabs.removeTab(name);
        if (guiServiceGui != null) {
          guiServiceGui.rebuildGraph();
        }
        if (frame != null) {
          frame.pack();
          frame.repaint();
        }
      }
    });
  }

  public void setArrow(String s) {
    guiServiceGui.arrow0.setText(s);
  }

  public void setDstMethodName(String d) {
    guiServiceGui.dstMethodName.setText(d);
  }

  public void setDstServiceName(String d) {
    guiServiceGui.dstServiceName.setText(d);
  }

  public void setGraphXML(String xml) {
    graphXML = xml;
  }

  public void setPeriod0(String s) {
    guiServiceGui.period0.setText(s);
  }

  public void setPeriod1(String s) {
    guiServiceGui.period1.setText(s);
  }

  public void setSrcMethodName(String d) {
    guiServiceGui.srcMethodName.setText(d);
  }

  public void setSrcServiceName(String d) {
    guiServiceGui.srcServiceName.setText(d);
  }

  @Override
  synchronized public void startService() {
    super.startService();
    // FIXME - silly - some of these should be initialized in the constructor or
    // before !!
    if (!active) {
      active = true;

      // ===== build tab panels begin ======
      // builds all the service tabs for the first time called when
      // SwingGui
      // starts

      // add welcome table - weird - this needs to be involved in display
      tabs.addTab("Welcome", new Welcome("welcome", this).getDisplay());

      /**
       * pack() repaint() works on current selected (non-hidden) tab welcome is
       * the first panel when the UI starts - therefore this controls the
       * initial size .. however the largest panel typically at this point is
       * the RuntimeGui - so we set it to the rough size of that panel
       */

      Map<String, ServiceInterface> services = Runtime.getRegistry();
      log.info("buildTabPanels service count " + Runtime.getRegistry().size());

      TreeMap<String, ServiceInterface> sortedMap = new TreeMap<String, ServiceInterface>(services);
      Iterator<String> it = sortedMap.keySet().iterator();

      tabPanel = new JPanel(new BorderLayout());
      tabPanel.add(tabs.getTabs(), BorderLayout.CENTER);
      JPanel statusPanel = new JPanel(new BorderLayout());
      statusPanel.add(status, BorderLayout.CENTER);
      statusPanel.add(statusClear, BorderLayout.EAST);
      tabPanel.add(statusPanel, BorderLayout.SOUTH);

      statusClear.addActionListener(this);
      status.setOpaque(true);

      while (it.hasNext()) {
        String serviceName = it.next();
        addTab(Runtime.getService(serviceName));
      }

      // frame.repaint(); not necessary - pack calls repaint

      // pick out a reference to our own gui
      List<ServiceGui> sgs = nameMethodCallbackMap.get(getName());
      if (sgs != null) {
        for (int i = 0; i < sgs.size(); ++i) {
          // another potential bug :(
          guiServiceGui = (SwingGuiGui) sgs.get(i);
        }
      }

      // create gui parts
      createJFrame(fullscreen);
    }
  }

  public String getSelected() {
    return tabs.getSelected();
  }

  @Override
  public void stopService() {
    super.stopService();
    if (frame != null) {
      frame.dispose();
    }
    active = false;
  }

  public void explode() {
    tabs.explode();
  }

  public void collapse() {
    tabs.collapse();
  }

  public void fullscreen(boolean b) {
    if (fullscreen != b) {
      fullscreen = b;
      createJFrame(b);
      save(); // request to alter fullscreen
    }
  }

  public void undockTab(final String title) {
    tabs.undockTab(title);
  }

  public void unhideAll() {
    log.info("unhideAll");
    for (String key : tabs.keySet()) {
      unhideTab(key);
    }
  }

  // must handle docked or undocked & re-entrant for unhidden
  public void unhideTab(final String title) {
    tabs.unhideTab(title);
  }

  // @Override - only in Java 1.6
  @Override
  public void windowActivated(WindowEvent e) {
    // log.info("windowActivated");
  }

  // @Override - only in Java 1.6
  @Override
  public void windowClosed(WindowEvent e) {
    // log.info("windowClosed");
  }

  // @Override - only in Java 1.6
  @Override
  public void windowClosing(WindowEvent e) {
    Runtime.shutdown(closeTimeout);
  }

  // @Override - only in Java 1.6
  @Override
  public void windowDeactivated(WindowEvent e) {
    // log.info("windowDeactivated");
  }

  // @Override - only in Java 1.6
  @Override
  public void windowDeiconified(WindowEvent e) {
    // log.info("windowDeiconified");
  }

  // @Override - only in Java 1.6
  @Override
  public void windowIconified(WindowEvent e) {
    // log.info("windowActivated");
  }

  // @Override - only in Java 1.6
  @Override
  public void windowOpened(WindowEvent e) {
    // log.info("windowOpened");

  }

  public static void main(String[] args) throws ClassNotFoundException, URISyntaxException {
    LoggingFactory.getInstance().configure();
    Logging logging = LoggingFactory.getInstance();
    try {
      logging.setLevel(Level.INFO);
      Runtime.main(new String[] { "--interactive", "--id", "r1" });

      Runtime.start("gui", "SwingGui");
      // Runtime.start("python", "Python");
      /*
      Runtime.start("clock01", "Clock");
      Runtime.start("clock02", "Clock");
      Runtime.start("clock03", "Clock");
      Runtime.start("clock04", "Clock");
      Runtime.start("clock05", "Clock");
      */

      boolean done = true;
      if (done) {
        return;
      }

      // Runtime.start("i01", "InMoov");
      // Runtime.start("mac", "Runtime");
      // Runtime.start("python", "Python");
      // remote.setDefaultPrefix("raspi");
      // remote.connect("tcp://127.0.0.1:6767");

      // Runtime.start("python", "Python");
      Arduino arduino = (Arduino) Runtime.start("client", "Arduino");
      Serial serial = arduino.getSerial();
      /*
       * for (int i = 0; i < 40; ++i) { Runtime.start(String.format("servo%d",
       * i), "Servo"); }
       */

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void setActiveTab(String title) {
    // we need to wait a little after Runtime.start to select an active tab
    // TODO understand why we need a sleep(1000); - cuz swing is lame :(
    this.tabs.getTabs().setSelectedIndex(tabs.getTabs().indexOfTab(title));
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(SwingGui.class.getCanonicalName());
    meta.addDescription("Service used to graphically display and control other services");
    meta.addCategory("display");

    meta.includeServiceInOneJar(true);
    meta.addDependency("com.fifesoft", "rsyntaxtextarea", "2.0.5.1");
    meta.addDependency("com.fifesoft", "autocomplete", "2.0.5.1");
    meta.addDependency("com.jidesoft", "jide-oss", "3.6.18");
    meta.addDependency("com.mxgraph", "jgraphx", "1.10.4.2");

    return meta;
  }

  public Component getDisplay() {
    return (Component) tabs.getTabs();
  }

  public void setDesktop(String name) {
    tabs.setDesktop(name);
  }

  public void resetDesktop(String name) {
    tabs.resetDesktop(name);
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    // log.info("changedUpdate");
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    // log.info("insertUpdate");
    Map<String, DockableTab> m = tabs.getDockableTabs();
    String type = search.getText().toLowerCase();
    for (DockableTab t : m.values()) {
      if (t.getTitleLabel().getText().toString().toLowerCase().contains(type)) {
        t.unhideTab();
      } else {
        t.hideTab();
      }
    }
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    String type = search.getText();
    Map<String, DockableTab> m = tabs.getDockableTabs();
    if (type == null || type.length() == 0) {
      for (DockableTab t : m.values()) {
        t.unhideTab();
      }
    }
  }

  // WOW ! GATEWAY FUNCTION !!
  public boolean isVirtualLocal(Message msg) {
    String msgId = msg.getId();
    if (msgId == null) {
      log.error("msg cannot be tested for local cli - needs to be not null {}", msg);
    }
    return msgId.endsWith(guiId);
  }

  @Override
  public void connect(String uri) throws Exception {
    // easy single client support
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("gateway", getName());
    attributes.put("c-type", getSimpleName());
    attributes.put("id", Runtime.getInstance().getId() + "-swing");
    String uuid = java.util.UUID.randomUUID().toString();
    attributes.put("uuid", uuid);
    Runtime.getInstance().addConnection(uuid, attributes);
    Runtime.updateRoute(guiId, uuid);
  }

  @Override
  public List<String> getClientIds() {
    return null;
  }

  @Override
  public Map<String, Map<String, Object>> getClients() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void sendRemote(Message msg) throws Exception {
    // TODO Auto-generated method stub
    log.info("sendRemote");
  }

  @Override
  public Object sendBlockingRemote(Message msg, Integer timeout) throws Exception {
    log.info("sendBlockingRemote");
    return null;
  }

  @Override
  public boolean isLocal(Message msg) {
    log.info("isLocal");
    return false;
  }

  @Override
  public Message getDefaultMsg(String connId) {
    return Runtime.getInstance().getDefaultMsg(connId);
  }

}
