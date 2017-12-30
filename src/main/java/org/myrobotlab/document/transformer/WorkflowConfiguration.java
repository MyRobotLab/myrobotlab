package org.myrobotlab.document.transformer;

import java.util.ArrayList;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class WorkflowConfiguration extends Configuration {

  ArrayList<StageConfiguration> stages;
  private String name = "default";
  private int numWorkerThreads = 1;
  private int queueLength = 50;

  public WorkflowConfiguration(String name) {
    this.name = name;
    stages = new ArrayList<StageConfiguration>();
    // default workflow static config
  }

  public void addStage(StageConfiguration config) {
    stages.add(config);
  }

  public ArrayList<StageConfiguration> getStages() {
    // TODO Auto-generated method stub
    return stages;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getNumWorkerThreads() {
    return numWorkerThreads;
  }

  public void setNumWorkerThreads(int numWorkerThreads) {
    this.numWorkerThreads = numWorkerThreads;
  }

  public int getQueueLength() {
    return queueLength;
  }

  public void setQueueLength(int queueLength) {
    this.queueLength = queueLength;
  }

  public static WorkflowConfiguration fromXML(String xml) {
    // TODO: move this to a utility to serialize/deserialize the config objects.
    // TODO: should override on the impl classes so they return a properly
    // cast config.
    Object o = (new XStream(new StaxDriver())).fromXML(xml);
    return (WorkflowConfiguration) o;
  }

}
