package org.myrobotlab.openni;

import org.simpleframework.xml.Element;

public class Skeleton {
	
	public int frameNumber;
	public int userId;
	
	@Element
	public PVector centerOfMass = new PVector();
	
	@Element
	public PVector head = new PVector();
	
	@Element
	public PVector rightShoulder = new PVector();
	@Element
	public PVector neck = new PVector();
	@Element
	public PVector leftShoulder = new PVector();
	
	@Element
	public PVector torso = new PVector();

	@Element
	public PVector leftElbow = new PVector();
	@Element
	public PVector leftHand = new PVector();
	
	@Element
	public PVector rightElbow = new PVector();
	@Element
	public PVector rightHand = new PVector();
	
	@Element
	public PVector rightHip = new PVector();
	@Element
	public PVector rightKnee = new PVector();
	@Element
	public PVector rightFoot = new PVector();
	
	@Element
	public PVector leftHip = new PVector();
	@Element
	public PVector leftKnee = new PVector();
	@Element
	public PVector leftFoot = new PVector();
		
}
