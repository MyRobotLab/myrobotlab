package org.myrobotlab.control.widget;

import java.net.URI;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;
import org.myrobotlab.net.CommData;

public class CommunicationNodeEntry extends JPanel {

	private static final long serialVersionUID = 1L;

	URI uri;
	CommData data;
	ImageIcon image;

	public CommunicationNodeEntry(URI uri, CommData data) {
		this.uri = uri;
		this.data = data;
		this.image = Util.getResourceIcon("instance.png");
	}

	public String getTitle() {
		return String.format("<html>%s %s<br/>RX %s.%s %d<br/>TX %s.%s %d</html>", uri, data.state, data.rxName, data.rxMethod, data.rx, data.txName, data.txMethod, data.tx);// data.toString();//String.format("%s connected rx %d tx %d         ", uri.toString(), data.rx, data.tx);
	}

	public ImageIcon getImage() {
		return image;
	}

	public String toString() {
		return uri.toString();
	}
}
