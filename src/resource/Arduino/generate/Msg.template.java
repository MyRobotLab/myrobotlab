package org.myrobotlab.arduino;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.myrobotlab.logging.Level;

import org.myrobotlab.arduino.virtual.MrlComm;

/**
 * <pre>
 * 
 Welcome to Msg.java
 Its created by running ArduinoMsgGenerator
 which combines the MrlComm message schema (src/resource/Arduino/arduinoMsg.schema)
 with the cpp template (src/resource/Arduino/generate/Msg.template.java)

 	Schema Type Conversions

	Schema      ARDUINO					Java							Range
	none		byte/unsigned char		int (cuz Java byte bites)		1 byte - 0 to 255
	boolean		boolean					boolean							0 1
    b16			int						int (short)						2 bytes	-32,768 to 32,767
    b32			long					int								4 bytes -2,147,483,648 to 2,147,483, 647
    bu32		unsigned long			long							0 to 4,294,967,295
    str			char*, size				String							variable length
    []			byte[], size			int[]							variable length

 All message editing should be done in the arduinoMsg.schema

 The binary wire format of an %javaArduinoClass% is:

 MAGIC_NUMBER|MSG_SIZE|METHOD_NUMBER|PARAM0|PARAM1 ...
 
 </pre>

 */

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.VirtualArduino;

import java.io.FileOutputStream;
import java.util.Arrays;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * Singlton messaging interface to an %javaArduinoClass%
 *
 * @author GroG
 *
 */

public class %javaClass% {

	public static final int MAX_MSG_SIZE = 64;
	public static final int MAGIC_NUMBER = 170; // 10101010
	public static final int MRLCOMM_VERSION = %MRLCOMM_VERSION%;
	
	// send buffer
  int sendBufferSize = 0;
  int sendBuffer[] = new int[MAX_MSG_SIZE];
  
  // recv buffer
  int ioCmd[] = new int[MAX_MSG_SIZE];
  
  int byteCount = 0;
  int msgSize = 0;

	// ------ device type mapping constants
	int method = -1;
	public boolean debug = false;
	boolean invoke = true;
	
	boolean ackEnabled = %ackEnabled%;
	
	 public static class AckLock {
	    // first is always true - since there
	    // is no msg to be acknowledged...
	    volatile boolean acknowledged = true;
	  }
	 
	transient AckLock ackRecievedLock = new AckLock();
	
	// recording related
	transient FileOutputStream record = null;
	transient StringBuilder rxBuffer = new StringBuilder();
	transient StringBuilder txBuffer = new StringBuilder();	

%javaDeviceTypes%		
%javaDefines%

/**
 * These methods will be invoked from the Msg class as callbacks from MrlComm.
 */
	
%javaGeneratedCallBacks%	

	
	public transient final static Logger log = LoggerFactory.getLogger(Msg.class);

	public %javaClass%(%javaArduinoClass% arduino, SerialDevice serial) {
		this.arduino = arduino;
		this.serial = serial;
	}
	
	public void begin(SerialDevice serial){
	  this.serial = serial;
	}

	// transient private Msg instance;

	// ArduinoSerialCallBacks - TODO - extract interface
	transient private %javaArduinoClass% arduino;
	
	transient private SerialDevice serial;

	/**
	 * want to grab it when SerialDevice is created
	 *
	 * @param serial
	 * @return
	 */
	/*
	static public synchronized Msg getInstance(%javaArduinoClass% arduino, SerialDevice serial) {
		if (instance == null) {
			instance = new Msg();
		}

		instance.arduino = arduino;
		instance.serial = serial;

		return instance;
	}
	*/
	
	public void setInvoke(boolean b){
	  invoke = b;
	}
	
	public void processCommand(){
	  processCommand(ioCmd);
	}
	
	public void processCommand(int[] ioCmd) {
		int startPos = 0;
		method = ioCmd[startPos];
		switch (method) {
%javaHandleCases%		
		}
	}
	

	// Java-land --to--> MrlComm
%javaMethods%

	public static String methodToString(int method) {
		switch (method) {
%methodToString%
		default: {
			return "ERROR UNKNOWN METHOD (" + Integer.toString(method) + ")";

		} // default
		}
	}

	public String str(int[] buffer, int start, int size) {
		byte[] b = new byte[size];
		for (int i = start; i < start + size; ++i){
			b[i - start] = (byte)(buffer[i] & 0xFF);
		}
		return new String(b);
	}

	public int[] subArray(int[] buffer, int start, int size) {		
		return Arrays.copyOfRange(buffer, start, start + size);
	}

	// signed 16 bit bucket
	public int b16(int[] buffer, int start/*=0*/) {
		return  (short)(buffer[start] << 8) + buffer[start + 1];
	}
	
	// signed 32 bit bucket
	public int b32(int[] buffer, int start/*=0*/) {
		return ((buffer[start + 0] << 24) + (buffer[start + 1] << 16)
				+ (buffer[start + 2] << 8) + buffer[start + 3]);
	}
	
	// unsigned 32 bit bucket
	public long bu32(int[] buffer, int start/*=0*/) {
		long ret = ((buffer[start + 0] << 24)
				+ (buffer[start + 1] << 16)
				+ (buffer[start + 2] << 8) + buffer[start + 3]);
		if (ret < 0){
			return 4294967296L + ret;
		}
		
		return ret;
	}

  // float 32 bit bucket
  public float f32(int[] buffer, int start/*=0*/) {
    byte[] b = new byte[4];
    for (int i = 0; i < 4; ++i){
      b[i] = (byte)buffer[start + i];
    }
    float f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
    return f;
  }
  
  public boolean readMsg() throws Exception {
    // handle serial data begin
    int bytesAvailable = serial.available();
    if (bytesAvailable > 0) {
      //publishDebug("RXBUFF:" + String(bytesAvailable));
      // now we should loop over the available bytes .. not just read one by one.
      for (int i = 0; i < bytesAvailable; i++) {
        // read the incoming byte:
        int newByte = serial.read();
        //publishDebug("RX:" + String(newByte));
        ++byteCount;
        // checking first byte - beginning of message?
        if (byteCount == 1 && newByte != VirtualMsg.MAGIC_NUMBER) {
          publishError(F("error serial"));
          // reset - try again
          byteCount = 0;
          // return false;
        }
        if (byteCount == 2) {
          // get the size of message
          // todo check msg < 64 (MAX_MSG_SIZE)
          if (newByte > 64) {
            // TODO - send error back
            byteCount = 0;
            continue; // GroG - I guess  we continue now vs return false on error conditions?
          }
          msgSize = newByte;
        }
        if (byteCount > 2) {
          // fill in msg data - (2) headbytes -1 (offset)
          ioCmd[byteCount - 3] = newByte;
        }
        // if received header + msg
        if (byteCount == 2 + msgSize) {
          // we've reach the end of the command, just return true .. we've got it
          byteCount = 0;
          return true;
        }
      }
    } // if Serial.available
      // we only partially read a command.  (or nothing at all.)
    return false;
  }

  String F(String msg) {
    return msg;
  }
  
  public void publishError(String error) {
    log.error(error);
  }
  
	void write(int b8) throws Exception {

		if ((b8 < 0) || (b8 > 255)) {
			log.error("writeByte overrun - should be  0 <= value <= 255 - value = {}", b8);
		}

		serial.write(b8 & 0xFF);
	}

	void writebool(boolean b1) throws Exception {
		if (b1) {
			serial.write(1);
		} else {
			serial.write(0);
		}
	}

	void writeb16(int b16) throws Exception {
		if ((b16 < -32768) || (b16 > 32767)) {
			log.error("writeByte overrun - should be  -32,768 <= value <= 32,767 - value = {}", b16);
		}

		write(b16 >> 8 & 0xFF);
		write(b16 & 0xFF);
	}

	void writeb32(int b32) throws Exception {
		write(b32 >> 24 & 0xFF);
		write(b32 >> 16 & 0xFF);
		write(b32 >> 8 & 0xFF);
		write(b32 & 0xFF);
	}
	
	void writef32(float f32) throws Exception {
    //  int x = Float.floatToIntBits(f32);
    byte[] f = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(f32).array();
    write(f[3] & 0xFF);
    write(f[2] & 0xFF);
    write(f[1] & 0xFF);
    write(f[0] & 0xFF);
	}
	
	void writebu32(long b32) throws Exception {
		write((int)(b32 >> 24 & 0xFF));
		write((int)(b32 >> 16 & 0xFF));
		write((int)(b32 >> 8 & 0xFF));
		write((int)(b32 & 0xFF));
	}

	void write(String str) throws Exception {
		write(str.getBytes());
	}

	void write(int[] array) throws Exception {
		// write size
		write(array.length & 0xFF);

		// write data
		for (int i = 0; i < array.length; ++i) {
			write(array[i] & 0xFF);
		}
	}

	void write(byte[] array) throws Exception {
		// write size
		write(array.length);

		// write data
		for (int i = 0; i < array.length; ++i) {
			write(array[i]);
		}
	}
	
	
	public boolean isRecording() {
		return record != null;
	}
	

	public void record() throws Exception {
		
		if (record == null) {
			record = new FileOutputStream(String.format("%s.ard", arduino.getName()));
		}
	}

	public void stopRecording() {
		if (record != null) {
			try {
				record.close();
			} catch (Exception e) {
			}
			record = null;
		}
	}
	
	public static String deviceTypeToString(int typeId) {
		switch(typeId){
%deviceTypeToString%		
		default: {
			return "unknown";
		}
		}
	}
  
  /**
   * enable acks on both sides Arduino/Java-Land
   * and MrlComm-land
   */
  public void enableAcks(boolean b){
    // disable local blocking
	  ackEnabled = b;
	  // if (!localOnly){
	  // shutdown MrlComm from sending acks
	  // below is a method only in Msg.java not in VirtualMsg.java
	  // it depends on the definition of enableAck in arduinoMsg.schema  
	  // %enableAck%
	  // }
	}
	
	public void waitForAck(){
	  if (!ackEnabled || ackRecievedLock.acknowledged){
	    return;
	  }
    synchronized (ackRecievedLock) {
      try {
        long ts = System.currentTimeMillis();
        // log.info("***** starting wait *****");
        ackRecievedLock.wait(2000);
        // log.info("*****  waited {} ms *****", (System.currentTimeMillis() - ts));
      } catch (InterruptedException e) {// don't care}
      }

      if (!ackRecievedLock.acknowledged) {
        //log.error("Ack not received : {} {}", Msg.methodToString(ioCmd[0]), numAck);
        log.error("Ack not received");
      }
    }
	}
	
	public void ackReceived(int function){
	   synchronized (ackRecievedLock) {
	      ackRecievedLock.acknowledged = true;
	      ackRecievedLock.notifyAll();
	    }
	}
	
	public int getMethod(){
	  return method;
	}
	

  public void add(int value) {
    sendBuffer[sendBufferSize] = (value & 0xFF);
    sendBufferSize += 1;
  }
  
  public int[] getBuffer() {    
    return sendBuffer;
  }
	
	public static void main(String[] args) {
		try {

			// FIXME - Test service started or reference retrieved
			// FIXME - subscribe to publishError
			// FIXME - check for any error
			// FIXME - basic design - expected state is connected and ready -
			// between classes it
			// should connect - also dumping serial comm at different levels so
			// virtual arduino in
			// Python can model "real" serial comm
			String port = "COM10";

			LoggingFactory.init(Level.INFO);
			
			/*
			Runtime.start("gui","SwingGui");
			VirtualArduino virtual = (VirtualArduino)Runtime.start("varduino","VirtualArduino");
			virtual.connectVirtualUart(port, port + "UART");
			*/
			
			%javaArduinoClass% arduino = (%javaArduinoClass%)Runtime.start("arduino","%javaArduinoClass%");
			Servo servo01 = (Servo)Runtime.start("servo01","Servo");
			
			/*
			arduino.connect(port);
			
			// test pins
			arduino.enablePin(5);
			
			arduino.disablePin(5);
			
			// test status list enabled
			arduino.enableBoardStatus(true);
			
			servo01.attach(arduino, 8);
			
			servo01.moveTo(30);
			servo01.moveTo(130);
			
			arduino.enableBoardStatus(false);
			*/
			// test ack
			
			// test heartbeat
			
			

		} catch (Exception e) {
			log.error("main threw", e);
		}

	}

}
