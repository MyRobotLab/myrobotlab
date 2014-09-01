package org.myrobotlab.dynamicGUI;
import java.awt.Color;
import java.awt.Component;

public class DesktopCommandClicked implements Runnable{
  Component c;
  Thread runner=new Thread(this);
  
  public DesktopCommandClicked(Component c){
    this.c=c;
	runner.start();
  }

  public void run(){
    Color d=c.getBackground();
    if (d.equals(Color.red))c.setBackground(Color.blue);
	else c.setBackground(Color.red);
	for (int r=0;r<10;r++){
	  c.repaint();
	  try{runner.sleep(100);}
	  catch(Exception e){}
	}
	c.setBackground(d);
	c.repaint();
  }
  
}