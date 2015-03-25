package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

public interface TabControlEventHandler {

	public void actionPerformed(ActionEvent e, String tabName);

	public void dockPanel();

	public void mouseClicked(MouseEvent event, String tabName);

	public void undockPanel();

}
