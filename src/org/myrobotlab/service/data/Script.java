package org.myrobotlab.service.data;

import java.io.File;
import java.io.Serializable;

public class Script implements Serializable {
	static final long serialVersionUID = 1L;
	/**
	 * unique location &amp; key of the script
	 * e.g. /mrl/scripts/myScript.py
	 */
	File file;
	/**
	 * actual code/contents of the script
	 */
	String code;

	public Script(String name, String script) {
		this.file = new File(name);
		// DOS2UNIX line endings.
		// This seems to get triggered when people use editors that don't do
		// the cr/lf thing very well..
		// TODO:This will break python quoted text with the """ syntax in
		// python.
		if (script != null) {
			script = script.replaceAll("(\r)+\n", "\n");
		}
		this.code = script;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		if (file == null){
			return null;
		}
		// FIXME - display name 
		return file.getName();
	}
	
	public String getDisplayName() {
		if (file == null){
			return null;
		}
		// FIXME - display name 
		return file.getName();
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setName(String name) {
		// FIXME - logic for setting file ?
		this.file = new File(name);
	}
}

