package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class AzureTranslator extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(AzureTranslator.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			AzureTranslator translator = (AzureTranslator) Runtime.start("translator", "AzureTranslator");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public AzureTranslator(String n) {
		super(n);
	}
	
	/**
	 * This static method returns all the details of the class without
	 * it having to be constructed.  It has description, categories,
	 * dependencies, and peer definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData(){
		
		ServiceType meta = new ServiceType(AzureTranslator.class.getCanonicalName());
		meta.addDescription("interface to Azure translation services");
		meta.addCategory("translation", "cloud", "ai");		
		meta.addDependency("com.azure.translator");
		return meta;		
	}

	
}
