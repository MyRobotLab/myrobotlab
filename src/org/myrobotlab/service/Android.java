package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.android.MRL;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
public class Android extends Service implements SensorEventListener {

	private static final long serialVersionUID = 1L;
	private SensorManager sensorManager;
	private boolean color = false; 
	//private View view;
	private long lastUpdate;

	private Context context;
	public transient final static Logger log = LoggerFactory.getLogger(Android.class.getCanonicalName());
	

	public Android(String n) {
		super(n, Android.class.getCanonicalName());
		
		
		
		// add runtime notification - so we can manage
		// new Services starting or release or importing
		Runtime.getInstance().addListener(n, "registered", String.class);
		MRL.getInstance().addServiceActivityIntent(Runtime.getInstance().getName(),
				Runtime.getInstance().getSimpleName());

	}
	
	public String registered (String n)
	{
		log.info("got registered event " + n);
		return n;
	}
		
	public void setContext(Context context)
	{
		this.context = context;
	}
	
	public void startService()
	{
		super.startService();
	}
	
	public void startSensors()
	{
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		lastUpdate = System.currentTimeMillis();
	}
	
	public void stopService()
	{
		super.stopService();
		if (sensorManager != null)
		{
			sensorManager.unregisterListener(this);
		}
	}
	
	@Override
	public String getDescription() {
		return "used as a general android";
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float[] values = event.values;
			// Movement
			float x = values[0];
			float y = values[1];
			float z = values[2];

			float accelationSquareRoot = (x * x + y * y + z * z)
					/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
			long actualTime = System.currentTimeMillis();
			if (accelationSquareRoot >= 2) //
			{
				if (actualTime - lastUpdate < 200) {
					return;
				}
				lastUpdate = actualTime;
				/*
				Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT)
						.show();
				if (color) {
					view.setBackgroundColor(Color.GREEN);
					
				} else {
					view.setBackgroundColor(Color.RED);
				}
				*/
				color = !color;
				log.info("color " + color);
			}

		}
		
	}

	protected void onResume() {
		// register this class as a listener for the orientation and
		// accelerometer sensors
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onPause() {
		// unregister listener
		sensorManager.unregisterListener(this);
	}
	

	public boolean preProcessHook(Message m)
	{
		if (methodSet.contains(m.method))
		{
			return true;
		} 
		
		// if my current Activity UI equals my message sender
		// send the data
		if (m.sender.equals(MRL.currentServiceName))
		{
			Handler handler = MRL.handlers.get(m.sender);
			if (handler != null)
			{
				android.os.Message msg = handler.obtainMessage();
			    msg.what = 1;
			    msg.obj = m;
			    //msg.arg1 = index;
			    handler.sendMessage(msg);
			}
		    // TODO try handler.sendMessageAtFrontOfQueue(msg)
		    // TODO - make the myrobotlab.xml listview flash when a
		    // message goest to a specific Service
		}
		
		/*
		ServiceActivity ca = MRL.getCurrentActivity();// FIXME - DOES NOT WORK !!!
		if (ca != null && ca.getBoundServiceName().equals(m.sender))
		{
			MRL.handler.
			invoke(ca, m.method, m.data);
		}
		*/
		
		return false;
	}
	
	
	/**
	 * a general message function - is displayed on the AndroidActivity screen
	 * @param msg
	 * @return
	 */
	public String logMsg(String msg)
	{
		return msg;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		
		Android android = new Android("android");
		android.startService();
		
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		
		*/
		
	}
	
	@Override
	public String[] getCategories() {
		return new String[] {"sensor", "framework", "control"};
	}
	
}
