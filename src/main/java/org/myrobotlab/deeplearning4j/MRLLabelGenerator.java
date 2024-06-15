package org.myrobotlab.deeplearning4j;

import java.net.URI;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.writable.Writable;

public class MRLLabelGenerator extends ParentPathLabelGenerator {

  private static final long serialVersionUID = 1L;

  @Override
  public Writable getLabelForPath(String path) {
    Writable w = super.getLabelForPath(path);
    return w;
  }

  @Override
  public Writable getLabelForPath(URI uri) {
    Writable w = super.getLabelForPath(uri);
    return w;
  }

}
