package org.myrobotlab.junit;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

public class ZipTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// TODO create a test.jar file
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public final void testExtractFromSelf() throws IOException {
		// TODO: make this work.
		// Zip.extractFromSelf();
	}

	@Test
	public final void testExtractFromSelfString() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testExtractFromSelfStringString() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testExtractFromFileStringString() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testExtractFromFileStringStringString() throws IOException {
		// TODO with and without trailing /
		// TODO root / test ..
		// TODO target ./ test
		// TODO with and without leading /
		// Zip.extractFromFile("./myrobotlab.jar", "./", "resource/framework/root/");
		// Zip.extractFromFile("./myrobotlab.jar", "./resource", "resource");
		// TODO: add an example jar file to the repo and make sure Zip handles it properly.
		// myrobotlab.jar is an artifact from the build.. it's easier to have a test/resource
		// for this ..
	}

	@Test
	public final void testExtractFromResourceStringString() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testExtractFromResourceStringStringString() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testExtract() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testUnzip() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testListDirectoryContents() {
		//fail("Not yet implemented"); 
		// TODO
	}

	@Test
	public final void testCountOccurrences() {
		//fail("Not yet implemented"); 
		// TODO
	}

	static public void main(String[] args) {
		try {
			// get test for data / create test for data
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			ZipTest zipTest = new ZipTest();
			zipTest.testExtractFromFileStringStringString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
