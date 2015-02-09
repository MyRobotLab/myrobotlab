package org.myrobotlab.kinematics;

import java.util.ArrayList;

public class DHRobotArm {

	private ArrayList<DHLink> links;
	
	public DHRobotArm() {
		super();
		links = new ArrayList<DHLink>();
	}

	public ArrayList<DHLink> addLink(DHLink link) {
		links.add(link);
		return links;
	}
	
	public ArrayList<DHLink> getLinks() {
		return links;
	}

	public void setLinks(ArrayList<DHLink> links) {
		this.links = links;
	}

	public Point getPalmPosition() {
		// TODO Auto-generated method stub
		// return the position of the end effector wrt the base frame
		Matrix m = new Matrix(4,4);
		// TODO: init to the ident?
		m.elements[0][0] = 1;
		m.elements[1][1] = 1;
		m.elements[2][2] = 1;
		m.elements[3][3] = 1;
		
		// TODO: validate this approach..
		for (DHLink link : links) {
			Matrix s = link.resolveMatrix();
			//System.out.println(s);
			m = m.multiply(s);			
		}
		
		// now m should be the total translation for the arm 
		// given the arms current position
		
		double x = m.elements[0][3];
		double y = m.elements[1][3];
		double z = m.elements[2][3];
		
		//double ws = m.elements[3][3];
		//System.out.println("World Scale : " + ws);
		
		Point palm = new Point(x,y,z);
		
		return palm;
	}
	
	
}
