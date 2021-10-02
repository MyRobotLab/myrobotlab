package org.myrobotlab.service.config;

public class ServiceConfig {

  public String name;
  public String type;
  public String locale;
  // public boolean isVirtual = false; - "over configured !" just use Runtime's
  // virtual: config

  // FIXME - is HashMap instead of Map because outbox.notifyList is HashMap too
  // -
  // didn't want to correct it at this time - possibly a serialization problem
  // public HashMap<String, List<MRLListener>> listeners;
  // public ArrayList<String> attach;

}
