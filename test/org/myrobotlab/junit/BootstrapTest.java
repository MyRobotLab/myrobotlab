package org.myrobotlab.junit;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.fileLib.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class BootstrapTest {
	
	public final static Logger log = LoggerFactory.getLogger(BootstrapTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public final void testSpawn() {
		// fail("Not yet implemented"); 
		// TODO: implement me!
	}

	@Test
	public final void testExtract() {
		// on this build? but inJar or not
		// fail("Not yet implemented"); // TODO
		// TODO: test extract...
	}
	
	static public void main(String[] args) {
		try {
			// get test for data / create test for data
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);
			
			// so far 
			
			String protectedDomain = Zip.class.getProtectionDomain().getCodeSource().getLocation().toURI().toASCIIString();
			// String protectedDomain = Zip.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			String location = File.class.getResource("/").toString();
			log.info(protectedDomain);
			log.info(location);

			BootstrapTest test = new BootstrapTest();
			test.testExtract();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 

}
