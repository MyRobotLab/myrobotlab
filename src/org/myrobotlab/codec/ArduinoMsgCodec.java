package org.myrobotlab.codec;

import java.util.HashMap;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino2;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Serial;
import org.slf4j.Logger;

// FIXME - use InputStream OutputStream 
// Stream encoders are more complicated than Document 
// with InputStream decoding - you need to deal with blocking / timeouts etc
// if the thing before it deals with it then you have a byte array - but it may not be complete

public class ArduinoMsgCodec implements Codec {

	public final static Logger log = LoggerFactory.getLogger(ArduinoMsgCodec.class);
	
	transient static final HashMap<Integer,String> byteToMethod = new HashMap<Integer,String>();
	transient static final HashMap<String,Integer> methodToByte = new HashMap<String,Integer>();
	int byteCount = 0;
	int msgSize = 0;
	StringBuilder rest = new StringBuilder();
	
	
	static {
		/////// JAVA GENERATED DEFINITION BEGIN //////
		/////// java ByteToMethod generated definition - DO NOT MODIFY - Begin //////
	// {analogReadPollingStart class java.lang.Integer} 
	byteToMethod.put(2,"analogReadPollingStart");
	methodToByte.put("analogReadPollingStart",2);

	// {analogReadPollingStop class java.lang.Integer} 
	byteToMethod.put(3,"analogReadPollingStop");
	methodToByte.put("analogReadPollingStop",3);

	// {analogWrite class java.lang.Integer class java.lang.Integer} 
	byteToMethod.put(4,"analogWrite");
	methodToByte.put("analogWrite",4);

	// {digitalReadPollingStart class java.lang.Integer} 
	byteToMethod.put(5,"digitalReadPollingStart");
	methodToByte.put("digitalReadPollingStart",5);

	// {digitalReadPollingStop class java.lang.Integer} 
	byteToMethod.put(6,"digitalReadPollingStop");
	methodToByte.put("digitalReadPollingStop",6);

	// {digitalWrite class java.lang.Integer class java.lang.Integer} 
	byteToMethod.put(7,"digitalWrite");
	methodToByte.put("digitalWrite",7);

	// {motorAttach class java.lang.String class java.lang.String class java.lang.Integer class java.lang.Integer class java.lang.Integer} 
	byteToMethod.put(8,"motorAttach");
	methodToByte.put("motorAttach",8);

	// {motorDetach class java.lang.String} 
	byteToMethod.put(9,"motorDetach");
	methodToByte.put("motorDetach",9);

	// {motorMove class java.lang.String} 
	byteToMethod.put(10,"motorMove");
	methodToByte.put("motorMove",10);

	// {motorMoveTo class java.lang.String double} 
	byteToMethod.put(11,"motorMoveTo");
	methodToByte.put("motorMoveTo",11);

	// {pinMode class java.lang.Integer class java.lang.Integer} 
	byteToMethod.put(12,"pinMode");
	methodToByte.put("pinMode",12);

	// {publishMRLCommError class java.lang.Integer} 
	byteToMethod.put(13,"publishMRLCommError");
	methodToByte.put("publishMRLCommError",13);

	// {publishPin class org.myrobotlab.service.data.Pin} 
	byteToMethod.put(14,"publishPin");
	methodToByte.put("publishPin",14);

	// {publishVersion class java.lang.Integer} 
	byteToMethod.put(15,"publishVersion");
	methodToByte.put("publishVersion",15);

	// {pulseIn int int int int} 
	byteToMethod.put(16,"pulseIn");
	methodToByte.put("pulseIn",16);

	// {sensorAttach class java.lang.String} 
	byteToMethod.put(17,"sensorAttach");
	methodToByte.put("sensorAttach",17);

	// {sensorPollingStart class java.lang.String int} 
	byteToMethod.put(18,"sensorPollingStart");
	methodToByte.put("sensorPollingStart",18);

	// {sensorPollingStop class java.lang.String} 
	byteToMethod.put(19,"sensorPollingStop");
	methodToByte.put("sensorPollingStop",19);

	// {servoAttach class java.lang.String class java.lang.Integer} 
	byteToMethod.put(20,"servoAttach");
	methodToByte.put("servoAttach",20);

	// {servoDetach class java.lang.String} 
	byteToMethod.put(21,"servoDetach");
	methodToByte.put("servoDetach",21);

	// {servoStop class java.lang.String} 
	byteToMethod.put(22,"servoStop");
	methodToByte.put("servoStop",22);

	// {servoSweep class java.lang.String int int int} 
	byteToMethod.put(23,"servoSweep");
	methodToByte.put("servoSweep",23);

	// {servoWrite class java.lang.String class java.lang.Integer} 
	byteToMethod.put(24,"servoWrite");
	methodToByte.put("servoWrite",24);

	// {servoWriteMicroseconds class java.lang.String class java.lang.Integer} 
	byteToMethod.put(25,"servoWriteMicroseconds");
	methodToByte.put("servoWriteMicroseconds",25);

	// {setDebounce int} 
	byteToMethod.put(26,"setDebounce");
	methodToByte.put("setDebounce",26);

	// {setDigitalTriggerOnly class java.lang.Boolean} 
	byteToMethod.put(27,"setDigitalTriggerOnly");
	methodToByte.put("setDigitalTriggerOnly",27);

	// {setLoadTimingEnabled boolean} 
	byteToMethod.put(28,"setLoadTimingEnabled");
	methodToByte.put("setLoadTimingEnabled",28);

	// {setPWMFrequency class java.lang.Integer class java.lang.Integer} 
	byteToMethod.put(29,"setPWMFrequency");
	methodToByte.put("setPWMFrequency",29);

	// {setSampleRate int} 
	byteToMethod.put(30,"setSampleRate");
	methodToByte.put("setSampleRate",30);

	// {setSerialRate int} 
	byteToMethod.put(31,"setSerialRate");
	methodToByte.put("setSerialRate",31);

	// {setServoEventsEnabled class java.lang.String boolean} 
	byteToMethod.put(32,"setServoEventsEnabled");
	methodToByte.put("setServoEventsEnabled",32);

	// {setServoSpeed class java.lang.String class java.lang.Float} 
	byteToMethod.put(33,"setServoSpeed");
	methodToByte.put("setServoSpeed",33);

	// {setStepperSpeed class java.lang.Integer} 
	byteToMethod.put(34,"setStepperSpeed");
	methodToByte.put("setStepperSpeed",34);

	// {softReset} 
	byteToMethod.put(35,"softReset");
	methodToByte.put("softReset",35);

	// {stepperAttach class java.lang.String} 
	byteToMethod.put(36,"stepperAttach");
	methodToByte.put("stepperAttach",36);

	// {stepperDetach class java.lang.String} 
	byteToMethod.put(37,"stepperDetach");
	methodToByte.put("stepperDetach",37);

	// {stepperMove class java.lang.String class java.lang.Integer} 
	byteToMethod.put(38,"stepperMove");
	methodToByte.put("stepperMove",38);

	// {stepperReset class java.lang.String} 
	byteToMethod.put(39,"stepperReset");
	methodToByte.put("stepperReset",39);

	// {stepperStep class java.lang.String class java.lang.Integer class java.lang.Integer} 
	byteToMethod.put(40,"stepperStep");
	methodToByte.put("stepperStep",40);

	// {stepperStop class java.lang.String} 
	byteToMethod.put(41,"stepperStop");
	methodToByte.put("stepperStop",41);

	// {stopService} 
	byteToMethod.put(42,"stopService");
	methodToByte.put("stopService",42);

///// java ByteToMethod generated definition - DO NOT MODIFY - End //////

		///// JAVA GENERATED DEFINITION END //////
	}
	
	/**
	 * MAGIC_NUMBER|NUM_BYTES|FUNCTION|DATA0|DATA1|....|DATA(N)
	 * @throws CodecException 
	 */
	@Override
	public String decode(int newByte) throws CodecException {
		++byteCount;
		if (byteCount == 1 && newByte != Arduino2.MAGIC_NUMBER)
		{
			// reset - try again
			rest.setLength(0);
			byteCount = 0;
			throw new CodecException("bad magic number");
		}

		if (byteCount == 2)
		{
		   // get the size of message
		   // todo check msg < 64 (MAX_MSG_SIZE)
		   msgSize = newByte;
		}
		
		// set method
		if (byteCount == 3){
			rest.append(byteToMethod.get(newByte));
		}
		
		if (byteCount > 3) {
			// FIXME - for 
			rest.append(String.format("/%d", newByte));
		}

		// if received header + msg
		if (byteCount == 2 + msgSize)
		{
		  // msg done
		  byteCount = 0;
		  rest.append("\n");
		  return rest.toString();
		}
		
		// not ready yet
		// no msg :P should be null ???
		return "";
	}

	int segment = 0;

	// must maintain state - partial string
	@Override
	public int[] encode(String msg) {
		
		// while not done - string not completed...
		// make sure you leave in a good state if not a full String
		
		// newline terminator
		int pos = msg.indexOf("/");
		
		if (pos != -1 && segment == 0){
			// found method
			if (methodToByte.containsKey(msg)){
				
			}
		}
		
		return new int[0];
	}

	@Override
	public String getCodecExt() {
		return "arduino";
	}
	

	public static void main(String[] args) {
		
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);
			
			ArduinoMsgCodec codec = new ArduinoMsgCodec();

			// digitalWrite/9/1
			StringBuilder sb = new StringBuilder();
			sb.append(codec.decode(170));
			sb.append(codec.decode(3));
			sb.append(codec.decode(7));
			sb.append(codec.decode(9));
			sb.append(codec.decode(1));
			
			log.info(sb.toString());
			
			codec.encode(sb.toString());
			
			Arduino2 arduino = (Arduino2)Runtime.start("arduino","Arduino2");
			Serial serial = arduino.getSerial();
			serial.record();
			serial.processRxByte(170);
			serial.processRxByte(3);
			serial.processRxByte(7);
			serial.processRxByte(9);
			serial.processRxByte(1);
			
		} catch (Exception e) {
			Logging.logException(e);
		}
		
	}


}
