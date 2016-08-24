package org.myrobotlab.arduino;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class MrlMsg {

	public transient final static Logger log = LoggerFactory.getLogger(MrlMsg.class);
	
	Integer msgType;
	/**
	 * device id - this identifies the device on the MrlComm deviceList
	 * For example a servo might be device id 1, in that case this variable should be 1
	 * to identify which device the message should be routed to
	 * 
	 * Arduino system calls (e.g. pinMode, digitalWrite, analogWrite) will have
	 * device id == null
	 * 
	 */
	Integer id; 

	/**
	 * data are really bytes - but signed byte is such a PITA - we will use ints
	 * (so does the makers of InputStream & OutputStream ;)
	 * 
	 * <pre>
	 * Java Data Types
	 * type						range								bytes	bits
	 * byte						-128 to 127 (YUCK !)				1		8
	 * int						-2,147,483,648 to 2,147,483,647		4		16
	 * long						-(really small) to (really big)		8		64
	 * 
	 * 
	 * 
	 * Arduino Data Types
	 * type						range								bytes	bits
	 * byte						0 to 255							1		8
	 * int 						-32,768 to 32,767  					2 		16
	 * unsigned int				0 to 65,535 						2		16
	 * long						-2,147,483,648 to 2,147,483,647.	4		32
	 * unsigned long			0 to 4,294,967,295					4		32
	 * </pre>
	 */

	List<Integer> dataBuffer = new ArrayList<Integer>();

	public MrlMsg(int msgType) {
		this.msgType = msgType;
	}

	// add msg name or id ???

	public MrlMsg(int msgType, int id) {
		this.msgType = msgType;
		this.id = id;
		dataBuffer.add(id);
	}

	/**
	 * default add a byte of data
	 * 
	 * @param data
	 */
	public void addData(int data) {
		dataBuffer.add(data & 0xff);
	}

	/**
	 * equivalent to a Arduino int
	 * 
	 * Java Int --to--> Arduino int  (-32,768 to 32,767) or
	 * Java Int --to--> Arduino unsigned int (0 to 65,535)
	 * 
	 * @param data
	 */
	public void addData16(int data) {
		if (data < -32768 || data > 32767){
			log.warn("addData16 value is {} - if Arduino type is an int data will be lost");
		}
		
		if (data > 65535){
			log.warn("addData16 value is {} - if Arduino type is an unsigned int data will be lost");
		}
		dataBuffer.add((data >> 8) & 0xFF);
		dataBuffer.add(data & 0xFF);
	}

	/**
	 * equivalent to a Arduino long
	 * typically used for
	 * 
	 * Java Int --to--> Arduino Long (-2,147,483,648 to 2,147,483,647)
	 * 
	 * @param data
	 */
	public void addData32(int data) {
		dataBuffer.add((data >> 24) & 0xFF);
		dataBuffer.add((data >> 16) & 0xFF);
		dataBuffer.add((data >> 8) & 0xFF);
		dataBuffer.add(data & 0xFF);
	}

	/**
	 * size first for variable length
	 * 
	 * @param args
	 */
	public void addData(String data) {
		dataBuffer.add(data.length());
		for (int i = 0; i < data.length(); ++i) {
			dataBuffer.add((int) data.charAt(i));
		}
	}

	public List<Integer> getList() {
		return dataBuffer;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public int getMethod() {
		return msgType;
	}

}
