package org.myrobotlab.integration;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Ignore;
import org.junit.Test;
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
		
		Solr solr = (Solr)Runtime.createAndStart("solr", "Solr");
		solr.startEmbedded();
		
		OpenCV opencv = (OpenCV)Runtime.createAndStart("opencv", "OpenCV");
		solr.attach(opencv);
		
		// ok. should open cv attach solr? or should solr attach opencv ?
		opencv.capture();
		
		while (true) {
			Thread.sleep(1000);
		}	
		
	}
}
