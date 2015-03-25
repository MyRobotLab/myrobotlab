package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.Comparator;

public class Dependency implements Serializable, Comparator<Dependency> {
	private static final long serialVersionUID = 1L;
	private String org;
	private String revision;
	private boolean installed = false;

	public Dependency() {
		this(null, null, true);
	}

	public Dependency(String organisation, String version) {
		this.org = organisation;
		this.revision = version;
	}

	public Dependency(String organisation, String version, boolean released) {
		this.org = organisation;
		this.revision = version;
	}

	@Override
	public int compare(Dependency o1, Dependency o2) {
		return o1.getOrg().compareTo(o2.getOrg());
	}

	public String getModule() {
		if (org == null) {
			return null;
		}
		int p = org.lastIndexOf(".");
		if (p == -1) {
			return org;
		} else {
			return org.substring(p + 1);
		}
	}

	public String getOrg() {
		return org;
	}

	public String getRevision() {
		return revision;
	}

	public boolean isInstalled() {
		return installed;
	}

	public boolean isResolved() {
		return installed;
	}

	public void setResolved(boolean b) {
		installed = b;
	}

	public void setRevision(String revision2) {
		revision = revision2;
	}

	@Override
	public String toString() {
		return String.format("%s %s %b", org, revision, installed);
	}

}
