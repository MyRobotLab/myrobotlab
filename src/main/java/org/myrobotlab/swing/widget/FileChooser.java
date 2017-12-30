package org.myrobotlab.swing.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Keyboard;
import org.slf4j.Logger;

public class FileChooser extends JButton implements ActionListener {
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Keyboard.class);

	JFileChooser chooser;
	JTextField field;

	public FileChooser(String label, JTextField field) {
		super(label);
		this.addActionListener(this);
		this.field = field;
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));

	}
	
	// FIXME - filterExt(String extention)
	public void filterDirsOnly(){
		chooser.setDialogTitle("choose directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);		
		chooser.setAcceptAllFileFilterUsed(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (field.getText() != null) {
			chooser.setCurrentDirectory(new File(field.getText()));
		}

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			log.info("getCurrentDirectory(): " + chooser.getCurrentDirectory());
			log.info("getSelectedFile() : " + chooser.getSelectedFile());

			field.setText(chooser.getSelectedFile().getAbsolutePath());

		} else {
			log.info("No Selection ");
		}
	}

	/*
	public Dimension getPreferredSize() {
		return new Dimension(200, 200);
	}
	*/

	public static void main(String s[]) {
		JFrame frame = new JFrame("");

		JTextField field = new JTextField(40);
		
		FileChooser chooser = new FileChooser("test", field);
		JPanel panel = new JPanel();
		panel.add(field);
		panel.add(chooser);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.getContentPane().add(panel, "Center");
		frame.setSize(panel.getPreferredSize());
		frame.setVisible(true);
	}
}