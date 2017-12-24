package org.myrobotlab.test;

import org.junit.Ignore;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

// TODO: the ant build picks this up and tries to run it.. 
@Ignore
public class TestUtils {

	public static void initEnvirionment() {
		// TODO: there might be other "frameworky" sort of things to init. but for now, we 
		// will just initialize the logger 
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		LoggingFactory.getInstance().setLevel("org.myrobotlab.service.Arduino", Level.WARN);
		LoggingFactory.getInstance().setLevel("org.myrobotlab.service.Runtime", Level.WARN);
		LoggingFactory.getInstance().setLevel("org.myrobotlab.framework.Status", Level.INFO);
	}
}