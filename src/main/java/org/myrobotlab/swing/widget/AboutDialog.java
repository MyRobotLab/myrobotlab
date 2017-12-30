package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class AboutDialog extends JDialog implements ActionListener, MouseListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(AboutDialog.class);

  JButton noWorky = null;
  JButton ok = null;
  JFrame parent = null;
  JLabel versionLabel = new JLabel(Runtime.getVersion());
  SwingGui gui;

  public AboutDialog(SwingGui gui) {
    super(gui.getFrame(), "about", true);
    this.gui = gui;
    this.parent = gui.getFrame();
    if (parent != null) {
      Dimension parentSize = parent.getSize();
      Point p = parent.getLocation();
      setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
    }
    
    Platform platform = Platform.getLocalInstance();

    JPanel content = new JPanel(new BorderLayout());
    content.setPreferredSize(new Dimension(350, 150));
    getContentPane().add(content);

    // picture
    JLabel pic = new JLabel();
    pic.setIcon(Util.getResourceIcon("mrl_logo_about_128.png"));
    content.add(pic, BorderLayout.WEST);

    JPanel center = new JPanel(new GridLayout(0, 1));
    JLabel link = new JLabel("<html><p align=center><a href=\"http://myrobotlab.org\">http://myrobotlab.org</a><html>");
    link.addMouseListener(this);
    content.add(center, BorderLayout.CENTER);
    
    center.add(link);
    JPanel flow = new JPanel();
    flow.add(new JLabel("platform "));
    flow.add(new JLabel(platform.toString()));
    center.add(flow);
    
    flow = new JPanel();
    flow.add(new JLabel("version "));
    flow.add(new JLabel(platform.getVersion()));
    center.add(flow);

    flow = new JPanel();
    flow.add(new JLabel("branch "));
    flow.add(new JLabel(platform.getBranch()));
    flow.setAlignmentX(LEFT_ALIGNMENT);
    center.add(flow);

    
    JPanel buttonPane = new JPanel();

    ok = new JButton("OK");
    buttonPane.add(ok);
    ok.addActionListener(this);

    noWorky = new JButton("Help, it \"no-worky\"!");
    buttonPane.add(noWorky);
    noWorky.addActionListener(this);

    getContentPane().add(buttonPane, BorderLayout.SOUTH);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    pack();
    setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == ok) {
      setVisible(false);
      dispose();
    } else if (source == noWorky) {
      gui.noWorky();
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseReleased(MouseEvent e) {
    BareBonesBrowserLaunch.openURL("http://myrobotlab.org");
  }

}