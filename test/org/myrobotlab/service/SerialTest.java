/**
 * 
 */
package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FileIO.FileComparisonException;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.VirtualSerialPort.VirtualNullModemCable;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *  This class is just responsible for testing the service
 *  Assumptions :
 *  	Service dependencies installed
 *  	Environment classpath is set appropriately to those dependencies
 *  	Environment java.library.path is set appropriately for the platform being tested
 *  	Static compare and control files test.zip uncompressed into /test/...
 *  
 *  Use the Test service for easy fulfillment of those assumptions
 */
public class SerialTest {

	public final static Logger log = LoggerFactory.getLogger(SerialTest.class);

	static VirtualNullModemCable cable = null;
	static Serial serial = null;
	static Serial uart = null;
	static TestCatcher catcher = null;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		log.info("setUpBeforeClass");

		/*
		if (cleanDepenencies) {
			log.info("cleaning cache");
			if (!Runtime.cleanCache()){
				throw new IOException("could not clean cache");
			}
		}

		if (installDependencies) {
			log.info("installing Serial");
			Repo repo = new Repo("install");
			repo.retrieveServiceType("org.myrobotlab.service.Serial");
			
			if (repo.hasErrors()){
				throw new IOException(repo.getErrors());
			}
			
		}
		*/
		
		// FIXME TODO clean repo before installing !!!!
		serial = (Serial) Runtime.start("serial", "Serial");
		uart = (Serial) Runtime.start("uart", "Serial");
		cable = Serial.createNullModemCable("v0", "v1");
		catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info("tearDownAfterClass");
		// FIXME - check service and thread count
		// Runtime should still exist - but any
		// additional threads should not
		serial.releaseService();
		uart.releaseService();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		log.info("setUp");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		//log.info("tearDown");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#getDescription()}.
	 */
	@Test
	public final void testGetDescription() {
		// fail("Not yet implemented");
		org.junit.Assert.assertNotNull("description is null", serial.getDescription());
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#test()}.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws FileComparisonException
	 */
	@Test
	public final void testTest() throws IOException, InterruptedException, FileComparisonException {
		// non destructive tests
		// TODO - test blocking / non blocking / time-out blocking / reading an
		// array (or don't bother?) or do with length? num bytes to block or
		// timeout
		// TODO - if I am connected to a different serial port
		// get that name - disconnect - and then reconnect when done
		// FIXME - very little functionality for a combined tx rx file
		// TODO - test sendFile & record
		// TODO - speed test
		// TODO use utility methods to help parse read data types
		// because we should not assume we know the details of ints longs etc
		// nor
		// the endianess
		// utility methods - ascii
		// FIXME - // test case write(-1) as display becomes -1 ! - file is
		// different than gui !?!?!

		boolean noWorky = true;
		if (noWorky)
			return;

		int timeout = 500;// 500 ms serial timeout

		// Runtime.start("gui", "GUIService");
		// Runtime.start("webgui", "WebGUI");

		serial.connect(cable.vp0.getName());
		uart.connect(cable.vp1.getName());

		// get serial handle and creates a uart & abl= null modem cable

		// verify the null modem cable is connected
		if (!serial.isConnected()) {
			throw new IOException(String.format("%s not connected", serial.getName()));
		}

		if (!uart.isConnected()) {
			throw new IOException(String.format("%s not connected", uart.getName()));
		}

		serial.stopRecording();
		uart.stopRecording();

		// start binary recording
		serial.record("test/Serial/serial.1");
		uart.record("test/Serial/uart.1");

		// test blocking on exact size
		serial.write("VER\r");
		uart.write("000D\r");
		// read back
		log.info(serial.readString(5));

		// blocking read with timeout
		uart.write("HELLO");
		String helo = serial.readString(5, timeout);

		assertEquals("HELLO", helo);

		log.info("array write");
		serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 127, (byte) 128, (byte) 254, (byte) 255 });

		// FIXME !!! - bug - we wrote a big array to serial -
		// then immediately cleared the uart buffer
		// in fact we cleared it so fast - that the serial data ---going
		// to----> uart
		// has not reached uart (because there is some overhead in moving,
		// reading and formatting the incoming data)
		// then we start checking values in "test blocking" this by that
		// time the serial data above has hit the
		// uart

		// with a virtual null modem cable I could "cheat" and flush() could
		// look at the serial's tx buffer size
		// and block until its cleared - but this would not be typical of
		// "real" serial ports
		// but it could stabilize the test

		// in the real world we don't know when the sender to
		// our receiver is done - so we'll Service.sleep here
		Service.sleep(300);
		log.info("clear buffers");
		serial.clear();
		uart.clear();

		assertEquals(0, serial.available());

		log.info("testing blocking");
		for (int i = 257; i > -2; --i) {
			serial.write(i);
			int readBack = uart.read();
			log.info(String.format("written %d read back %d", i, readBack));
			if (i < 256 && i > -1) {
				assertEquals(String.format("read back not the same as written for value %d %d !", i, readBack), readBack, i);
			}
		}

		// in the real world we don't know when the sender to
		// our receiver is done - so we'll Service.sleep here
		Service.sleep(300);
		log.info("clear buffers");
		serial.clear();
		uart.clear();

		// cleared - nothing should be in the buffer
		assertNull(serial.readString(5, 100));

		// test publish/subscribe nonblocking
		serial.addByteListener(catcher);
		uart.write(64);

		Message msg = catcher.getMsg(100);
		assertEquals("onByte", msg.method);
		assertEquals(1, msg.data.length);
		assertEquals(64, msg.data[0]);

		uart.clear();
		serial.clear();

		serial.write(new String("MRL ROCKS!"));

		// partial buffer filled
		String back = uart.readString(15, 100);
		assertEquals("MRL ROCKS!", back);

		// publish test
		uart.write(new String("MRL ROCKS!"));
		ArrayList<Message> msgs = catcher.getMsgs(100);

		FileIO.compareFiles("test/Serial/serial.1.rx.bin", "test/Serial/control/serial.1.rx.bin");
		FileIO.compareFiles("test/Serial/serial.1.tx.bin", "test/Serial/control/serial.1.tx.bin");
		FileIO.compareFiles("test/Serial/uart.1.rx.bin", "test/Serial/control/uart.1.rx.bin");
		FileIO.compareFiles("test/Serial/uart.1.tx.bin", "test/Serial/control/uart.1.tx.bin");

		serial.stopRecording();
		uart.stopRecording();

		// FIXME compare binary files

		// FIXME - finish up test & compare tx & rx files in multiple formats
		// ======= decimal format begin ===========
		serial.setBinaryFileFormat(false);
		uart.setBinaryFileFormat(false);

		// default non-binary format is ascii decimal
		serial.record("test/Serial/serial.2");
		// uart.record("test/Serial/uart.2");
		serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
		// we have to pause here momentarily
		// so the data can be written and read from the virtual null modem
		// cable (on different threads)
		// before we close the file streams
		Service.sleep(30);
		// uart.stopRecording();
		serial.stopRecording();
		// ======= decimal format end ===========

		// ======= hex format begin ===========
		serial.setDisplayFormat(Serial.DISPLAY_HEX);
		serial.record("test/Serial/serial.3");
		// uart.record("test/Serial/uart.3");
		serial.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, (byte) 255 });
		Service.sleep(30);
		serial.stopRecording();
		// uart.stopRecording();
		// ======= hex format begin ===========

		// parsing of files based on extension check

		// TODO flush & close tests ?
		// serial.disconnect();
		// uart.disconnect();

	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#stopService()}.
	 */
	@Test
	public final void testStopService() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#releaseService()}.
	 */
	@Test
	public final void testReleaseService() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#Serial(java.lang.String)}.
	 */
	@Test
	public final void testSerial() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#setBufferSize(int)}.
	 */
	@Test
	public final void testSetBufferSize() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#bytesToUnsignedInt(byte[], int, int)}
	 * .
	 */
	@Test
	public final void testBytesToUnsignedInt() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#bytesToLong(int[], int, int)}.
	 */
	@Test
	public final void testBytesToLong() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#recordRX(java.lang.String)}.
	 */
	@Test
	public final void testRecordRX() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#recordTX(java.lang.String)}.
	 */
	@Test
	public final void testRecordTX() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#record(java.lang.String)}.
	 */
	@Test
	public final void testRecordString() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#record()}.
	 */
	@Test
	public final void testRecord() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#stopRecording()}.
	 */
	@Test
	public final void testStopRecording() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#isRecording()}.
	 */
	@Test
	public final void testIsRecording() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#getPortName()}.
	 */
	@Test
	public final void testGetPortName() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#format(int)}.
	 */
	@Test
	public final void testFormat() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#getExtention()}.
	 */
	@Test
	public final void testGetExtention() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#serialEvent(org.myrobotlab.serial.SerialDeviceEvent)}
	 * .
	 */
	@Test
	public final void testSerialEvent() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#getPortNames()}.
	 */
	@Test
	public final void testGetPortNames() {
		// fail("Not yet implemented");
		ArrayList<String> ports = serial.getPortNames();
		boolean v0Found = false;
		boolean v1Found = false;
		for (int i = 0; i < ports.size(); ++i) {
			if (ports.get(i).equals("v0")) {
				v0Found = true;
			}
			if (ports.get(i).equals("v1")) {
				v1Found = true;
			}
		}

		if (!v0Found || !v1Found) {
			fail("");
		}
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#addByteListener(org.myrobotlab.service.interfaces.SerialDataListener)}
	 * .
	 */
	@Test
	public final void testAddByteListener() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#connect(java.lang.String, java.lang.Double, java.lang.Double, java.lang.Double, java.lang.Double)}
	 * .
	 */
	@Test
	public final void testConnectStringDoubleDoubleDoubleDouble() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#connect(java.lang.String, int, int, int, int)}
	 * .
	 */
	@Test
	public final void testConnectStringIntIntIntInt() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#connect(java.lang.String)}.
	 */
	@Test
	public final void testConnectString() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#publishByte(java.lang.Integer)}.
	 */
	@Test
	public final void testPublishByte() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#publishDisplay(java.lang.String)}.
	 */
	@Test
	public final void testPublishDisplay() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#read()}.
	 */
	@Test
	public final void testRead() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#read(byte[])}.
	 */
	@Test
	public final void testReadByteArray() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#read(int[])}.
	 */
	@Test
	public final void testReadIntArray() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#read(int[], int)}.
	 */
	@Test
	public final void testReadIntArrayInt() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#readString(int)}.
	 */
	@Test
	public final void testReadStringInt() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#readString(int, int)}.
	 */
	@Test
	public final void testReadStringIntInt() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#intArrayToByteArray(int[])}.
	 */
	@Test
	public final void testIntArrayToByteArray() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#isConnected()}.
	 */
	@Test
	public final void testIsConnected() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#readToDelimiter(java.lang.String)}.
	 */
	@Test
	public final void testReadToDelimiter() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#write(java.lang.String)}.
	 */
	@Test
	public final void testWriteString() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#write(byte[])}.
	 */
	@Test
	public final void testWriteByteArray() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#write(int)}.
	 */
	@Test
	public final void testWriteInt() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#writeFile(java.lang.String)}.
	 */
	@Test
	public final void testWriteFile() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#disconnect()}.
	 */
	@Test
	public final void testDisconnect() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#clear()}.
	 */
	@Test
	public final void testClear() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#createNullModemCable(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testCreateNullModemCable() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#createVirtualUART()}
	 * .
	 */
	@Test
	public final void testCreateVirtualUART() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#isOpen()}.
	 */
	@Test
	public final void testIsOpen() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#refresh()}.
	 */
	@Test
	public final void testRefresh() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#addRelay(java.lang.String, int)}.
	 */
	@Test
	public final void testAddRelay() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#onByte(java.lang.Integer)}.
	 */
	@Test
	public final void testOnByte() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#setBinaryFileFormat(boolean)}.
	 */
	@Test
	public final void testSetBinaryFileFormat() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#parse(byte[], java.lang.String)}.
	 */
	@Test
	public final void testParse() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#setDisplayFormat(java.lang.String)}.
	 */
	@Test
	public final void testSetDisplayFormat() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#getDisplayFormat()}.
	 */
	@Test
	public final void testGetDisplayFormat() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.myrobotlab.service.Serial#available()}.
	 */
	@Test
	public final void testAvailable() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.service.Serial#main(java.lang.String[])}.
	 */
	@Test
	public final void testMain() {
		// fail("Not yet implemented");
	}

}
