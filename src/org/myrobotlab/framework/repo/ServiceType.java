package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

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
@Root//(name = "serviceDescriptor")
public class ServiceType implements Serializable, Comparator<ServiceType>  {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public int compare(ServiceType o1, ServiceType o2) {
		return o1.name.compareTo(o2.name);
	}

	@Attribute(required = true)
	public String name;
	
	@Attribute(required = false)
	public String state = null;

	@Attribute(required = false)
	public Integer workingLevel = null;

	@Attribute(required = false)
	public String description = null;

	@ElementList(entry="org", required = false, name = "dependencies")
	public ArrayList<String> dependencyList;

	public ServiceType(){}
	
	public ServiceType(String name) {
		this.name = name;
	}

	public void addDependency(String org) {
		if (dependencyList == null){
			dependencyList = new ArrayList<String>();
		}
		dependencyList.add(org);
	}

	public int size() {
		return dependencyList.size();
	}

	public String get(int index) {
		return dependencyList.get(index);
	}

	/* NOT A GOOD METHOD - will add incomplete version info
	 * to dependencies
	public void add(Dependency dep) {
		dependencyList.add(dep.getOrg());
	}
	*/

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
