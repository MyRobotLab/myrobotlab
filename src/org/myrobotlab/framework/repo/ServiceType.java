package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.ServiceReservation;

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
public class ServiceType implements Serializable, Comparator<ServiceType> {

	private static final long serialVersionUID = 1L;

	public String name;

	public String state = null;
	public Integer workingLevel = null;
	public String description = null;
	public Boolean available = true; // why not ? :P
	/**
	 * dependency keys of with key structure {org}-{version}
	 */
	public HashSet<String> dependencies = new HashSet<String>();
	public HashSet<String> categories = new HashSet<String>();	
	public TreeMap<String, ServiceReservation> peers = new TreeMap<String, ServiceReservation>();
	
	public ServiceType() {
	}

	public ServiceType(String name) {
		this.name = name;
	}

	public void addDependency(String org, String version) {
		dependencies.add(String.format("%s/%s", org, version));		
	}


	@Override
	public int compare(ServiceType o1, ServiceType o2) {
		return o1.name.compareTo(o2.name);
	}

	/*
	public String get(int index) {
		return dependencies.get(index);
	}
	*/

	public String getName() {
		return name;
	}

	public String getSimpleName() {
		if (name == null) {
			return null;
		}
		if (name.indexOf(".") == -1) {
			return name;
		}
		return name.substring(name.lastIndexOf('.') + 1);
	}

	public boolean isAvailable() {
		return (available != null && available == true);
	}

	public int size() {
		return dependencies.size();
	}

	@Override
	public String toString() {
		return name;
	}

	public void addDescription(String description) {
		this.description = description;
	}
	
	public void addCategory(String...categories){
		for (int i = 0; i < categories.length; ++i){
			this.categories.add(categories[i]);
		}
	}
	
	public void addPeer(String name, String peerType, String comment) {
		peers.put(name, new ServiceReservation(name, peerType, comment));
	}
	
	public void sharePeer(String key, String actualName, String peerType, String comment) {		
		peers.put(key, new ServiceReservation(key, actualName, peerType, comment));
	}
	
	public void addRootPeer(String key, String actualName, String peerType, String comment) {		
		peers.put(key, new ServiceReservation(key, actualName, peerType, comment, true));
	}

	public void setAvailable(boolean b) {
		this.available = b;
	}

	public Set<String> getDependencies() {
		return dependencies;
	}

	public TreeMap<String, ServiceReservation> getPeers() {
		return peers;
	}
	
	

}
