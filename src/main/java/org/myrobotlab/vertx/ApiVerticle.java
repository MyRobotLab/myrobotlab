package org.myrobotlab.vertx;

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
import io.vertx.ext.web.handler.StaticHandler;

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

    // static file routing - this is from a npm "build" ... but durin gdevelop its far
    // easier to use setupProxy.js from a npm start .. but deployment would be easier with a "build"

    // new UI
    // StaticHandler root = StaticHandler.create("../robotlab-x-app/build/");
    
    // old UI (runtime vs dev time...)
    StaticHandler root = StaticHandler.create("src/main/resources/resource/WebGui/app/");
    root.setCachingEnabled(false);
    root.setDirectoryListing(true);
    root.setIndexPage("index.html");
    // root.setAllowRootFileSystemAccess(true);
    // root.setWebRoot(null);
    
    // VideoStreamHandler video = new VideoStreamHandler(service);
    
    // router.route("/video/*").handler(video);
    router.route("/*").handler(root);    


    // router.get("/health").handler(this::generateHealth);
    // router.get("/api/transaction/:customer/:tid").handler(this::handleTransaction);

    // create the HTTP server and pass the
    // "accept" method to the request handler
    HttpServerOptions httpOptions = new HttpServerOptions();

    if (config.ssl) {
      SelfSignedCertificate certificate = SelfSignedCertificate.create();
      httpOptions.setSsl(true);
      httpOptions.setKeyCertOptions(certificate.keyCertOptions());
      httpOptions.setTrustOptions(certificate.trustOptions());
    }
    httpOptions.setPort(config.port);
    

    HttpServer server = vertx.createHttpServer(httpOptions);
    // TODO - this is where multiple workers would be defined
    // .createHttpServer()
    
    // WebSocketHandler webSocketHandler =  new WebSocketHandler(service);
    // server.webSocketHandler(webSocketHandler);

    // FIXME - don't do "long" or "common" processing in the start()
    // FIXME - how to do this -> server.webSocketHandler(this::handleWebSocket);
    server.webSocketHandler(new WebSocketHandler(service));
    server.requestHandler(router);
    // start servers
    server.listen();
  }
  

  @Override
  public void stop() throws Exception {
    log.info("stopping api verticle");
  }

}
