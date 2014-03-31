package org.myrobotlab.control.widget;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.myrobotlab.service.GUIService;

public class ConnectDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	public JTextField host = new JTextField("127.0.0.1", 10);
	public JTextField port = new JTextField("6767", 10);
	JButton connect = new JButton("connect");
	JButton cancel = new JButton("cancel");
	GUIService myService;
	
	public final String CANCELED = "canceled";
	
	public String state;

	public ConnectDialog(JFrame parent, String title, String message, GUIService myService, String defaultIP, String defaultPort) {
		super(parent, title, true);
		this.myService = myService;
		if (parent != null) {
			Dimension parentSize = parent.getSize();
			Point p = parent.getLocation();
			setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
		}

		if (defaultIP != null) {
			host = new JTextField(defaultIP, 10);
		}
		if (defaultPort != null) {
			port = new JTextField(defaultPort, 10);
		}

		// TODO - refactor - i hate grid bag
		
		GridBagConstraints gc = new GridBagConstraints();
		JPanel connectInfo = new JPanel();
		connectInfo.setLayout(new GridBagLayout());
		gc.gridx = 0;
		gc.gridy = 0;
		connectInfo.add(new JLabel("host  "), gc);
		++gc.gridx;
		connectInfo.add(host, gc);
		gc.gridx = 0;
		++gc.gridy;
		connectInfo.add(new JLabel("port  "), gc);
		++gc.gridx;
		connectInfo.add(port, gc);
		++gc.gridy;
		connectInfo.add(connect, gc);
		connect.addActionListener(this);
		++gc.gridx;
		connectInfo.add(cancel, gc);
		cancel.addActionListener(this);
		getContentPane().add(connectInfo);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ConnectDialog dlg = new ConnectDialog(new JFrame(), "title", "message", null, "10.11.123.24", "6767");

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("connect".endsWith(e.getActionCommand())) {
			// FIXME - refactor name
			myService.connect(null, null, host.getText(), Integer.parseInt(port.getText()));
		}
		setVisible(false);
		dispose();
	}

}
