package org.myrobotlab.framework.repo;

import java.util.ArrayList;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name="ServiceData")
public class ServiceDataLoader {
	
	@ElementList(entry = "serviceType", inline = false, required = false)
	public ArrayList<ServiceType> serviceTypes = new ArrayList<ServiceType>();

	@ElementList(entry = "category", inline = false, required = false)
	public ArrayList<Category> categories = new ArrayList<Category>();
	
	@ElementList(name = "thirdPartyLibs", entry = "lib", inline = false, required = false)
	public ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

	
	public ServiceDataLoader(){
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
