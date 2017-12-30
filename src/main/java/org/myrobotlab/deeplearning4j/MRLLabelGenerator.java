package org.myrobotlab.deeplearning4j;

import java.net.URI;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.writable.Writable;

public class MRLLabelGenerator extends ParentPathLabelGenerator {

  @Override
  public Writable getLabelForPath(String path) {
    Writable w = super.getLabelForPath(path);
    //System.out.println("Path:" + path);
    return w;
  }

  @Override
  public Writable getLabelForPath(URI uri) {
    Writable w = super.getLabelForPath(uri);
    //System.out.println("URL: " + uri.toString());
    return w;
  }

}
