package org.myrobotlab.memory;

public interface MemoryChangeListener {

  // public void onAdd(String parentPath, Node node);
  public void onPut(String parentPath, Node node);

  public void publish(String path, Node node);

}
