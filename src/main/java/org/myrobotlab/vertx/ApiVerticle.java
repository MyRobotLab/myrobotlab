package org.myrobotlab.vertx;

import java.util.UUID;

import org.myrobotlab.service.config.VertxConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

/**
 * verticle to handle api requests
 * 
 * @author GroG
 */
public class ApiVerticle extends AbstractVerticle {

  public final static Logger log = LoggerFactory.getLogger(ApiVerticle.class);

  private Router router;

  transient private org.myrobotlab.service.Vertx service;

  public ApiVerticle(org.myrobotlab.service.Vertx service) {
    super();
    this.service = service;
  }

  @Override
  public void start() throws Exception {
    // process configuration and create handlers
    log.info("starting api verticle");
    VertxConfig config = (VertxConfig) service.getConfig();

    // create a router
    router = Router.router(vertx);

    // handle cors requests
    router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.OPTIONS).allowedHeader("Accept").allowedHeader("Authorization")
        .allowedHeader("Content-Type"));

    StaticHandler root = StaticHandler.create(service.getConfig().root);
    root.setCachingEnabled(false);
    root.setDirectoryListing(true);
    root.setIndexPage("index.html");
    
    router.route("/*").handler(root); // FIXME need a map of paths
    
    
    StaticHandler root2 = StaticHandler.create("src/main/resources/resource");
    root2.setCachingEnabled(false);
    root2.setDirectoryListing(true);
    root2.setIndexPage("index.html");
    router.route("/*").handler(root2);

    // VideoStreamHandler video = new VideoStreamHandler(service);
    // router.route("/video/*").handler(video);

    router.route("/api/service/*").handler(new ApiHandler(service));

    // create the HTTP server and pass the
    // "accept" method to the request handler
    HttpServerOptions httpOptions = new HttpServerOptions();

    if (config.ssl) {
      SelfSignedCertificate certificate = SelfSignedCertificate.create();

      // FIXME make/save/install the certificate so its valid on this machine

      httpOptions.setSsl(true);
      httpOptions.setKeyCertOptions(certificate.keyCertOptions());
      httpOptions.setTrustOptions(certificate.trustOptions());
    }
    httpOptions.setPort(config.port);

    HttpServer server = vertx.createHttpServer(httpOptions);
    // TODO - this is where multiple workers would be defined
    // .createHttpServer()

    // WebSocketHandler webSocketHandler = new WebSocketHandler(service);
    // server.webSocketHandler(webSocketHandler);

    // FIXME - don't do "long" or "common" processing in the start()
    // FIXME - how to do this -> server.webSocketHandler(this::handleWebSocket);
    server.webSocketHandler(new WebSocketHandler(service));
    server.requestHandler(router);

    // Create a session handler using LocalSessionStore
    SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));
    router.route().handler(sessionHandler); // Attach the session handler to the
                                            // router

    // start servers
    server.listen();
  }

  @Override
  public void stop() throws Exception {
    log.info("stopping api verticle");
  }

}
