package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.LeapMotion2.Hand;
import org.myrobotlab.service.LeapMotion2.LeapData;
import org.myrobotlab.service.data.OculusData;
import org.myrobotlab.service.interfaces.OculusDataListener;
import org.myrobotlab.service.interfaces.OculusDataPublisher;
import org.slf4j.Logger;

import com.leapmotion.leap.Frame;

public class OculusDIY extends Service implements OculusDataPublisher , OculusDataListener{

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OculusDIY.class);

	Integer lastValue = 30;
	Integer resetValue = 30;
	Integer head = 90;
	Integer rothead = 90;
	Integer offSet = 0;
	
	
	public OculusDIY(String n) {
		super(n);
	}
	

	@Override
	public String getDescription() {
		return "Service to receive and compute data from a DIY Oculus";
	}
	
	public void onCustomMsg(Integer ay, Integer ax2 , Integer headingint){
		//Integer ay = (Integer) data[0];
		//Integer ax2 = (Integer) data[0];
		//Integer headingint = (Integer) data[0];
		this.computeAngles(ax2,headingint);
		OculusData oculus = new OculusData();
		oculus.yaw = rothead;
		oculus.pitch = head;
		this.publishOculusData(oculus);
		
		System.out.println(head+","+rothead);
		
	}
	
	public void calibrate(){
		resetValue = lastValue;
	    offSet = (90 - lastValue);
	}

	public void computeAngles(Integer ax2, Integer headingint){
		
		head = (20 +(((ax2 - 250)/(-250 -250))*(160-20)));
		lastValue = headingint;
		if (resetValue > 90 && lastValue <0){ 
		  rothead = (offSet + headingint + 360);
		  }
		else if (resetValue < -90 && lastValue >0){
		  rothead = (offSet + headingint - 360);
		  }
		else{
		  rothead = (offSet + headingint);
		  }}
		
		  
    public OculusData publishOculusData(OculusData oculus){
    	    return oculus;
    	
           }
    
    public void addOculusDataListener(Service service){
    	addListener("publishOculusData", service.getName(), "onOculusData", Frame.class);
    }
    
    public OculusData onOculusData(OculusData oculus){
    	   return oculus;
    }
    
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			OculusDIY oculus = (OculusDIY)Runtime.start("oculus", "OculusDIY");
			oculus.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
