package org.myrobotlab.control.widget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class AboutDialog extends JDialog implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(AboutDialog.class);

	JButton noWorky = null;
	JButton ok = null;
	JFrame parent = null;
	JLabel versionLabel = new JLabel(org.myrobotlab.service.Runtime.getVersion());
	GUIService gui;

	public static void main(String[] args) throws Exception {
		LoggingFactory.getInstance().configure();

		log.info("[{}]", "1060M.20130227.0733".compareTo("1059M.20130227.0722"));
		log.info("[{}]", "1059M.20130227.0722".compareTo("1060M.20130227.0733"));

		// HTTPRequest logPoster = new HTTPRequest(new
		// URL("http://myrobotlab.org/myrobotlab_log/postLogFile.php"));
		HTTPRequest.postFile("http://myrobotlab.org/myrobotlab_log/postLogFile.php", "GroG", "file", new File("myrobotlab.log"));
		// logPoster.setParameter("file", "myrobotlab.log", new
		// FileInputStream(new File("myrobotlab.log")));
		// logPoster.setParameter("file", new File("myrobotlab.log"));
		// logPoster.setc
		/*
		 * InputStream in = logPoster.post().getInputStream(); //read it with
		 * BufferedReader BufferedReader br = new BufferedReader( new
		 * InputStreamReader(in));
		 * 
		 * StringBuilder sb = new StringBuilder();
		 * 
		 * String line; while ((line = br.readLine()) != null) {
		 * sb.append(line); }
		 * 
		 * System.out.println(sb.toString());
		 * 
		 * br.close();
		 */
	}

	public AboutDialog(GUIService gui) {
		super(gui.getFrame(), "about", true);
		this.gui = gui;
		this.parent = gui.getFrame();
		if (parent != null) {
			Dimension parentSize = parent.getSize();
			Point p = parent.getLocation();
			setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
		}

		JPanel content = new JPanel(new BorderLayout());
		content.setPreferredSize(new Dimension(350, 150));
		getContentPane().add(content);

		// picture
		JLabel pic = new JLabel();
		ImageIcon icon = Util.getResourceIcon("mrl_logo_about_128.png");
		if (icon != null) {
			pic.setIcon(icon);
		}
		content.add(pic, BorderLayout.WEST);

		JPanel center = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();

		JLabel link = new JLabel("<html><p align=center><a href=\"http://myrobotlab.org\">http://myrobotlab.org</a><html>");
		link.addMouseListener(this);
		content.add(center, BorderLayout.CENTER);
		content.add(versionLabel, BorderLayout.SOUTH);
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;
		center.add(link, gc);
		gc.gridwidth = 1;
		++gc.gridy;
		center.add(new JLabel("version "), gc);
		++gc.gridx;
		center.add(versionLabel, gc);

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