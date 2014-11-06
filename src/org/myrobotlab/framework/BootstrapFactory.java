package org.myrobotlab.framework;

import org.myrobotlab.service.interfaces.Bootstrap;

public class BootstrapFactory {
	static private Bootstrap bootstrap;
	final static private String packageName = "org.myrobotlab.framework"; 
	
	static public Bootstrap getInstance(){
		if (bootstrap != null){
			return bootstrap;
		}
		Platform platform = Platform.getLocalInstance();
		if (platform.isDalvik()){
			bootstrap = (Bootstrap)createInstance(String.format("%s.BootstrapDalvik", packageName));
		} else {
			bootstrap = (Bootstrap)createInstance(String.format("%s.BootstrapHotSpot", packageName));
		}

		return bootstrap;
	}
	
	static private Object createInstance(String clazz){
		try {
			Class<?> c = Class.forName(clazz);
			//log.info("Loaded class: " + c);
			Object bootstrap = c.newInstance();
			return bootstrap;
		} catch (Exception e) {
			e.printStackTrace();
			//log.error(e.getMessage());
		}
		return null;
	}

}
