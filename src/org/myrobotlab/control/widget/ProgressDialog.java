package org.myrobotlab.control.widget;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.control.RuntimeGUI;
import org.myrobotlab.framework.repo.Updates;
import org.myrobotlab.image.Util;

/**
 * @author GroG class to handle the complex interaction for processing updates
 * 
 */
public class ProgressDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	// north
	JLabel actionText = null;

	// center
	private JTextArea reportArea = null;
	JScrollPane scrollPane = null;
	JLabel spinner = null;

	// south
	JLabel buttonText = new JLabel("");
	JButton ok = new JButton("ok");
	JButton cancel = new JButton("cancel");
	JButton restart = new JButton("restart");
	JButton noWorky = new JButton("noWorky!");

	boolean hasError = false;
	RuntimeGUI parent;

	public ProgressDialog(RuntimeGUI parent) {
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
		ok.addActionListener(this);
		cancel.addActionListener(this);
		restart.addActionListener(this);
		noWorky.addActionListener(this);
		hideButtons();
		south.add(ok);
		south.add(cancel);
		south.add(restart);
		south.add(noWorky);
		south.add(buttonText);
		// setPreferredSize(new Dimension(350, 300));
		setSize(320, 300);
	}

	public void hideButtons() {
		ok.setVisible(false);
		cancel.setVisible(false);
		restart.setVisible(false);
		noWorky.setVisible(false);
	}

	public void addInfo(String msg) {
		// data += "\n" + info;
		reportArea.append(String.format("%s\n", msg));
		// move caret???
		// reportArea.setText(data);
	}

	public void addErrorInfo(String error) {
		hasError = true;
		spinner.setIcon(Util.getImageIcon("error.png"));
		addInfo(error);
	}

	public void finished() {
		hideButtons();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (!hasError) {
					spinner.setIcon(Util.getImageIcon("success.png"));
					restart.setVisible(true);
				} else {
					noWorky.setVisible(true);
				}
				actionText.setText("finished");
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == noWorky) {
			parent.myService.noWorky();
		} else if (source == restart) {
			parent.restart();
		}
	}

	public void beginUpdates() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				hideButtons();
				hasError = false;
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
			public void run() {
				hideButtons();
				hasError = false;
				buttonText.setText("");
				reportArea.setText("");
				setVisible(true);
				actionText.setText("checking for updates");
				spinner.setIcon(new ImageIcon(ProgressDialog.class.getResource("/resource/progressBar.gif")));
			}
		});
	}

	public void publishUpdates(final Updates updates) {
		hideButtons();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				if (!updates.isValid) {
					spinner.setIcon(new ImageIcon(ProgressDialog.class.getResource("/resource/evilGnome.png")));
					actionText.setText("");
					buttonText.setText(String.format("<html>OMG ! I can't find the updates!<br/>I think it might be evil gnomes!<br/>%s</html>", updates.lastError));
					return;
				}

				if (updates.hasJarUpdate()) {
					buttonText.setText(String.format("<html>Version %s is available<br/>Would you like to update?</html>", updates.repoVersion));
					ok.setVisible(true);
					cancel.setVisible(true);
				} else {
					ok.setVisible(true);
					buttonText.setText("No new updates are available.");
				}
				// FIXME - enable cancel & ok button -> okUpdate button ?
				/*
				 * if (reply == JOptionPane.OK_OPTION) { // do (ALL updates)
				 * send("applyUpdates", updates); } else { // chose not to do
				 * updates log.info("user chose not to update"); }
				 */
			}
		});
	}

}
