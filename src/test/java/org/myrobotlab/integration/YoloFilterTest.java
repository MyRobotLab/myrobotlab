package org.myrobotlab.integration;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;

public class YoloFilterTest {

  public static void main(String[] args) throws SolrServerException, IOException {
    // TODO Auto-generated method stub

    
    Runtime.start("gui", "SwingGui");
    org.apache.log4j.BasicConfigurator.configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);
    
    
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
