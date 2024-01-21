package org.myrobotlab.caliko;

import org.myrobotlab.service.Caliko;

import au.edu.federation.caliko.demo3d.CalikoDemoStructure3D;
import au.edu.federation.utils.Mat4f;

public class GuiDemoStructure extends CalikoDemoStructure3D {
  
  private transient Caliko service;
  protected String name;
  
  public GuiDemoStructure(Caliko service, String name) {
    this.service = service;
    this.name = name;
  }

  @Override
  public void setup() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void drawTarget(Mat4f mvpMatrix) {
    // TODO Auto-generated method stub
    
  }

}
