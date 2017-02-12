/**
 *                    
 * @author GroG (at) myrobotlab.org
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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.Swing;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * @author GroG ServiceGUI - is owned immediately or through a routing map to
 *         ultimately a Service it has the capability of undocking and docking
 *         itself
 * 
 */

// if need an interface make TabAdapter extend WindowAdapter and contain it
public abstract class ServiceGui extends WindowAdapter implements TabControlEventHandler {

  public final static Logger log = LoggerFactory.getLogger(ServiceGui.class);
  public final String boundServiceName;
  public final Swing myService; // FIXME - rename gui

  /**
   * display is the panel to be added to the JTabbedPane it has border layout
   * similar to how a default frame its a good paradigm to think of the
   * JTabbedPane as its own frame since when it become undocked - it is within a
   * frame
   */
  transient JPanel display = new JPanel(new BorderLayout());

  // border parts
  transient JPanel north;
  transient JPanel south;
  transient JPanel center;
  transient JPanel west;
  transient JPanel east;

  int x;
  int y;
  int width = 800;
  int height = 200;

  transient private JFrame undocked;

  // FIXME - refactor ...
  TabControl2 tabControl;
  private JTabbedPane tabs; // the tabbed pane this tab control belongs to

  protected ServiceGui self;
  boolean isHidden = false;

  public ServiceGui(final String boundServiceName, final Swing myService, JTabbedPane tabs) {
    self = this;
    this.boundServiceName = boundServiceName;
    this.myService = myService;
    this.tabs = tabs;
    this.tabControl = new TabControl2(this, myService.getTabs(), display, boundServiceName);

    north = new JPanel(new GridLayout(0, 2)); // flow
    west = new JPanel(new GridLayout(0, 2));
    // center = new JPanel(); // vertical stack
    // center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
    center = new JPanel(new GridLayout(0, 2));
    east = new JPanel(new GridLayout(0, 2));
    // east = new JPanel(new GridLayout(0, 1)); // vertical stack
    // new BoxLayout(east, BoxLayout.Y_AXIS);
    south = new JPanel(new GridLayout(0, 2)); // flow

    display.add(north, BorderLayout.NORTH);
    display.add(east, BorderLayout.EAST);
    display.add(center, BorderLayout.CENTER);
    display.add(west, BorderLayout.WEST);
    display.add(south, BorderLayout.SOUTH);

    // adding content to JTabbedPane ..
    tabs.addTab(boundServiceName, display);
    tabs.setTabComponentAt(tabs.getTabCount() - 1, tabControl);
  }

  @Override
  public void actionPerformed(ActionEvent e, String tabName) {
    String cmd = e.getActionCommand();
    // parent.getSelectedComponent()
    String label = tabName;
    if (label.equals(tabName)) {
      // Service Frame
      ServiceInterface sw = Runtime.getService(tabName);
      if ("info".equals(cmd)) {
        BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName());
      } else if ("undock".equals(cmd)) {
        undockPanel();
      } else if ("release".equals(cmd)) {
        // send to service to release
        // runtime is the last thing called from the service's own release
        myService.send(sw.getName(), "releaseService");
      } else if ("hide".equals(cmd)) {
        hidePanel();
      }
    } else {
      // Sub Tabbed sub pane
      ServiceInterface sw = Runtime.getService(label);
      if ("info".equals(cmd)) {
        BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName() + "#" + tabName);

      } else if ("undock".equals(cmd)) {
        undockPanel();
      }
    }

  }

  /**
   * stub for subscribing all service specific/gui events for desired callbacks
   */
  public void subscribeGui() {
  }

  /**
   * stub for unsubscribing all service specific/gui events from desired
   * callbacks
   */
  public void unsubscribeGui() {
  }

  // -- TabControlEventHandler -- begin
  @Override
  /**
   * closes window and puts the panel back into the tabbed pane
   */
  synchronized public void dockPanel() {

    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {

        // setting tabcontrol
        String label = tabControl.getText();
        display.setVisible(true);
        tabs.add(display);
        log.debug("here tabs count {}", tabs.getTabCount());
        tabs.setTabComponentAt(tabs.getTabCount() - 1, tabControl);

        savePosition();

        log.debug("{}", tabs.indexOfTab(label));

        if (undocked != null) {
          undocked.dispose();
          undocked = null;
        }

        // FIXME - necessary ? or just this panel invalidate?
        myService.getFrame().pack();
        myService.save();

        tabs.setSelectedComponent(display);
      }
    });
  }

  public JPanel getDisplay() {
    return display;
  }

  public Component getTabControl() {
    return tabControl;
  }

  public void hidePanel() {
    if (isHidden) {
      log.info("{} panel is already hidden", tabControl.getText());
      return;
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        log.info("hidePanel");
        if (undocked != null) {
          undocked.setVisible(false);
        } else {
          // YAY! - the way to do it !
          int index = tabs.indexOfComponent(display);
          // int index = tabs.indexOfTab(tabControl.getText());
          if (index != -1) {
            tabs.remove(index);
          } else {
            log.error("{} - has -1 index", tabControl.getText());
          }
        }

        isHidden = true;

      }
    });
  }

  // public abstract void init();

  public boolean isDocked() {
    return undocked == null;
  }

  public boolean isHidden() {
    return isHidden;
  }

  /**
   * hook for Swing framework to query each panel before release checking
   * if any panel needs user input before shutdown
   * 
   * @return
   */
  public boolean isReadyForRelease() {
    return true;
  }

  public void makeReadyForRelease() {

  }

  @Override
  public void mouseClicked(MouseEvent event, String tabName) {
    if (myService != null) {
      myService.setLastTabVisited(tabName);
    }
  }

  public void remove() {
    unsubscribeGui();
    hidePanel();
    if (undocked != null) {
      undocked.dispose();
    }
  }

  public void savePosition() {
    if (undocked != null) {
      Point point = undocked.getLocation();
      x = point.x;
      y = point.y;
      width = undocked.getWidth();
      height = undocked.getHeight();
    }
  }

  public void send(String method) {
    send(method, (Object[]) null);
  }

  public void send(String method, Object... params) {
    myService.send(boundServiceName, method, params);
  }

  public void subscribe(String method) {
    subscribe(method, CodecUtils.getCallBackName(method), (Class<?>[]) null);
  }

  public void subscribe(String method, String callback, Class<?>... parameterType) {
    MRLListener listener = new MRLListener(method, myService.getName(), callback);
    myService.send(boundServiceName, "addListener", listener);
  }

  @Override
  /**
   * undocks a tabbed panel into a JFrame FIXME - NORMALIZE - there are similar
   * methods in Swing FIXME - there needs to be clear pattern replacement -
   * this is a decorator - I think... (also it will always be Swing)
   * 
   */
  // can't return JFrame referrence since its in a invokeLater..
  public void undockPanel() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        tabs.remove(display);

        String label = tabControl.getText();

        if (undocked != null) {
          log.warn("{} undocked already created", label);
        }

        undocked = new JFrame(label);

        undocked.getContentPane().add(display);

        // icon
        URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image img = kit.createImage(url);
        undocked.setIconImage(img);

        if (x != 0 || y != 0) {
          undocked.setLocation(x, y);
        }

        if (width != 0 || height != 0) {
          undocked.setSize(width, height);
        }

        undocked.addWindowListener(self);

        undocked.setVisible(true);
        undocked.pack();
      }
    });

  }

  public void unhidePanel() {
    if (!isHidden) {
      log.info("{} panel is already un-hidden", tabControl.getText());
      return;
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        log.info("unhidePanel {}", tabControl.getText());

        if (undocked != null) {
          undocked.setVisible(true);
        } else {
          display.setVisible(true);
          // tabs.addTab(tabControl.getText(), tabControl);

          tabs.add(tabControl.getText(), display);
          tabs.setTabComponentAt(tabs.getTabCount() - 1, tabControl);

        }

        // FIXME don't know what i'm doing...
        display.revalidate();
        // getFrame().revalidate();
        // getFrame().pack();

        isHidden = false;
      }

    });

  }

  // TODO - more closely model java event system with addNotification or
  // addListener
  public void unsubscribe(String inOutMethod) {
    unsubscribe(inOutMethod, inOutMethod, (Class<?>[]) null);
  }

  public void unsubscribe(String inMethod, String outMethod) {
    unsubscribe(inMethod, outMethod, (Class<?>[]) null);
  }

  public void unsubscribe(String outMethod, String inMethod, Class<?>... parameterType) {

    MRLListener listener = null;
    if (parameterType != null) {
      listener = new MRLListener(outMethod, myService.getName(), inMethod);
    } else {
      listener = new MRLListener(outMethod, myService.getName(), inMethod);
    }
    myService.send(boundServiceName, "removeListener", listener);

  }

  // -- TabControlEventHandler -- end

  @Override
  public void windowClosing(WindowEvent winEvt) {
    dockPanel();
  }

  // adding helper methods.. because Swing can be a bit of a pain...
  /*
   * public JPanel addGroup(String title, Object... components){ JPanel panel =
   * new JPanel(new GridLayout(0, components.length)); }
   */

  public JPanel addPanel(String title, Object... components) {
    JPanel panel = new JPanel();
    if (title != null) {
      TitledBorder t;
      t = BorderFactory.createTitledBorder(title);
      panel.setBorder(t);
    }
    for (int i = 0; i < components.length; ++i) {
      Object o = components[i];
      JComponent j = null;
      if (o == null) {
        log.error("addLine - component is null !");
        return null;
      }
      if (o.getClass().equals(String.class)) {
        JLabel label = new JLabel((String) o);
        j = label;
      } else {
        j = (JComponent) o;
      }
      panel.add(j);
    }

    return panel;
  }

  public JComponent addComponents(JPanel panel, Object... components) {
    panel.setLayout(new GridLayout(0, components.length));
    // JPanel panel = new JPanel(new GridLayout(1, 0));
    // JPanel panel = new JPanel(new GridBagLayout());
    // panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    // panel.setAlignmentY(Component.TOP_ALIGNMENT);
    // panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    // Box panel = Box.createHorizontalBox(); // auto-glues (good for tables)
    // panel.add(Box.createVerticalGlue());
    // panel.setAlignmentY(Component.TOP_ALIGNMENT);
    // panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    GridBagConstraints gc = new GridBagConstraints();
    for (int i = 0; i < components.length; ++i) {
      Object o = components[i];
      JComponent j = null;
      if (o == null) {
        log.error("addLine - component is null !");
        return null;
      }
      if (o.getClass().equals(String.class)) {
        JLabel label = new JLabel((String) o);
        j = label;
      } else {
        j = (JComponent) o;
      }

      gc.gridx = 0;
      gc.gridy = 0;
      // must be a jcomponent at this point..
      // j.setAlignmentY(Component.TOP_ALIGNMENT);
      // j.setAlignmentX(Component.LEFT_ALIGNMENT);
      // panel.add(j, gc.gridx++);
      panel.add(j);
    }

    return panel;
  }

  public JComponent addTopLine(Object... components) {
    JComponent c = addComponents(north, components);
    return c;
  }

  public JComponent addBottomLine(Object... components) {
    JComponent c = addComponents(south, components);
    return c;
  }

  public JComponent addLeftLine(Object... components) {
    JComponent c = addComponents(west, components);
    return c;
  }

  public JComponent addRightLine(Object... components) {
    JComponent c = addComponents(east, components);
    return c;
  }

  /**
   * add a line to a panel
   * 
   * @param components
   * @return
   */
  public JComponent addLine(Object... components) {
    JComponent c = addComponents(center, components);
    // center.add(c);
    return c;
  }

  public void setTitle(String title) {
    TitledBorder t;
    t = BorderFactory.createTitledBorder(title);
    center.setBorder(t);
  }

  public void setTopTitle(String title) {
    TitledBorder t;
    t = BorderFactory.createTitledBorder(title);
    north.setBorder(t);
  }

  public void setBottomTitle(String title) {
    TitledBorder t;
    t = BorderFactory.createTitledBorder(title);
    south.setBorder(t);
  }

  public void setLeftTitle(String title) {
    TitledBorder t;
    t = BorderFactory.createTitledBorder(title);
    west.setBorder(t);
  }

  public void setRightTitle(String title) {
    TitledBorder t;
    t = BorderFactory.createTitledBorder(title);
    east.setBorder(t);
  }

}
