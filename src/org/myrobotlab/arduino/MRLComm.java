package org.myrobotlab.arduino;

import java.util.HashMap;

public class MRLComm {
	
	// ---------- MRLCOMM FUNCTION INTERFACE BEGIN -----------

	static HashMap<Integer, String> rest = new HashMap<Integer, String>();
	
	public static final int MRLCOMM_VERSION = 20;

	// serial protocol functions
	public static final int MAGIC_NUMBER = 170; // 10101010


	// imported Arduino constants
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;
	
	//  Arduino & MRLComm methods
	public static final int DIGITAL_WRITE = 0;
	public static final int DIGITAL_VALUE_CALLBACK = 1; // normalized with PinData
	public static final int ANALOG_WRITE = 2;
	public static final int ANALOG_VALUE_CALLBACK = 3; // normalized with PinData
	public static final int PINMODE = 4;
	public static final int PULSE_IN = 5; // FIXME seperate ordinals !
	public static final int PULSE_IN_CALLBACK = 31; // FIXME seperate ordinals !
	public static final int SERVO_ATTACH = 6;
	public static final int SERVO_WRITE = 7;
	public static final int SERVO_SET_MAX_PULSE = 8;
	public static final int SERVO_DETACH = 9;
	public static final int SET_PWM_FREQUENCY = 11;
	public static final int SET_SERVO_SPEED = 12;
	public static final int ANALOG_READ_POLLING_START = 13;
	public static final int ANALOG_READ_POLLING_STOP = 14;
	public static final int DIGITAL_READ_POLLING_START = 15;
	public static final int DIGITAL_READ_POLLING_STOP = 16;
	public static final int SET_ANALOG_TRIGGER = 17;
	public static final int REMOVE_ANALOG_TRIGGER = 18;
	public static final int SET_DIGITAL_TRIGGER = 19;
	public static final int REMOVE_DIGITAL_TRIGGER = 20;
	public static final int DIGITAL_DEBOUNCE_ON = 21;
	public static final int DIGITAL_DEBOUNCE_OFF = 22;
	public static final int DIGITAL_TRIGGER_ONLY_ON = 23;
	public static final int DIGITAL_TRIGGER_ONLY_OFF = 24;
	public static final int SET_SERIAL_RATE = 25;
	public static final int GET_MRLCOMM_VERSION_CALLBACK = 26;
	public static final int SET_SAMPLE_RATE = 27;
	public static final int SERVO_WRITE_MICROSECONDS = 28;
	
	public static final int MRLCOMM_ERROR_CALLBACK = 29;

	public static final int PINGDAR_ATTACH = 30;
	public static final int PINGDAR_START = 31;
	public static final int PINGDAR_STOP = 32;
	public static final int PINGDAR_DATA = 33;

	public static final int SENSOR_ATTACH = 34;
	public static final int SENSOR_POLLING_START = 35;
	public static final int SENSOR_POLLING_STOP = 36;
	public static final int SENSOR_DATA_CALLBACK = 37;

	public static final int SERVO_SWEEP_START = 38;
	public static final int SERVO_SWEEP_STOP = 39;
				
	// callback event - e.g. position arrived
	// MSG MAGIC | SZ | SERVO-INDEX | POSITION
	public static final int SERVO_EVENTS_ENABLE = 40;
	public static final int SERVO_EVENT_CALLBACK = 41;
	
	public static final int LOAD_TIMING_ENABLE = 42;
	public static final int LOAD_TIMING_EVENT_CALLBACK = 43;

	public static final int STEPPER_ATTACH	= 44;
	public static final int STEPPER_MOVE = 45; 
	public static final int STEPPER_STOP = 46; 
	public static final int STEPPER_RESET = 47; 

	public static final int STEPPER_EVENT_CALLBACK = 48; 
	public static final int STEPPER_EVENT_STOP = 1; 

	public static final int STEPPER_TYPE_POLOLU = 1; 
	
	public static final int CUSTOM_MSG_CALLBACK = 50;
	
	public static final int ARDUINO_TYPE_INT = 16;
	
// servo event types
	public static final int SERVO_EVENT_STOPPED = 1;
	public static final int SERVO_EVENT_POSITION_UPDATE = 2;

	// error types
	public static final int ERROR_SERIAL = 1;
	public static final int ERROR_UNKOWN_CMD = 2;

	// sensor types
	public static final int SENSOR_ULTRASONIC = 1;

	// need a method to identify type of board
	// http://forum.arduino.cc/index.php?topic=100557.0

	public static final int COMMUNICATION_RESET = 252;
	public static final int SOFT_RESET = 253;
	public static final int NOP = 255;

	// ---------- MRLCOMM FUNCTION INTERFACE END ----------

	HashMap<Integer, Method> ordinalToMethod = new HashMap<Integer, Method>();
	HashMap<String, Method> nameToMethod = new HashMap<String, Method>();
	
	public static class Method {
		int ordinal;
		String direction;
		String method;
		Class<?>[] params;

		public Method(int ordinal, String method, Class<?>... params) {
			this.ordinal = ordinal;
			this.method = method;
			this.params = params;
		}
	}

	public void put(int ordinal, String method, Class<?>... params) {
		Method newMethod = new Method(ordinal, method, params);
		
		ordinalToMethod.put(ordinal, newMethod);
		nameToMethod.put(method, newMethod);
	}

	public void init() {
		put(DIGITAL_WRITE, "digitalWrite", byte.class /*pin*/, byte.class /*value*/);
		put(DIGITAL_VALUE_CALLBACK, "digitalValue", byte.class /*pin*/, byte.class /*value*/);
		put(ANALOG_WRITE, "analogWrite", byte.class /*pin*/, byte.class /*value*/);
		put(ANALOG_VALUE_CALLBACK, "analogValue", byte.class /*pin*/, int.class /*value*/);
		put(PINMODE, "pinMode", int.class /*pin*/, byte.class /*value (IN | OUT)*/);
		put(PULSE_IN, "pulseIn", int.class /*pin*/, int.class /*value*/);
		put(PULSE_IN_CALLBACK, "pulseIn", int.class /*pin*/, int.class /*value*/);
		put(SERVO_ATTACH, "servoAttach", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
		put(DIGITAL_WRITE, "digitalWrite", int.class /*pin*/, int.class /*value*/);
	}

}
