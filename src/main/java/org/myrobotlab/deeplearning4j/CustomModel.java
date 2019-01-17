package org.myrobotlab.deeplearning4j;

import java.util.List;

import org.deeplearning4j.nn.graph.ComputationGraph;

/**
 * This is a simple POJO that contains a DL4J computation graph and it's
 * associated output labels
 * 
 * @author kwatters
 *
 */
public class CustomModel {

  public ComputationGraph model;
  public List<String> labels;

  public CustomModel(ComputationGraph model, List<String> labels) {
    super();
    this.model = model;
    this.labels = labels;
  }

  public ComputationGraph getModel() {
    return model;
  }

  public void setModel(ComputationGraph model) {
    this.model = model;
  }

  public List<String> getLabels() {
    return labels;
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }

}
