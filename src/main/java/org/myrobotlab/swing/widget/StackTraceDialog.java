package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.swing.RuntimeGui;
import org.slf4j.Logger;

public class StackTraceDialog extends JDialog implements ActionListener, MouseListener {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(StackTraceDialog.class);

  RuntimeGui parent;
  private JTextArea traceArea = null;
  private JButton refresh = null;

  JScrollPane scrollPane = null;

  public StackTraceDialog(RuntimeGui parent) {
    super(parent.swingGui.getFrame(), "stack traces");
    this.parent = parent;
    Container display = getContentPane();
    // north
    JPanel north = new JPanel();
    display.add(north, BorderLayout.NORTH);
    // TODO: create a better way to navigate the stack traces in a swing gui..
    traceArea = new JTextArea("Current Stack Traces\n", 5, 10);
    traceArea.setLineWrap(true);
    traceArea.setEditable(false);
    traceArea.setBackground(SystemColor.control);
    scrollPane = new JScrollPane(traceArea);
    DefaultCaret caret = (DefaultCaret) traceArea.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    // build our report to and add it to the text area
    refreshStackTraces();
    display.add(scrollPane, BorderLayout.CENTER);
    // south
    JPanel south = new JPanel();

    refresh = new JButton("Refresh");
    south.add(refresh);
    refresh.addActionListener(this);

    display.add(south, BorderLayout.SOUTH);
    // TODO: add a button to refresh..
    setSize(600, 600);
    setVisible(true);
  }

  private void refreshStackTraces() {
    // TODO: skip some of the stack traces.. for example the one that is
    // generating this result.
    Set<Thread> threads = Thread.getAllStackTraces().keySet();
    StringBuilder traceBuilder = new StringBuilder();
    for (Thread t : threads) {
      traceBuilder.append("Thread:" + t.getId() + " " + t.getName() + " Status:" + t.getState() + "\n");
      for (StackTraceElement ele : t.getStackTrace()) {
        traceBuilder.append("  " + ele.getClassName() + " " + ele.getFileName() + ":" + ele.getLineNumber() + "\n");
      }
      traceBuilder.append("\n");
    }
    traceArea.setText(traceBuilder.toString());
    traceArea.setCaretPosition(0);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseReleased(MouseEvent e) {
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
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    Object source = e.getSource();

    if (source == refresh) {
      refreshStackTraces();
    }

  }

}