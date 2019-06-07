package org.myrobotlab.joystick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Controller {

  public final static Logger log = LoggerFactory.getLogger(Controller.class);

  String name;
  int portNumber;
  String portType;
  String type;
  List<Component> components = new ArrayList<Component>();
  Map<String, Component> componentMap = new HashMap<String, Component>();
  transient net.java.games.input.Controller jinputController = null;
  String serviceName;
  
  // FIXME use transient if needed

  public Component[] getComponents() {
    return components.toArray(new Component[components.size()]);
  }

  public boolean poll() {
    if (jinputController != null) {
      return jinputController.poll();
    } else {
      // virtual always works !
      return true;
    }
  }
  
  public boolean isVirtual() {
    return jinputController == null;
  }

  public String getName() {
    return this.name;
  }

  public Controller(String serviceName, net.java.games.input.Controller controller) {
    this.jinputController = controller;
    this.name = controller.getName();
    this.portNumber = controller.getPortNumber();
    this.portType = (controller.getPortType() != null) ? controller.getPortType().toString() : null;
    this.serviceName = serviceName;
    // this.rumblers = controller.getRumblers();
    this.type = (controller.getType() != null) ? controller.getType().toString() : null;

    net.java.games.input.Component[] components = controller.getComponents();
    for (int i = 0; i < components.length; ++i) {
      net.java.games.input.Component jinputComponent = components[i];
      // Component c = new Component(getName(), i, jinputComponent);
      Component c = new Component(serviceName, i, jinputComponent);
      this.components.add(c);
      this.componentMap.put(jinputComponent.getIdentifier().toString(), c);
    }
  }

  public Map<String, Component> getComponentMap() {
    return componentMap;
  }

  public void setName(String name) {
    this.name = name;
  }

  // needed after deserializing json, because it doesn't know the
  // map index & the list objects point to the same component.
  // TODO - have its own contained serialization
  public void reIndex(String serviceName) {
    this.serviceName = serviceName;
    // clear the index
    componentMap.clear();
    // rebuild map
    for (Component c : components) {
      c.serviceName = serviceName;
      componentMap.put(c.getIdentifier(), c);
    }
  }

}
