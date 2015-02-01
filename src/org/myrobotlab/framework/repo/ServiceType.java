package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * list of relations from a Service type to a Dependency key the key is used to
 * look up in the masterList - this keeps the data normalized and if one Service
 * fulfills its dependency and the dependency is shared with another Service
 * type, it is fulfilled there too
 * 
 * The dependency key is the "org" - no version is keyed at the moment.. this
 * would be something to avoid anyway (complexities of cross-versions - jar
 * hell)
 * 
 */
public class ServiceType implements Serializable, Comparator<ServiceType>  {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public int compare(ServiceType o1, ServiceType o2) {
		return o1.name.compareTo(o2.name);
	}

	public String name;	
	public String state = null;
	public Integer workingLevel = null;
	public String description = null;
	public ArrayList<String> dependencies;
	public TreeMap<String, String> peers;

	public ServiceType(){}
	
	public ServiceType(String name) {
		this.name = name;
	}
	
	public void addPeer(String name, String peerType){
		if (peers == null){
			peers = new TreeMap<String, String>();
		}
		peers.put(name, peerType);
	}

	public void addDependency(String org) {
		if (dependencies == null){
			dependencies = new ArrayList<String>();
		}
		dependencies.add(org);
	}

	public int size() {
		return dependencies.size();
	}

	public String get(int index) {
		return dependencies.get(index);
	}

	public String getName() {
		return name;
	}

	public String getSimpleName() {
		if (name == null){
			return null;
		}
		if (name.indexOf(".") == -1){
			return name;
		}
		return name.substring(name.lastIndexOf('.') + 1);
	}
	
	public String toString(){
		return name;
	}

}
