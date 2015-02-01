/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.PinState;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 *  Implementation of a Arduino Service connected to MRL through a serial port.  
 *  The protocol is basically a pass through of system calls to the Arduino board.  Data 
 *  can be passed back from the digital or analog ports by request to start polling. The
 *  serial port can be wireless (bluetooth), rf, or wired. The communication protocol
 *  supported is in arduinoSerial.pde - located here :
 *  
 *	Should support nearly all Arduino board types  
 *   
 *   References:
 *    <a href="http://www.arduino.cc/playground/Main/RotaryEncoders">Rotary Encoders</a> 
 *   @author GroG
 */

@Root
public class ArduinoBT extends Service implements //SerialPortEventListener,
		SensorDataPublisher, DigitalIO, AnalogIO, ServoController, MotorController {
	
	public transient final static Logger log = LoggerFactory.getLogger(ArduinoBT.class.getCanonicalName());
	private static final long serialVersionUID = 1L;
	
	// debugging
    private static final String TAG = "ArduinoBT";
    private static final boolean D = true;

	private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Member fields
    public BluetoothAdapter btAdapter;
    private volatile Handler mHandler;

    private int state;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // serial connection details
	boolean rawReadMsg = false;
	int rawReadMsgLength = 4;

	// FIXME methods - is this a good idea ??? should this be generated like 
	// android R file ???
	public static final String digitalWrite = "digitalWrite";
	public static final String pinMode = "pinMode";
	public static final String analogWrite = "analogWrite";
	public static final String analogReadPollingStart = "analogReadPollingStart";
	public static final String analogReadPollingStop = "analogReadPollingStop";
	
    BluetoothAdapter adapter = null;
    // Name of the connected device
	
    String deviceName = null;
  
    private ConnectThread connectThread = null;
    private ConnectedThread connectedThread = null;
 
	
	int baudRate = 115200;
	
	int dataBits = 8;
	
	int parity = 0;
	
	int stopBits = 1;
	
	// FIXME imported Arduino constants FIXME - NORMLIZE / GLOBALIZE
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int TCCR0B = 0x25; // register for pins 6,7
	public static final int TCCR1B = 0x2E; // register for pins 9,10
	public static final int TCCR2B = 0xA1; // register for pins 3,11

	// serial protocol functions
	public static final int DIGITAL_WRITE 				= 0;
	public static final int ANALOG_WRITE 				= 2;
	public static final int ANALOG_VALUE 				= 3;
	public static final int PINMODE 					= 4;
	public static final int PULSE_IN 					= 5;
	public static final int SERVO_ATTACH 				= 6;
	public static final int SERVO_WRITE 				= 7;
	public static final int SERVO_SET_MAX_PULSE 		= 8;
	public static final int SERVO_DETACH 				= 9;
	public static final int SET_PWM_FREQUENCY 			= 11;
	public static final int SERVO_READ 					= 12;
	public static final int ANALOG_READ_POLLING_START 	= 13;
	public static final int ANALOG_READ_POLLING_STOP 	= 14;
	public static final int DIGITAL_READ_POLLING_START 	= 15;
	public static final int DIGITAL_READ_POLLING_STOP 	= 16;
	public static final int SET_ANALOG_PIN_SENSITIVITY 	= 17;
	public static final int SET_ANALOG_PIN_GAIN 		= 18;

	// servo related
	public static final int SERVO_ANGLE_MIN 	= 0;
	public static final int SERVO_ANGLE_MAX 	= 180;
	public static final int SERVO_SWEEP 		= 10;
	public static final int MAX_SERVOS 			= 8; 

	// servos
	boolean[] servosInUse = new boolean[MAX_SERVOS - 1];
	HashMap<Integer, Integer> pinToServo = new HashMap<Integer, Integer>(); 
	HashMap<Integer, Integer> servoToPin = new HashMap<Integer, Integer>(); 
			
	
	public HashMap<Integer, PinState> pins = new HashMap<Integer, PinState>(); 
	
	/**
	 *  list of serial port names from the system which the Arduino service is 
	 *  running
	 */
	public ArrayList<String> portNames = new ArrayList<String>(); 
	
	public ArduinoBT(String n) {
		super(n, ArduinoBT.class.getCanonicalName());

		load(); // attempt to load config
		
		// get ports - return array of strings
		// set port? / init port
		// detach port
		
		// populate the pins
		for (int i = 0; i < 20; ++i) // + 6 analogs
		{   
			PinState p = new PinState();
			p.value = 0; // FIXME - if you set it here - you should initialize the board to 0
			p.address = i;
			if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11)
			{
				// pwm pins
				p.type = PinState.ANALOGDIGITAL;
				pins.put(i, p);
			} else if (i > 13) {
				p.type = PinState.ANALOG;
				pins.put(i, p);				
			} else {
				p.type = PinState.DIGITAL;
				pins.put(i, p);				
			}
		}
		
		// FIXME distill Arduino out of BT & GNU IO
		adapter = BluetoothAdapter.getDefaultAdapter();
		state = STATE_NONE;

		for (int i = 0; i < servosInUse.length; ++i) {
			servosInUse[i] = false;
		}

	}
	
	public PinState getPinState(int pinNum)
	{
		return pins.get(pinNum);		
	}
	
	/**
	 * @return the current serials port name or null if not opened
	 */
	public String getDeviceName() //FIXME - BT MAC address or BT Name ???
	{
		return deviceName;
	}
	



	/**
	 * serialSend communicate to the arduino using our simple language 3 bytes 3
	 * byte functions - |function name| d0 | d1
	 * 
	 * if outputStream is null: Important note to Fedora 13 make sure
	 * /var/lock/uucp /var/spool/uucp /var/spool/uucppublic and all are chown'd
	 * by uucp:uucp
	 */
	public synchronized void serialSend(int function, int param1, int param2) {
		log.info("serialSend fn " + function + " p1 " + param1 + " p2 "
				+ param2);
		// 3 byte Arduino Protocol
		byte data[] = new byte[3];
		data[0] = (byte)function;
		data[1] = (byte)param1;
		data[2] = (byte)param2;

		if (connectedThread != null)
		{
			connectedThread.write(data);
		} else {
			log.error("currently not connected"); // FIXME at some point use a Service logger interface
		}
	}

	@ToolTip("sends an array of data to the serial port which an Arduino is attached to")
	public void serialSend(String data) {
		log.error("serialSend [" + data + "]");
		serialSend(data.getBytes());
	}

	public synchronized void serialSend(byte[] data) {
		connectedThread.write(data);
	}

	public void setPWMFrequency(IOData io) {
		int freq = io.value;
		int prescalarValue = 0;

		switch (freq) {
		case 31:
		case 62:
			prescalarValue = 0x05;
			break;
		case 125:
		case 250:
			prescalarValue = 0x04;
			break;
		case 500:
		case 1000:
			prescalarValue = 0x03;
			break;
		case 4000:
		case 8000:
			prescalarValue = 0x02;
			break;
		case 32000:
		case 64000:
			prescalarValue = 0x01;
			break;
		default:
			prescalarValue = 0x03;
		}

		serialSend(SET_PWM_FREQUENCY, io.address, prescalarValue);
	}


	/*
	 * Servo Commands Arduino has a concept of a software Servo - and supports
	 * arrays Although Services could talk directly to the Arduino software
	 * servo in order to control the hardware the Servo service was created to
	 * store/handle the details, provide a common interface for other services
	 * regardless of the controller (Arduino in this case but could be any
	 * uController)
	 */
	
	// ---------------------------- Servo Methods Begin -----------------------
	
	/* servoAttach
	 * attach a servo to a pin
	 * @see org.myrobotlab.service.interfaces.ServoController#servoAttach(java.lang.Integer)
	 */
	public boolean servoAttach(Integer pin) { 
		
		if (deviceName == null) {
			log.error("could not attach servo to pin " + pin
					+ " serial port in null - not initialized?");
			return false;
		}
		// deviceName == null ??? make sure you chown it correctly !
		log.info("servoAttach (" + pin + ") to " + deviceName
				+ " function number " + SERVO_ATTACH);

		/*
		 * soft servo if (pin != 3 && pin != 5 && pin != 6 && pin != 9 && pin !=
		 * 10 && pin != 11) { log.error(pin + " not valid for servo"); }
		 */

		for (int i = 0; i < servosInUse.length; ++i) {
			if (!servosInUse[i]) {
				servosInUse[i] = true;
				pinToServo.put(pin, i);
				servoToPin.put(i, pin);
				serialSend(SERVO_ATTACH, pinToServo.get(pin), pin);
				return true;
			}
		}

		log.error("servo " + pin + " attach failed - no idle servos");
		return false;
	}

	public boolean servoDetach(Integer pin) {
		log.info("servoDetach (" + pin + ") to " + deviceName
				+ " function number " + SERVO_DETACH);

		if (pinToServo.containsKey(pin)) {
			int removeIdx = pinToServo.get(pin);
			serialSend(SERVO_DETACH, pinToServo.get(pin), 0);
			servosInUse[removeIdx] = false;

			return true;
		}

		log.error("servo " + pin + " detach failed - not found");
		return false;

	}

	/*
	 * servoWrite(IOData io) interface that allows routing with a single
	 * parameter TODO - how to "route" to multiple parameters
	 */
	public void servoWrite(IOData io) {
		servoWrite(io.address, io.value);
	}

	// Set the angle of the servo in degrees, 0 to 180.
	// @Override - TODO - make interface - implements ServoController interface
	public void servoWrite(Integer pin, Integer angle) {
		if (deviceName == null) // TODO - remove this only for debugging without
		// Arduino
		{
			return;
		}

		log.info("servoWrite (" + pin + "," + angle + ") to "
				+ deviceName + " function number " + SERVO_WRITE);

		if (angle < SERVO_ANGLE_MIN || angle > SERVO_ANGLE_MAX) {
			// log.error(pin + " angle " + angle + " request invalid");
			return;
		}

		serialSend(SERVO_WRITE, pinToServo.get(pin), angle);

	}

	// ---------------------------- Servo Methods End -----------------------
	
	// ---------------------- Serial Control Methods Begin ------------------

	public void releaseSerialPort() {
		log.debug("releaseSerialPort");
		stop();
	    log.info("released port");
	}


	public String getDeviceString()
	{
		return adapter.getName();
	}
	
	public boolean setBaud(int baudRate)
	{
		if (deviceName == null)
		{
			log.error("setBaudBase - deviceName is null");
			return false;
		}
		try {
			// boolean ret = deviceName.set.setBaudBase(baudRate); // doesnt work - operation not allowed
			// boolean ret = setSerialPortParams(baudRate, deviceName.getDataBits(), deviceName.getStopBits(), deviceName.getParity());
			boolean ret = false;
			this.baudRate = baudRate;
			save();
			broadcastState(); // state has changed let everyone know
			return ret;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}
	
	public int getBaudRate()
	{
		return baudRate;
	}
	
	public boolean setSerialPortParams (int baudRate, int dataBits, int stopBits, int parity)
	{
		if (deviceName == null)
		{
			log.error("setSerialPortParams - deviceName is null");
			return false;
		}
		
		/*
		try {
			deviceName.setSerialPortParams(baudRate, dataBits, stopBits, parity);
		} catch (UnsupportedCommOperationException e) {
			Logging.logException(e);
		}
		*/
		
		return true;
	}
	
	public void digitalReadPollStart(Integer address) {

		log.info("digitalRead (" + address + ") to " + deviceName);
		serialSend(DIGITAL_READ_POLLING_START, address, 0);

	}
	// ---------------------- Serial Control Methods End ------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalReadPollStop(Integer address) {
		log.info("digitalRead (" + address + ") to " + deviceName);
		serialSend(DIGITAL_READ_POLLING_STOP, address, 0);
	}

	public IOData digitalWrite(IOData io) {
		serialSend(DIGITAL_WRITE, io.address, io.value);
		return io;
	}

	public void pinMode(Integer address, Integer value) {
		pins.get(address).mode = value;
		serialSend(PINMODE, address, value);
	}

	public IOData analogWrite(IOData io) {
		serialSend(ANALOG_WRITE, io.address, io.value);
		return io;
	}

	public Pin publishPin(Pin p) {
		log.debug("pin {}",p);
		return p;
	}

	public String readSerialMessage(String s) {
		return s;
	}

	public void setRawReadMsg(Boolean b) {
		rawReadMsg = b;
	}

	public void setReadMsgLength(Integer length) {
		rawReadMsgLength = length;
	}


	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin) {
		serialSend(DIGITAL_READ_POLLING_START, pin, 0); // last param is not
		// used in read
	}

	public void digitalReadPollingStop(Integer pin) {
		serialSend(DIGITAL_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	// force an analog read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void analogReadPollingStart(Integer pin) {
		serialSend(ANALOG_READ_POLLING_START, pin, 0); // last param is not used
		// in read
	}

	public void analogReadPollingStop(Integer pin) {
		serialSend(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	/*
	 * Another means of distributing the data would be to publish to individual
	 * functions which might be useful for some reason in the future - initially
	 * this was started because of the overlap on the Arduino board where the
	 * analog pin addresses overlapped the digital vs 14 - 19 analog pins they
	 * are addressed 0 - 1 with analog reads
	 */

	class MotorData {
		boolean isAttached = false;
	}
	
	HashMap<String, MotorData> motorMap = new HashMap<String, MotorData>();

	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it
	public void motorAttach(String name, Integer PWMPin, Integer DIRPin) {
		// set the pinmodes on the 2 pins
		if (deviceName != null) {
			pinMode(PWMPin, PinState.OUTPUT);
			pinMode(DIRPin, PinState.OUTPUT);
		} else {
			log.error("attempting to attach motor before serial connection to "
					+ name + " Arduino is ready");
		}

	}

	public boolean motorDetach(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	public void motorMove(String name, Integer amount) {
		// TODO Auto-generated method stub

	}

	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDescription() {
		return "<html>Arduino is a service which interfaces with an Arduino micro-controller.<br>"
				+ "This interface can operate over radio, IR, or other communications,<br>"
				+ "but and appropriate .PDE file must be loaded into the micro-controller.<br>"
				+ "See http://myrobotlab.org/communication for details";
	}

	public void stopService() {
		super.stopService();
		releaseSerialPort();
	}

	public Vector<Integer> getOutputPins()
	{
		// TODO - base on "type"
		Vector<Integer> ret = new Vector<Integer>();
		for (int i = 2; i < 13; ++i )
		{
			ret.add(i);
		}
		return ret;
	}
	
	   /**
     * Return the current connection state. */
    public synchronized int getBTState() {
        return state;
    }

    /**
     * Gleaned from Google's API Bluetooth Demo
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket, socketType, this);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        deviceName = device.getName();
        
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void write(int function, int param1, int param2) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(new byte[]{(byte)function, (byte)param1, (byte)param2});
    }

    
    /**
     * Indicate that the connection attempt failed and addListener the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        deviceName = null;
        // Start the service over to restart listening mode
        //BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and addListener the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        deviceName = null;
        // Start the service over to restart listening mode
        //BluetoothChatService.this.start();
    }



    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice - try 1 instead of MY_UUID_SECURE or Insecure
            try {
            	// Hint: If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
            	// REF : 
            	// http://stackoverflow.com/questions/5308373/how-to-create-insecure-rfcomm-socket-in-android
            	// http://stackoverflow.com/questions/5263144/bluetooth-spp-between-android-and-other-device-uuid-and-pin-questions
            	
                tmp = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                Log.e(TAG, "tmp = " + tmp);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                deviceName = null;
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ArduinoBT.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream inputStream;
        private final OutputStream mmOutStream;
        private final Service myService;

        public ConnectedThread(BluetoothSocket socket, String socketType, Service myService) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
        	this.myService = myService;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
    				byte[] msg = new byte[rawReadMsgLength];
    				int newByte;
    				int numBytes = 0;
    				
    				// FIXME - should be inputStream.read(msg) - for an
    				// attempted 4 byte read - and if < 4 bytes read the diff
    				while ((newByte = inputStream.read()) >= 0) {
    					msg[numBytes] = (byte) newByte;
    					++numBytes;

    					if (numBytes == rawReadMsgLength) {
    						if (rawReadMsg) {

    							String s = new String(msg);
    							log.info(s);
    							invoke("readSerialMessage", s);
    						} else {

    							// mrl protocol

    							Pin p = new Pin();
    							p.type = msg[0];
    							p.pin = msg[1];
    							// java assumes signed
    							// MSB - (Arduino int is 2 bytes)
    							p.value = (msg[2] & 0xFF) << 8; 
    							p.value += (msg[3] & 0xFF); // LSB

    							p.source = myService.getName();
    							invoke(SensorDataPublisher.publishPin, p);
        						// Send the obtained bytes to the UI Activity
    							/*
        	                    mHandler.obtainMessage(MESSAGE_READ, numBytes, -1, buffer)
        	                            .sendToTarget();
        	                            */
    						}

    						numBytes = 0;

    						// reset buffer
    						for (int i = 0; i < rawReadMsgLength; ++i) {
    							msg[i] = -1;
    						}

    					}
    				}
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public Handler getmHandler() {
		return mHandler;
	}

	public void setmHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}
	
	@Override // FIXME - normalize - and build only when change types
	public ArrayList<Pin> getPinList() {
		ArrayList<Pin> pinList = new ArrayList<Pin>();
		//String type = Preferences2.get("board");
		String type = "atmega328";

		if ("mega2560".equals(type))
		{
			for (int i = 0; i < 70; ++i) 
			{
				pinList.add(new Pin(i, ((i < 54)?Pin.DIGITAL_VALUE:Pin.ANALOG_VALUE), 0, getName()));
			}
		} else if ("atmega328".equals(type))
		{
			for (int i = 0; i < 20; ++i) 
			{
				pinList.add(new Pin(i, ((i < 14)?Pin.DIGITAL_VALUE:Pin.ANALOG_VALUE), 0, getName()));
			}
			
		} else {
			log.error(String.format("getPinList %s not supported", type));
		}

		return pinList;
	}

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void motorMove(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean servoAttach(String servoName, Integer pin) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void servoWrite(String name, Integer newPos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean servoDetach(String servoName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Integer getServoPin(String servoName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getMotorData(String motorName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setServoSpeed(String servoName, Float speed) {
		// TODO Auto-generated method stub
		
	}

}
