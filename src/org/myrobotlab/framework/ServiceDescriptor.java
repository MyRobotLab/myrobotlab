package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
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
@Root(name = "serviceDescriptor")
public class ServiceDescriptor implements Serializable {

	private static final long serialVersionUID = 1L;

	@Attribute(required = false)
	public String state = "dev";

	@Attribute(required = false)
	public Integer workingLevel = 0;

	@Element(required = false)
	public String description = "this is a service";

	@ElementList(name = "list")
	public ArrayList<String> dependencyList = new ArrayList<String>();

	public ServiceDescriptor() {
	}

	public ServiceDescriptor(String state) {
		this.state = state;
	}

	public void addDependency(String org) {
		dependencyList.add(org);
	}

	public int size() {
		return dependencyList.size();
	}

	public String get(int index) {
		return dependencyList.get(index);
	}

}
