package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.framework.Status;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.swing.RuntimeGui;
import org.slf4j.Logger;

/**
 * @author GroG class to handle the complex interaction for processing updates
 * 
 */
public class ProgressDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(ProgressDialog.class);

  // north
  JLabel actionText = null;

  // center
  private JTextArea reportArea = null;
  JScrollPane scrollPane = null;
  JLabel spinner = null;

  // south
  JLabel buttonText = new JLabel("");
  JButton okToUpdates = new JButton("ok");
  // JButton ok_update = new JButton("ok");
  JButton cancel = new JButton("cancel");
  JButton restart = new JButton("exit"); // FIXME - should be restart
  JButton noWorky = new JButton("noWorky!");

  ArrayList<Status> errors = new ArrayList<Status>();
  RuntimeGui parent;

  public ProgressDialog(RuntimeGui parent) {
    super(parent.myService.getFrame(), "new components");
    this.parent = parent;
    Container display = getContentPane();

    // north
    JPanel north = new JPanel();
    display.add(north, BorderLayout.NORTH);

    spinner = new JLabel();
    north.add(spinner);

    actionText = new JLabel("");
    north.add(actionText);

    // center
    reportArea = new JTextArea("details\n", 5, 10);
    reportArea.setLineWrap(true);
    reportArea.setEditable(false);
    reportArea.setBackground(SystemColor.control);

    scrollPane = new JScrollPane(reportArea);
    DefaultCaret caret = (DefaultCaret) reportArea.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

    display.add(scrollPane, BorderLayout.CENTER);

    // south
    JPanel south = new JPanel();
    display.add(south, BorderLayout.SOUTH);
    okToUpdates.addActionListener(this);
    cancel.addActionListener(this);
    restart.addActionListener(this);
    noWorky.addActionListener(this);
    hideButtons();
    south.add(okToUpdates);
    south.add(cancel);
    south.add(restart);
    south.add(noWorky);
    south.add(buttonText);
    // setPreferredSize(new Dimension(350, 300));
    setSize(320, 300);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object source = event.getSource();
    if (source == noWorky) {
      parent.myService.noWorky();
    } else if (source == restart) {
      parent.restart();
    } else if (source == cancel) {
      setVisible(false);
    } else {
      log.error("unknown source");
    }
  }

  public void addStatus(Status status) {
    reportArea.append(String.format("%s\n", status.detail));
    if (status.isError()) {
      errors.add(status);
      spinner.setIcon(Util.getImageIcon("error.png"));
    }
  }

  public void beginUpdates() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        hideButtons();
        errors.clear();
        buttonText.setText("");
        reportArea.setText("");
        setVisible(true);
        actionText.setText("downloading components");
        spinner.setIcon(new ImageIcon(ProgressDialog.class.getResource("/resource/progressBar.gif")));
      }
    });
  }

  public void checkingForUpdates() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        hideButtons();
        errors.clear();
        buttonText.setText("");
        reportArea.setText("");
        setVisible(true);
        actionText.setText("checking for updates");
        spinner.setIcon(new ImageIcon(ProgressDialog.class.getResource("/resource/progressBar.gif")));
      }
    });
  }

  public void finished() {
    hideButtons();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (errors.size() == 0) {
          spinner.setIcon(Util.getImageIcon("success.png"));
          restart.setVisible(true);
        } else {
          reportArea.append("ERRORS -----------\n");
          for (int i = 0; i < errors.size(); ++i) {
            reportArea.append(String.format("%s\n", errors.get(i).detail));
          }
          noWorky.setVisible(true);
        }
        actionText.setText("finished");
      }
    });
  }

  public void hideButtons() {
    okToUpdates.setVisible(false);
    cancel.setVisible(false);
    restart.setVisible(false);
    noWorky.setVisible(false);
  }

}
