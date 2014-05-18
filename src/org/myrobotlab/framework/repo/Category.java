package org.myrobotlab.framework.repo;

import java.util.ArrayList;
import java.util.Comparator;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class Category implements Comparator<Category> {
	@Attribute(required=false)
	public String name;
	@Attribute(required=false)
	public String description;
	@ElementList(entry = "serviceTypes", inline = true, required = false)
	public ArrayList<String> serviceTypes = new ArrayList<String>();
	@Override
	public int compare(Category o1, Category o2) {
		return o1.name.compareTo(o2.name);
	}
}
