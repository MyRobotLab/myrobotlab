package org.myrobotlab.control.widget;

import java.awt.BorderLayout;
import java.awt.SystemColor;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.image.Util;

public class ProgressDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private String data = "";
	private JTextArea reportArea = null;
	JScrollPane scrollPane = null;
	JLabel spinner = null;
	JLabel downloadText = null;
	boolean hasError = false;

	public ProgressDialog(JFrame frame) {
		super(frame, "new components");

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.NORTH);

		spinner = new JLabel();
		panel.add(spinner);
		spinner.setIcon(new ImageIcon(ProgressDialog.class.getResource("/resource/progressBar.gif")));

		downloadText = new JLabel("Downloading new components");
		panel.add(downloadText);

		reportArea = new JTextArea("details", 5, 10);
		reportArea.setLineWrap(true);
		reportArea.setEditable(false);
		reportArea.setBackground(SystemColor.control);

		scrollPane = new JScrollPane(reportArea);
		DefaultCaret caret = (DefaultCaret) reportArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		getContentPane().add(scrollPane, BorderLayout.CENTER);
		setSize(320, 240);
		setLocationRelativeTo(frame);
	}

	public void addInfo(String info) {
		data += "\n" + info;
		reportArea.setText(data);
	}

	public void addErrorInfo(String error) {
		hasError = true;
		spinner.setIcon(Util.getImageIcon("error.png"));
		addInfo(error);
	}

	public void finished() {
		if (!hasError) {
			spinner.setIcon(Util.getImageIcon("success.png"));
		}
		downloadText.setText("finished");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
