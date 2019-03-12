package org.myrobotlab.integration;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;
import org.myrobotlab.test.AbstractTest;

@Ignore
public class YoloFilterTest extends AbstractTest {

  @Test
  public void testYolo() throws SolrServerException, IOException {

    Runtime.start("gui", "SwingGui");

    Solr solr = (Solr) Runtime.createAndStart("solr", "Solr");
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