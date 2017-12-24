package org.myrobotlab.swing.widget;

public class DockableTabData {
	String title;
	int x;
	int y;
	int width;
	int height;
	// FYI - isShowing is a element on the ui - but you probably need to replicated this for json
	boolean isHidden = false; 
}
