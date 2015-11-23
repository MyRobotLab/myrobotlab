package org.myrobotlab.service.interfaces;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;

public abstract class AbstractConnectorTest {

	public abstract AbstractConnector createConnector();
	public abstract MockDocumentListener createListener();
	public abstract void validate(MockDocumentListener listener);
	
	@Test
	public void test() {
		AbstractConnector connector = createConnector();
		MockDocumentListener listener = createListener();
		connector.startService();
		listener.startService();
		connector.addDocumentListener(listener);
		connector.startCrawling();
		// flush any current batch and wait until the outbox is empty.
		connector.flush();
		System.out.println("Done Crawling");
		validate(listener);
		
//		connector.stopService();
//		listener.stopService();
		
		Assert.assertTrue(listener.getCount() > 0);
		
	}
}
