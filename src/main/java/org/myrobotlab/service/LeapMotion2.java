package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.WsClient;
import org.myrobotlab.service.config.LeapMotion2Config;
import org.myrobotlab.service.data.LeapData;
import org.myrobotlab.service.interfaces.ConnectionEventListener;
import org.myrobotlab.service.interfaces.LeapDataListener;
import org.myrobotlab.service.interfaces.LeapDataPublisher;
import org.myrobotlab.service.interfaces.PointPublisher;
import org.myrobotlab.service.interfaces.RemoteMessageHandler;
import org.slf4j.Logger;

import okhttp3.Response;
import okhttp3.WebSocket;

public class LeapMotion2 extends Service<LeapMotion2Config> implements LeapDataListener, LeapDataPublisher, PointPublisher, RemoteMessageHandler {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(LeapMotion2.class);

  final transient LeapWsListener poller;

  public LeapData lastLeapData = null;

  int numFrames = 0;

  public LeapMotion2(String n, String id) {
    super(n, id);
    poller = new LeapWsListener(this);
  }

  public void addFrameListener(Service service) {
    addListener("publishFrame", service.getName(), "onFrame");
  }

  public void addLeapDataListener(Service service) {
    addListener("publishLeapData", service.getName(), "onLeapData");
  }

  public void checkPolicy() {
    log.info("controller.policyFlags()");
  }

  @Override
  public LeapData onLeapData(LeapData data) {
    return data;
  }

  @Override
  public LeapData publishLeapData(LeapData data) {
    return data;
  }

  // FIXME - make POJO or don't compute other useful values .. etc.
  public String publishLeapDataJson(String data) {
    // TODO fill in a LeapData ! :)
    return data;
  }

  @Override
  public void releaseService() {
    poller.stop();
    super.releaseService();
  }

  @Override
  public void startService() {
    super.startService();
    LeapMotion2Config c = (LeapMotion2Config) config;
    if (c.tracking) {
      startTracking();
    }
  }

  public void startTracking() {
    poller.start();
  }

  public void stopTracking() {
    poller.stop();
  }

  @Override
  public List<Point> publishPoints(List<Point> points) {
    return points;
  }

  public void addPointsListener(Service s) {
    // TODO - reflect on a public heard method - if doesn't exist error ?
    addListener("publishPoints", s.getName(), "onPoints");
  }

  public class LeapWsListener implements ConnectionEventListener {

    private transient LeapMotion2 service;

    WsClient websocket;

    public LeapWsListener(LeapMotion2 service) {
      this.service = service;
    }

    public void stop() {
      try {
        websocket.close();
      } catch (Exception e) {
        service.error(e);
      }
    }

    public void start() {
      try {

        websocket = new WsClient();
        LeapMotion2Config c = (LeapMotion2Config) config;
        websocket.connect(this, c.websocketUrl);

      } catch (Exception e) {
        service.error(e);
      }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      if (service.isRunning()) {
        LeapMotion2Config c = (LeapMotion2Config) config;
        info("could not connect to ... %s", c.websocketUrl);
        sleep(5000);
        websocket.connect(this, c.websocketUrl);
      }
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);
    try {

      // leap.startService();
      // Runtime.start("webgui", "WebGui");
      Runtime.start("leap", "LeapMotion2");
      // Runtime.start("intro", "Intro");
      // Runtime.start("i01", "InMoov2");

      // Have the sample listener receive events from the controller

      // Remove the sample listener when done
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void onRemoteMessage(String uuid, String data) {
    invoke("publishLeapDataJson", data);
  }

}
