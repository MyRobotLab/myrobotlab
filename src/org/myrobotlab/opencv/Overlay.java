package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.CvFont;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;

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
