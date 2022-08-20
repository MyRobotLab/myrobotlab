package org.myrobotlab.service;

import org.atmosphere.cpr.AtmosphereFramework.AtmosphereHandlerWrapper;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.test.AbstractTest;

//TODO: re-enable this test when we figure out why it fails from the
// command line ant build...

@Ignore
public class WebGuiTest extends AbstractTest {


  static public void main(String[] args) {

    try {
      // // LoggingFactory.init();
      // FIXME - base class static method .webGui() & .gui()
      // Runtime.start("webgui", "WebGui");
      // Runtime.start("gui", "SwingGui");


      WebGuiTest test = new WebGuiTest();
      WebGuiTest.setUpBeforeClass();


      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(WebGuiTest.class);
      log.info("Result was: {}", result);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void testApi() throws Exception {
    
//    AtmosphereHandlerWrapper handlerWrapper = map(req);
//    AtmosphereResourceImpl resource = new AtmosphereResourceImpl(config,handlerWrapper.broadcaster, req, res, this, handlerWrapper.atmosphereHandler);
//    handlerWrapper.broadcaster.getBroa
//    
//    TesterBroadcaster broadcaster = (TesterBroadcaster) eventBus.getBroadcaster();
//    AtmosphereResource atmosphereResource = new AtmosphereResourceImpl();
//    AtmosphereRequest atmosphereRequest = AtmosphereRequest.wrap(wicketTester.getRequest());
//    AtmosphereResponse atmosphereResponse = AtmosphereResponse.wrap(wicketTester.getResponse());
//    TesterAsyncSupport asyncSupport = new TesterAsyncSupport();
//    atmosphereResource.initialize(broadcaster.getApplicationConfig(), broadcaster, atmosphereRequest, atmosphereResponse, asyncSupport, new AtmosphereHandlerAdapter());
//    atmosphereResource.setBroadcaster(broadcaster);
//    broadcaster.addAtmosphereResource(atmosphereResource);
//    String uuid = atmosphereResource.uuid();
//    Page page = getComponent().getPage();
//    page.setMetaData(ATMOSPHERE_UUID, uuid);
//    eventBus.registerPage(uuid, page);
//    
//    AtmosphereResourceImpl ar = new AtmosphereResourceImpl();
//    AtmosphereRequestImpl request = new AtmosphereRequestImpl(null);
//    ar.session(true);
//    ar.initialize(null, null, null, null, null, null);
//    WebGui webgui = (WebGui) Runtime.start("webgui", "WebGui");
//    webgui.handle((AtmosphereResource)ar);
  }

}
