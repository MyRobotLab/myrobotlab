package org.myrobotlab.webgui;

import java.util.ArrayList;
import java.util.HashMap;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;

@Default
public class ReturnType {
	
	@Element(required=false)
	public Object returnObject;
	
	@ElementArray(name="array",required=false)
	public Object[] array;

	@ElementList(name="arrayList",required=false)
	public ArrayList<Object> arrayList;
		
	@ElementMap(entry = "key", value = "value", attribute = true, inline = true, required = false)
	public HashMap<String, Object> map;
	

}
