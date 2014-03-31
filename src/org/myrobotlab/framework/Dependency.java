package org.myrobotlab.framework;

import java.io.Serializable;

import org.simpleframework.xml.Element;

/**
 * Keeper of dependency information. Set up to be serialized into XML.
 * 
 * @author GroG
 * 
 */
public class Dependency implements Serializable {
	// TODO these should have getters and setters instead
	@Element
	public String organisation;
	@Element
	public String module;
	@Element
	public String version;
	// TODO - this should be moved into the constructor, does serialization
	// require it to be intialized here?
	@Element
	public boolean resolved = false;
	// TODO - this should be moved into the constructor, does serialization
	// require it to be intialized here?
	@Element
	public boolean released = true;

	/**
	 * Default constructor.
	 */
	public Dependency() {
		this(null, null, null, true);
	}

	/**
	 * Main constructor.
	 * 
	 * @param organisation
	 * @param module
	 * @param version
	 * @param released
	 */
	public Dependency(String organisation, String module, String version, boolean released) {
		this.organisation = organisation;
		this.module = module;
		this.version = version;
		this.released = released;
	}

	/**
	 * Overridden to output information in this object.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append(this.organisation).append(" ").append(this.version);
		return sb.toString();
	}
}
