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

package org.myrobotlab.serial.gnu;

import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.TooManyListenersException;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;


// System.nanoTime()
// http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks
// http://ostermiller.org/convert_java_outputstream_inputstream.html
// Arduino - new soft serial - http://www.arduino.cc/cgi-bin/yabb2/YaBB.pl?num=1233298805
// rxtx home http://users.frii.com/jarvi/rxtx/download.html

public class BinaryCommPort extends SerialPort {

	public final static Logger log = LoggerFactory.getLogger(BinaryCommPort.class.getCanonicalName());

	private LineDriver lineDriver = null;

	// ByteArrayOutputStream in;
	ByteArrayOutputStream out;
	InputStream is;
	BinaryCommOutputStream os;

	public class BinaryCommOutputStream extends OutputStream {

		// High Resolution Timer in Java 5
		// http://www.sagui.org/~gustavo/blog/code
		// http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks

		Random generator = new Random();

		@Override
		public void write(int b) throws IOException {

			try {
				int period = 3; // this would be 300 baud

				// beginning of start bit
				lineDriver.pulseDown();
				// Thread.sleep(period,166);
				Thread.sleep(3, 333);
				// end start bit

				StringBuffer s = new StringBuffer(8); // for logging (the binary
														// string)

				// 8 random bits
				for (int i = 0; i < 8; ++i) {
					int x = generator.nextInt(100);
					if (i % 2 == 0) // && i < 7 //x > 150 &&
					{
						lineDriver.pulseDown();
						s.append("0");
					} else {
						lineDriver.pulseUp();
						s.append("1");
					}
					Thread.sleep(3, 333);
				}

				// the stop bit
				lineDriver.pulseUp();
				Thread.sleep(50); // extra time just to be nice

				log.info("{}",s);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public interface LineDriver {
		void pulseUp();

		void pulseDown();
	};

	public void setLineDriver(LineDriver ld) {
		lineDriver = ld;
	}

	@Override
	public void addEventListener(SerialPortEventListener arg0) throws TooManyListenersException {
		// TODO Auto-generated method stub
		log.info("addEventListener");
	}

	@Override
	public int getBaudBase() throws UnsupportedCommOperationException, IOException {
		log.info("getBaudBase");
		// TODO ????????
		return 9600;
	}

	@Override
	public int getBaudRate() {
		log.info("getBaudRate");
		return 9600;
	}

	@Override
	public boolean getCallOutHangup() throws UnsupportedCommOperationException {
		log.info("getCallOutHangup");
		return false;
	}

	@Override
	public int getDataBits() {
		log.info("getDataBits");
		return SerialPort.DATABITS_8;
	}

	@Override
	public int getDivisor() throws UnsupportedCommOperationException, IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getEndOfInputChar() throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFlowControlMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getLowLatency() throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getParity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getParityErrorChar() throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getStopBits() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getUARTType() throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCD() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCTS() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDSR() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDTR() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRI() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRTS() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void notifyOnBreakInterrupt(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnCTS(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnCarrierDetect(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnDSR(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnDataAvailable(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnFramingError(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnOutputEmpty(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnOverrunError(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnParityError(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOnRingIndicator(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeEventListener() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendBreak(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setBaudBase(int arg0) throws UnsupportedCommOperationException, IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setCallOutHangup(boolean arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDTR(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setDivisor(int arg0) throws UnsupportedCommOperationException, IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setEndOfInputChar(byte arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFlowControlMode(int arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setLowLatency() throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setParityErrorChar(byte arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setRTS(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSerialPortParams(int arg0, int arg1, int arg2, int arg3) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setUARTType(String arg0, boolean arg1) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void disableReceiveFraming() {
		// TODO Auto-generated method stub

	}

	@Override
	public void disableReceiveThreshold() {
		// TODO Auto-generated method stub

	}

	@Override
	public void disableReceiveTimeout() {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableReceiveFraming(int arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableReceiveThreshold(int arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableReceiveTimeout(int arg0) throws UnsupportedCommOperationException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getInputBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public InputStream getInputStream() throws IOException {

		out = new ByteArrayOutputStream();
		is = new ByteArrayInputStream(out.toByteArray());

		// TODO Auto-generated method stub
		return is;
	}

	@Override
	public int getOutputBufferSize() {
		return 0;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		// os = new ByteArrayOutputStream();
		os = new BinaryCommOutputStream();
		return os;
	}

	@Override
	public int getReceiveFramingByte() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getReceiveThreshold() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getReceiveTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isReceiveFramingEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReceiveThresholdEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReceiveTimeoutEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setInputBufferSize(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOutputBufferSize(int arg0) {
		// TODO Auto-generated method stub

	}

}
