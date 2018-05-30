package org.myrobotlab.integration;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;

public class YoloFilterTest {

  public static void main(String[] args) throws SolrServerException, IOException {
    
    Runtime.start("gui", "SwingGui");
    
    Solr solr = (Solr)Runtime.createAndStart("solr", "Solr");
    solr.startEmbedded();
    
    OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
    opencv.setStreamerEnabled(false);
    opencv.setCameraIndex(0);
    

    OpenCVFilterYolo yolo = new OpenCVFilterYolo("yolo");
    opencv.addFilter(yolo);

    solr.attach(opencv); 
    opencv.capture();
    
  }

}
