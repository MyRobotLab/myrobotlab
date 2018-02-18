package org.myrobotlab.integration;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.opencv.OpenCVFilterDL4J;
import org.myrobotlab.opencv.OpenCVFilterDetector;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;

@Ignore
public class VisionMemoryTest {

	@Test
	public void testVisionMemory() throws InterruptedException, SolrServerException, IOException {

		Runtime.setLogLevel("INFO");

		// for debugging
		Runtime.createAndStart("gui", "SwingGui");

		// start up the embedded solr server
		Solr solr = (Solr)Runtime.createAndStart("solr", "Solr");
		solr.startEmbedded();

		// now start up dl4j
		Deeplearning4j dl4j = (Deeplearning4j)Runtime.createAndStart("dl4j", "Deeplearning4j");
		dl4j.loadVGG16();


		// start up opencv
		OpenCV opencv = (OpenCV)Runtime.createAndStart("opencv", "OpenCV");

		// add the dl4j filter to opencv
		// TODO: add an attach pattern for the opencv filters
		OpenCVFilterDL4J dl4jfilter = new OpenCVFilterDL4J("dl4jfilter");
		opencv.addFilter(dl4jfilter);

		// attach dl4j to solr.
		solr.attach(dl4j);




		// ok. should open cv attach solr? or should solr attach opencv ?
		opencv.capture();

		while (true) {
			Thread.sleep(1000);
		}	

	}
}
