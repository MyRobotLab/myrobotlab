package org.myrobotlab.arduino;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.service.interfaces.PinDefinition;

public class PinDefinitions implements Serializable {
  
  private static final long serialVersionUID = 1L;
  Map<String, PinDefinition> pinMap;
  Map<Integer, PinDefinition> pinIndex;
  
  public PinDefinitions(){
    pinMap = new ConcurrentHashMap<String, PinDefinition>();
    pinIndex = new ConcurrentHashMap<Integer, PinDefinition>();
  }
  
  public void put(int address, String name, PinDefinition pinDef){
    pinDef.setAddress(address);
    // custom 'name' of pin e.g. A0 or D7 in Arduino
    pinMap.put(name, pinDef);
    // add additional string address to pinDef
    pinMap.put(String.format("%d", address), pinDef);
    // lastly - put address index 
    pinIndex.put(address, pinDef);
  }
  
  public void clear(){
    pinMap.clear();
    pinIndex.clear();
  }
  
  public PinDefinition get(int address){
    return pinIndex.get(address);
  }
  
  public PinDefinition get(String name){
    return pinMap.get(name);
  }

  public List<PinDefinition> getList() {
    return new ArrayList<PinDefinition>(pinIndex.values());
  }
}
