package org.myrobotlab.opencv;

import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;

public class Overlay {

	public String text;
	public CvPoint pos;
	public CvFont font;
	public CvScalar color;
	
	public Overlay(String text, CvPoint pos, CvScalar color, CvFont font){
		this.text = text;
		this.pos = pos;
		this.font = font;
		this.color = color;
	}
	
}
