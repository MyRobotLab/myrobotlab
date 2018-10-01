package org.myrobotlab.service;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggingFactory;

/**
 * A base generic test class that can start a service, call a test method on it,
 * and then release the service The idea is you can subclass this and implement
 * the createService() and testService() methods to instrument a unit test for
 * any service in MRL.
 * 
 * @author kwatters
 *
 */
@Ignore
public abstract class AbstractServiceTest {

  // The service object that is created for testing
  private Service service;

  // This method should be subclassed and it should return a service object.
  public abstract Service createService();

  // This is the internal testing method that sequences through the life cycle
  // of the service in the unit test
  @Test
  public void testServiceInternal() throws Exception {
    // set up the logging level ?
    LoggingFactory.init("WARN");
    service = createService();
    testService();
    releaseService();
  };

  // The implementing class will do all it's validation against the "service"
  // object
  public abstract void testService() throws Exception;

  // finally release the service after the unit test has completed.
  public void releaseService() {
    // TODO: what's the normal way to destroy a service?
    service.releaseService();
    // we could potentially do some post checks to make sure the service
    // actually shutdown
  }

}
