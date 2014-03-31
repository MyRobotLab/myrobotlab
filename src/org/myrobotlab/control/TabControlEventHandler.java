package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;

public interface TabControlEventHandler {
	
	public void dockPanel();
	public void undockPanel();
	public void mouseClicked(MouseEvent event, String tabName);
	public void actionPerformed(ActionEvent e, String tabName);

}
