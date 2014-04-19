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
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import javax.swing.JTabbedPane;

import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class AboutDialog extends JDialog implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(AboutDialog.class.getCanonicalName());

	JButton bleedingEdge = null;
	JButton noWorky = null;
	JButton ok = null;
	JFrame parent = null;
	JLabel versionLabel = new JLabel(FileIO.resourceToString("version.txt"));

	public AboutDialog(JFrame parent) {
		super(parent, "about", true);
		this.parent = parent;
		if (parent != null) {
			Dimension parentSize = parent.getSize();
			Point p = parent.getLocation();
			setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
		}

		JPanel content = new JPanel(new BorderLayout());
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

		bleedingEdge = new JButton("I feel lucky, give me the bleeding edge !");
		buttonPane.add(bleedingEdge);
		bleedingEdge.addActionListener(this);

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
		} else if (source == bleedingEdge) {
			String newVersion = Runtime.getBleedingEdgeVersionString();
			String currentVersion = FileIO.resourceToString("version.txt");
			log.info(String.format("comparing new version %s with current version %s", newVersion, currentVersion));
			if (newVersion == null)
			{
				log.info("newVersion == null - nothing available");
				JOptionPane.showMessageDialog(parent, "There are no updates available at this time");
			} else if (currentVersion.compareTo(newVersion) >= 0) {
				log.info("equals or old bleeding - no updates - currentVersion.compareTo(newVersion) = {}", currentVersion.compareTo(newVersion));
				JOptionPane.showMessageDialog(parent, "There are no new updates available at this time");
			} else {
				log.info("not equals - offer update");
				// Custom button text
				Object[] options = { "Yes, hit me daddy-O!", "No way, I'm scared" };
				int n = JOptionPane.showOptionDialog(parent, String.format("A fresh new version is ready, do you want this one? %s", newVersion), "Bleeding Edge Check",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (n == JOptionPane.YES_OPTION) {
					Runtime.getBleedingEdgeMyRobotLabJar();
					versionLabel.setText(String.format("updating with %s", newVersion));
					GUIService.restart("moveUpdate");
				} else {
					versionLabel.setText("bwak bwak bwak... chicken!");
				}

			}
		} else if (source == noWorky) {
			String logon = JOptionPane.showInputDialog(parent, "<html>This will send your myrobotlab.log file<br><p align=center>to our crack team of monkeys,</p><br> please type your myrobotlab.org user</html>");
			if (logon == null || logon.length() == 0) {
				return;
			}

			try {
				String ret = HTTPRequest.postFile("http://myrobotlab.org/myrobotlab_log/postLogFile.php", logon, "file", new File("myrobotlab.log"));
				if (ret.contains("Upload:")) {
					JOptionPane.showMessageDialog(parent, "log file sent, Thank you", "Sent !", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(parent, ret, "DOH !", JOptionPane.ERROR_MESSAGE);
				}
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(parent, Service.stackToString(e1), "DOH !", JOptionPane.ERROR_MESSAGE);
			}

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

	public static void main(String[] args) throws Exception {
		LoggingFactory.getInstance().configure();

		log.info("[{}]","1060M.20130227.0733".compareTo("1059M.20130227.0722"));
		log.info("[{}]","1059M.20130227.0722".compareTo("1060M.20130227.0733"));
		
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

}