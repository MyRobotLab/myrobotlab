package org.myrobotlab.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class GoPro extends Service {
	
	transient public HttpClient http;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(GoPro.class);
	
	String cameraModel;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			GoPro gopro = (GoPro) Runtime.start("gopro", "GoPro");
			Runtime.start("gui", "GUIService");
			Runtime.start("python","Python");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public GoPro(String n) {
		super(n);
		http = (HttpClient) createPeer("http");
	}
	
	public void startService() {
		super.startService();
	    http = (HttpClient) startPeer("http");
		return;
	}
	
	public void setCameraModel(String model){
		cameraModel = model;
	}
	
	public void turnCameraOff(){
		if (cameraModel == "HERO4"){
		sendHttpGet("http://10.5.5.9/gp/gpControl/command/system/sleep");
		}
		else {
			System.out.println("Select your Camera Before");
		}
	}
	
	public void shutterOn(){
		if (cameraModel == "HERO4"){
		sendHttpGet("http://10.5.5.9/gp/gpControl/command/shutter?p=1");
		}
		else {
			System.out.println("Select your Camera Before");
		}
	}
	
	
	
	public void sendHttpGet(String url){
		try {
			String getResult = http.get(url);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This static method returns all the details of the class without
	 * it having to be constructed.  It has description, categories,
	 * dependencies, and peer definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData(){
		
		ServiceType meta = new ServiceType(GoPro.class.getCanonicalName());
		meta.addDescription("Service to control your GoPro over Wifi");
		// add dependency if necessary
		// meta.addDependency("org.coolproject", "1.0.0");
		meta.addCategory("general");
		meta.addPeer("http", "HttpClient", "Http for GoPro control");
		return meta;		
	}

	
}
