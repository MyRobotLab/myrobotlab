package org.myrobotlab.service;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class SerialTest {


	public final static Logger log = LoggerFactory.getLogger(SerialTest.class);

	static Serial serial = null;
	static TestCatcher catcher = null;
	
	static VirtualDevice virtual = null;
	static Serial uart = null;
	static Python logic = null;
	static String vport = "vport";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		log.info("setUpBeforeClass");

		//Runtime.start("gui", "GUIService");
		serial = (Serial) Runtime.start("serial", "Serial");
		catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
		virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
		virtual.createVirtualPort(vport);
		
		uart = virtual.getUART();
		uart.setTimeout(100);
		
		logic = virtual.getLogic();
		
		serial.connect(vport);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		catcher.clear();
		catcher.isLocal = true;

		uart.clear();
		uart.setTimeout(100);
		
		serial.clear();
		serial.setTimeout(100);
		
		if (!serial.isConnected()){
			serial.connect(vport);
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testGetCategories() {
		assertTrue(serial.getCategories().length > 0);
	}

	@Test
	public final void testGetDescription() {
		assertTrue(serial.getDescription().length() > 0);
	}

	@Test
	public final void testStopService() {
		//fail("Not yet implemented"); // TODO
		// TODO thread count
	}

	// FIXME - remove
	@Test
	public final void testTest() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testBytesToInt() {
		log.info("testBytesToInt");
		
		int x = 0;

		// signed "biggest"
		//x = Serial.bytesToInt(new int[]{127, 255, 255, 255}, 0, 4);
		x = Serial.bytesToInt(new int[]{127, 255, 255, 255}, 0, 4);
		assertEquals(Integer.MAX_VALUE, x);

		x = Serial.bytesToInt(new int[]{0, 0, 0, 255}, 0, 4);
		log.info(String.format("%d", x));
		assertEquals(255, x);

		x = Serial.bytesToInt(new int[]{0, 0, 0, 0}, 0, 4);
		assertEquals(0, x);

		// Java is signed :P - good and bad
		x = Serial.bytesToInt(new int[]{255, 255, 255, 255}, 0, 4);
		assertEquals( -1, x);
		
		x = Serial.bytesToInt(new int[]{0, 0, 1, 0}, 0, 4);
		assertEquals(256, x);

		x = Serial.bytesToInt(new int[]{0, 1, 0, 0}, 0, 4);
		assertEquals(65536, x);

		x = Serial.bytesToInt(new int[]{1, 0, 0, 0}, 0, 4);
		assertEquals(16777216, x);

		/* TODO DO RANGE TESTS :P
		x = Serial.bytesToInt(new int[]{1, 0, 1, 0}, 1, 3);
		assertEquals(1, x);
		*/

	}


	@Test
	public final void testIntArrayToByteArray() {
		//fail("Not yet implemented"); // TODO
	}


	@Test
	public final void testAvailable() throws IOException, InterruptedException {
		
		serial.write(0);
		serial.write(127);
		serial.write(128);
		serial.write(255);
		
		Thread.sleep(100);

		assertEquals(4, uart.available());
	}

	@Test
	public final void testClear() throws IOException, InterruptedException {
		
		
		serial.write(0);
		serial.write(127);
		serial.write(128);
		serial.write(255);
		
		Thread.sleep(100);

		assertEquals(4, uart.available());
		uart.clear();
		assertEquals(0, uart.available());
	}

	@Test
	public final void testConnectString() throws InterruptedException, IOException {
		
		
		log.info("testing connect & disconnect for remote service");
		
		serial.addByteListener(catcher);
		
		if (serial.isConnected()){
			serial.disconnect();
			catcher.checkMsg("onDisconnect",vport);
		}

		catcher.isLocal = false;
		
		serial.connect(vport);
		catcher.checkMsg("onConnect",vport);
		
		serial.write(0);
		serial.write(127);
		serial.write(128);
		serial.write(255);
		
		assertEquals(0, uart.read());
		assertEquals(127, uart.read());
		assertEquals(128, uart.read());
		assertEquals(255, uart.read());
		
		serial.disconnect();
		
		serial.write(255);
		log.info("testing timeout");
		try {
			// timeout makes it throw
			uart.read();
		} catch(Exception e){
			Logging.logError(e);
		}
		
		catcher.checkMsg("onDisconnect", vport);
		serial.removeByteListener(catcher);

		log.info("testing connect & disconnect for local service");
		catcher.isLocal = true;
		
		serial.addByteListener(catcher);
		serial.connect(vport);
		catcher.checkMsg("onConnect",vport);
		
		serial.write(0);
		serial.write(127);
		serial.write(128);
		serial.write(255);
		
		assertEquals(0, uart.read());
		assertEquals(127, uart.read());
		assertEquals(128, uart.read());
		assertEquals(255, uart.read());		
		
		serial.disconnect();
		catcher.checkMsg("onDisconnect",vport);
		serial.removeByteListener(catcher);
		serial.connect(vport);
	}

	@Test
	public final void testConnectStringIntIntIntInt() {
		//fail("Not yet implemented"); // TODO
	}


	@Test
	public final void testConnectFilePlayer() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnectLoopback() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnectPort() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnectTCP() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnectVirtualNullModem() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testConnectVirtualUART() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testCreateHardwarePort() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testCreateTCPPort() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testCreateVirtualPort() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testCreateVirtualUART() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testDisconnect() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetHardwareLibrary() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetListeners() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPort() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPortName() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPortNames() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetPortSource() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetQueue() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetRXCodec() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetRXCodecKey() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetRXCount() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetTimeout() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetTXCodec() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testGetTXCodecKey() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testIsConnected() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testIsRecording() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnByte() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnConnect() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testOnDisconnect() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishConnect() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishDisconnect() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishPortNames() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishRX() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testPublishTX() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRead() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadByteArray() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadInt() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadIntArray() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadLine() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadLineChar() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadString() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadStringChar() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadStringInt() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReadToDelimiter() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRecord() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRecordString() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRecordRX() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testRecordTX() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testReset() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetBufferSize() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetCodec() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetDTR() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetHardwareLibrary() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetRXFormatter() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetTimeout() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testSetTXFormatter() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testStopRecording() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testWriteByteArray() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testWriteInt() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testWriteIntArray() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testWriteString() {
		//fail("Not yet implemented"); // TODO
	}

	@Test
	public final void testWriteFile() {
		//fail("Not yet implemented"); // TODO
	}

}
