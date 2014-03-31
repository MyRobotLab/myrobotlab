package org.myrobotlab.service.data;

public class PinState {

	// type
	public static final int DIGITAL = 0x0;
	public static final int ANALOG = 0x1;
	public static final int ANALOGDIGITAL = 0x2;

	// mode
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

	public static final int UNKNOWN = 0xFF;

	/**
	 * type - represents the physical type of pin - on an Arduino they can be
	 * DIGTAL (read/write), ANALOG (read only), ANALOGDIGITAL (pwm read/write)
	 */
	public int type = UNKNOWN;
	public int mode = UNKNOWN;
	public int address = UNKNOWN;
	public int value = UNKNOWN;
}
