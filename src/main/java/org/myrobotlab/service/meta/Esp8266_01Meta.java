package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Esp8266_01Meta extends MetaData {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Esp8266_01Meta.class);

	/**
	 * This class is contains all the meta data details of a service. It's peers,
	 * dependencies, and all other meta data related to the service.
	 * 
	 */
	public Esp8266_01Meta() {

		Platform platform = Platform.getLocalInstance();
		addDescription("ESP8266-01 service to communicate using WiFi and i2c");
		addCategory("i2c", "control");
		setSponsor("Mats");
		// FIXME - add HttpClient as a peer .. and use its interface .. :)
		// then remove direct dependencies to httpcomponents ...
		// One HttpClient to Rule them all !!
		/*
		 * Runtime currently includes these dependencies
		 * addDependency("org.apache.httpcomponents", "httpclient", "4.5.2");
		 * addDependency("org.apache.httpcomponents", "httpcore", "4.4.6");
		 */

	}

}
