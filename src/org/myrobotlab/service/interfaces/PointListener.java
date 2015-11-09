package org.myrobotlab.service.interfaces;

import org.myrobotlab.kinematics.Point;

public interface PointListener {

	public String getName();
	
	public void onPoint(Point p);
	
}
