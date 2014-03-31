package org.myrobotlab.control.widget;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CommunicationNodeRenderer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;
	private static final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

	public CommunicationNodeRenderer() {
		setOpaque(true);
		setIconTextGap(12);
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		CommunicationNodeEntry entry = (CommunicationNodeEntry) value;
		setText(entry.getTitle());
		setIcon(entry.getImage());
		if (isSelected) {
			setBackground(HIGHLIGHT_COLOR);
			setForeground(Color.white);
		} else {
			setBackground(Color.white);
			setForeground(Color.black);
		}
		return this;
	}

}
