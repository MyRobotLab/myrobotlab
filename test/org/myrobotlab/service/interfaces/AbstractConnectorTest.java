package org.myrobotlab.service.interfaces;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

@Ignore
public abstract class AbstractConnectorTest {

  public final static Logger log = LoggerFactory.getLogger(AbstractConnectorTest.class);

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

    // Wait for the inbox to drain
    while (listener.getInbox().size() > 0) {
      log.info("Draining Inbox...Size: {}", listener.getInbox().size());

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    validate(listener);

    // connector.stopService();
    // listener.stopService();

    Assert.assertTrue(listener.getCount() > 0);

  }
}
