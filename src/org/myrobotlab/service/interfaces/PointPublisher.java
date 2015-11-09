package org.myrobotlab.service.interfaces;

import org.myrobotlab.kinematics.Point;

public interface PointPublisher {

	public String getName();

	public Point publishPoint();
	
}
