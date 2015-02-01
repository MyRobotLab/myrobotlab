package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.Comparator;

public class Dependency implements Serializable, Comparator<Dependency>  {
	private static final long serialVersionUID = 1L;
	private String org;
	private String revision;
	private boolean resolved = false;
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
		int p = org.lastIndexOf(".");
		if (p == -1){
			return org;
		} else {
			return org.substring(p + 1);
		}
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
		return String.format("%s %s %b", org, revision, resolved);
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

	public void setRevision(String revision2) {
		revision = revision2;
	}

}
