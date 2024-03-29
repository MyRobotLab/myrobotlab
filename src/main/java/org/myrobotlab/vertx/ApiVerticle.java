package org.myrobotlab.vertx;

import java.io.FileOutputStream;

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

    // handle cors requests - required for development setupProxy.js
    CorsHandler cors = CorsHandler.create("*");
    cors.allowedMethod(HttpMethod.GET);
    cors.allowedMethod(HttpMethod.DELETE);
    cors.allowedMethod(HttpMethod.PUT);
    cors.allowedMethod(HttpMethod.POST);
    cors.allowedMethod(HttpMethod.OPTIONS);
    cors.allowedHeader("Accept");
    cors.allowedHeader("Authorization");
    cors.allowedHeader("Content-Type");
    router.route().handler(cors);

    // added here before all the file system nonsense
    router.route("/api/service/*").handler(new ApiHandler(service));
    
    // comment this out and its insane performance
    // file routing with merged roots is insanely slow !!!!
    if (service.getConfig().root != null) {

      for (String path : service.getConfig().root) {
        StaticHandler root = StaticHandler.create(path);
        root.setCachingEnabled(true);
        // root.setDirectoryListing(true);  Hellaciously SLOW !!!!
        root.setIndexPage("index.html");
        // root.setCachingEnabled(true);
        // root.setCacheEntryTimeout(0);
        router.route("/*").handler(root); // FIXME need a map of paths
      }

    }
    
//    StaticHandler webgui2 = StaticHandler.create("src/main/resources/resource/Vertx/build");
//    webgui2.setCachingEnabled(true);
//    // root.setDirectoryListing(true);  Hellaciously SLOW !!!!
//    webgui2.setIndexPage("index.html");
//    router.route("/webgui2/*").handler(webgui2);

    // VideoStreamHandler video = new VideoStreamHandler(service);
    // router.route("/video/*").handler(video);


    // create the HTTP server and pass the
    // "accept" method to the request handler
    HttpServerOptions httpOptions = new HttpServerOptions();

    if (config.ssl) {
      SelfSignedCertificate certificate = SelfSignedCertificate.create();
      
      String path = certificate.certificatePath();
      
      // Manually installing the certificate into the system's trust store is required
      // Below are example commands to add the certificate to the trust store on Linux (requires sudo/root access)
      // Replace '/path/to/your/certificate.pem' with the actual path to your certificate file
      
      // # Copy the certificate to the system's trusted certificate location
      // sudo cp /tmp/keyutil_localhost_5417591985169018746.crt /usr/local/share/ca-certificates/your_cert_name.crt
      log.info("sudo cp {} /usr/local/share/ca-certificates/localhost-ts.crt", path);
      log.info("sudo update-ca-certificates ");

      // Add the certificate to the system's trust store
      // You might need to convert it to a format acceptable by the trust store (e.g., .pem to .crt)
      // Example for adding to the system's CA trust store on Linux (may vary for different distributions)
      // sudo cp /path/to/your/certificate.pem /usr/local/share/ca-certificates/your_cert_name.crt
      // sudo update-ca-certificates      

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
