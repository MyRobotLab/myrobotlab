package org.myrobotlab.serial;

import java.io.IOException;
import java.util.TooManyListenersException;

public interface SerialDevice {

	public static final int DATABITS_5 = 5;
	public static final int DATABITS_6 = 6;
	public static final int DATABITS_7 = 7;
	public static final int DATABITS_8 = 8;
	public static final int PARITY_NONE = 0;
	public static final int PARITY_ODD = 1;
	public static final int PARITY_EVEN = 2;
	public static final int PARITY_MARK = 3;
	public static final int PARITY_SPACE = 4;
	public static final int STOPBITS_1 = 1;
	public static final int STOPBITS_2 = 2;
	public static final int STOPBITS_1_5 = 3;
	public static final int FLOWCONTROL_NONE = 0;
	public static final int FLOWCONTROL_RTSCTS_IN = 1;
	public static final int FLOWCONTROL_RTSCTS_OUT = 2;
	public static final int FLOWCONTROL_XONXOFF_IN = 4;
	public static final int FLOWCONTROL_XONXOFF_OUT = 8;
	public static final int PORTTYPE_SERIAL = 1;

	// identification
	public String getName();

	// public int getPortType();
	// public boolean isCurrentlyOwned();
	public int available() throws IOException;

	// open / close
	public void open() throws SerialDeviceException;

	// public SerialDevice open(FileDescriptor f) throws
	// SerialDeviceException;
	// public SerialDevice open(String TheOwner, int i) throws
	// SerialDeviceException;
	public boolean isOpen();

	public void close();

	// input/output
	// public InputStream getInputStream() throws IOException;
	// public OutputStream getOutputStream() throws IOException;

	// serial parameters
	public void setParams(int b, int d, int s, int p) throws SerialDeviceException;

	// special serial methods/states
	// public boolean isDTR();
	public void setDTR(boolean state);

	public void setRTS(boolean state);

	// public boolean isCTS();
	// public boolean isDSR();
	// public boolean isCD();
	// public boolean isRI();
	// public boolean isRTS();

	// reading/listening events
	public void addEventListener(SerialDeviceEventListener lsnr) throws TooManyListenersException;

	public void notifyOnDataAvailable(boolean enable);

	// write methods
	public void write(int data) throws IOException;

	public void write(int[] data) throws IOException;

	public int read() throws IOException;

	// LAME - but Java goes both ways with InputStream
	// it returns a single "int" / byte form 0/255
	// in addition it returns your byte array - as java bytes - which no one uses :P
	public int read(byte[] data) throws IOException;
}