package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.Comparator;

import org.simpleframework.xml.Attribute;

public class Dependency implements Serializable, Comparator<Dependency>  {
	private static final long serialVersionUID = 1L;
	@Attribute
	private String org;
	
	@Attribute
	private String revision;
	
	@Attribute
	private boolean resolved = false;
	
	@Attribute
	private boolean released = true;

	public Dependency() {
		this(null, null, true);
	}

	public Dependency(String organisation, String version, boolean released) {
		this.org = organisation;
		this.revision = version;
		this.released = released;
	}
	
	public Dependency(String organisation, String version) {
		this.org = organisation;
		this.revision = version;
	}
	
	@Override
	public int compare(Dependency o1, Dependency o2) {
		return o1.getOrg().compareTo(o2.getOrg());
	}
	
	
	public String getOrg(){
		return org;
	}
	
	public String getModule(){
		if (org == null){
			return null;
		}
		return org.substring(org.lastIndexOf("."));
	}
	public boolean isReleased()
	{
		return released;
	}	
	
	public boolean isResolved()
	{
		return resolved;
	}

	@Override
	public String toString() {
		return String.format("%s %s %s", org, getModule(), revision);
	}

	public void setResolved(boolean b) {
		resolved = b;
	}

	public void setReleased(boolean b) {
		released = b;
	}

	public String getRevision() {
		return revision;
	}
}
