package org.myrobotlab.serial;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;
import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

public class PortQueueTest implements SerialDataListener {

  transient public final static Logger log = LoggerFactory.getLogger(PortQueueTest.class);

  @Test
  public void testPortQueue() throws IOException {
    // Test a port queue.
    String portName = "vCOMPort";
    BlockingQueue<Integer> rx = new LinkedBlockingQueue<Integer>();
    BlockingQueue<Integer> tx = new LinkedBlockingQueue<Integer>();
    PortQueue portQueue = new PortQueue(portName, rx, tx);

    // TODO: use the port queue to do something.
    portQueue.listen(this);
    assertFalse(portQueue.isOpen());
    portQueue.open();
    assertTrue(portQueue.isOpen());
    portQueue.close();
    assertFalse(portQueue.isOpen());
    portQueue.open();
    assertTrue(portQueue.isOpen());

  }

  @Override
  public String getName() {
    return "PortQueueTest";
  }

  @Override
  public QueueStats publishStats(QueueStats stats) {
    // NoOp for this test.
    return null;
  }

  @Override
  public void updateStats(QueueStats stats) {
    // NoOp for this test
  }

  @Override
  public void onConnect(String portName) {
    // TODO Auto-generated method stub
    // call back notification, we should track something here
    log.info("On Connect");
  }

  @Override
  public void onDisconnect(String portName) {
    // TODO Auto-generated method stub
    // callback notification, we should track something here.
    log.info("On Disconnect");
  }

  @Override
  public void onBytes(byte[] bytes) {
    // TODO Auto-generated method stub
    // ?!?! what do we do with this? on bytes for which queue?!
    log.info("On Bytes");
  }

}
