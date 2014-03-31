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
		this.image = Util.getResourceIcon("RemoteAdapter/computer.png");
	}

	public String getTitle() {
		return uri.toString();
	}

	public ImageIcon getImage() {
		return image;
	}

	public String toString() {
		return uri.toString();
	}
}
