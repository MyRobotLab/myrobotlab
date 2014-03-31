package org.myrobotlab.pickToLight;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Controller {

	private String name;
	private String version;
	private String ipAddress;
	private String macAddress;
	
	private ConcurrentHashMap <String, Module> modules = new ConcurrentHashMap <String, Module>();
	
	public Controller(){
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public ConcurrentHashMap<String, Module> getModules() {
		return modules;
	}

	public void setModules(ConcurrentHashMap<String, Module> modules) {
		this.modules = modules;
	}

}
