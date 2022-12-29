package org.myrobotlab.vertx;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * Verticle to handle API requests
 * 
 * @author GroG
 */
public class ApiVerticle extends AbstractVerticle {

  public final static Logger log = LoggerFactory.getLogger(ApiVerticle.class);

  private Router router;
  private Integer maxDelayMs = 1000;
  private ExecutorService workerExecutor = null;
  private Scheduler scheduler;
  private AtomicLong latency = new AtomicLong(0);
  private Integer workerCount = 1; // FIXME - adjust in VertxConfig
  private AtomicInteger currentWorkers = new AtomicInteger(0);
  private Stack<String> deployedVerticles = new Stack<String>();
  private ConcurrentHashMap<String, ClientConnection> clientConnections = new ConcurrentHashMap<>();
  transient private org.myrobotlab.service.Vertx service;

  public ApiVerticle(org.myrobotlab.service.Vertx service) {
    super();
    this.service = service;
  }

  @Override
  public void start() throws Exception {
    log.info("starting api verticle");

    Integer workerPoolSize = Runtime.getRuntime().availableProcessors() * 2; // m_config.getInteger("worker-pool-size", Runtime.getRuntime().availableProcessors() * 2);
    log.info("maxDelayMs={} workerPoolSize={}", maxDelayMs, workerPoolSize);
    if (workerExecutor == null) {
      workerExecutor = Executors.newFixedThreadPool(workerPoolSize);
    }
    if (scheduler == null) {
      scheduler = Schedulers.from(workerExecutor);
    }
    
    
    // Create a router object.
    router = Router.router(vertx);

    // Handle CORS requests.
    router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.OPTIONS).allowedHeader("Accept").allowedHeader("Authorization")
        .allowedHeader("Content-Type"));

    router.get("/health").handler(this::generateHealth);
    router.get("/api/transaction/:customer/:tid").handler(this::handleTransaction);
    router.route("/static/*").handler(StaticHandler.create());

    // Create the SockJS handler. Specify options.
    SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000).setRegisterWriteHandler(true); // We
                                                                                                                        // need
                                                                                                                        // an
                                                                                                                        // identifier
    SockJSHandler ebHandler = SockJSHandler.create(vertx, options);

    // Our websocket endpoint: /eventbus/
    router.route("/eventbus/*").subRouter(ebHandler.socketHandler(sockJSSocket -> {
      // Extract the identifier
      final String id = sockJSSocket.writeHandlerID();
      // Create an object to map the client socket
      ClientConnection connection = new ClientConnection(id, sockJSSocket, vertx.eventBus());
      // Keep track of open connections
      clientConnections.put(id, connection);
      // Register for end callback
      sockJSSocket.endHandler((Void) -> {
        connection.stop();
        clientConnections.remove(id);
      });
      // Start the connection
      connection.start();
    }));

    // int port = m_config.getInteger("port", 8080);
    int port = 8443;
    // Create the HTTP server and pass the
    // "accept" method to the request handler.

    SelfSignedCertificate certificate = SelfSignedCertificate.create();

    HttpServerOptions httpOptions = new HttpServerOptions();
    httpOptions.setSsl(true);
    httpOptions.setKeyCertOptions(certificate.keyCertOptions());
    httpOptions.setTrustOptions(certificate.trustOptions());
    httpOptions.setPort(port);

    vertx.createHttpServer(httpOptions)
        // .createHttpServer()
        .requestHandler(router).listen(
            // Retrieve the port from the
            // configuration, default to 8080.
            // port,
            result -> {
              if (result.succeeded()) {
                log.info("Listening now on port {}", port);
                deployWorkers(workerCount);
              } else {
                log.error("Failed to listen", result.cause());
              }
            });
  }

  @Override
  public void stop() throws Exception {
    log.info("stopping api verticle");
    workerExecutor.shutdown();
  }
//
//  @Deprecated /* not needed nor wanted */
//  private void processConfig(JsonObject config) {
//    m_config = config;
//    workerCount = m_config.getInteger("worker-count", 1);
//    maxDelayMs = m_config.getInteger("max-delay-milliseconds", 1000);
//    Integer workerPoolSize = m_config.getInteger("worker-pool-size", Runtime.getRuntime().availableProcessors() * 2);
//    log.info("max_delay_milliseconds={} worker_pool_size={}", maxDelayMs, workerPoolSize);
//    if (workerExecutor == null) {
//      workerExecutor = Executors.newFixedThreadPool(workerPoolSize);
//    }
//    if (scheduler == null) {
//      scheduler = Schedulers.from(workerExecutor);
//    }
//  }
//
//  @Deprecated
//  private void processConfigChange(JsonObject prev, JsonObject current) {
//    if (prev.getInteger("worker-count", 1) != current.getInteger("worker-count", 1)) {
//      workerCount = current.getInteger("worker-count", 1);
//      deployWorkers(workerCount);
//    }
//  }


  private void handleTransaction(RoutingContext rc) {
    HttpServerResponse response = rc.response();
    String customer = rc.pathParam("customer");
    String tid = rc.pathParam("tid");
    long startTS = System.nanoTime();

    JsonObject requestObject = new JsonObject();
    requestObject.put("tid", tid);
    requestObject.put("customer", customer);
    requestObject.put("status", TransactionStatus.PENDING.value());
    vertx.eventBus().request("WORKER", requestObject.encode(), result -> {
      if (result.succeeded()) {
        String resp = result.result().body().toString();
        log.info("Sending response for request {} {}", tid, resp);
        latency.addAndGet((System.nanoTime() - startTS) / 1000000);
        if (response.closed() || response.ended()) {
          log.info("response closed");
          return;
        }
        response.setStatusCode(201).putHeader("content-type", "application/json; charset=utf-8").end(resp);
      } else {
        log.info("Sending error response for request {}", tid);
        latency.addAndGet((System.nanoTime() - startTS) / 1000000);
        if (response.closed() || response.ended()) {
          return;
        }
        response.setStatusCode(404).putHeader("content-type", "application/json; charset=utf-8").end();
      }
    });

  }

  public void generateHealth(RoutingContext ctx) {
    ctx.response().setChunked(true).putHeader("Content-Type", "application/json;charset=UTF-8").putHeader("Access-Control-Allow-Methods", "GET")
        .putHeader("Access-Control-Allow-Origin", "*").putHeader("Access-Control-Allow-Headers", "Accept, Authorization, Content-Type").setStatusCode(HttpResponseStatus.OK.code())
        .write((new JsonObject().put("status", "OK")).encode());
  }

  private void deployWorkers(int count) {
    if (count > currentWorkers.get()) {
      while (count > currentWorkers.get()) {
        addWorker();
      }
    } else if (count < currentWorkers.get()) {
      while (count < currentWorkers.get()) {
        removeWorker();
      }
    }
  }

  private void addWorker() {
    currentWorkers.incrementAndGet();
    JsonObject config = new JsonObject().put("instance", currentWorkers.get());
    DeploymentOptions workerOpts = new DeploymentOptions().setConfig(config).setWorker(true).setInstances(1).setWorkerPoolSize(1);
    vertx.deployVerticle(ApiWorkerVerticle.class.getName(), workerOpts, res -> {
      if (res.failed()) {
        log.error("Failed to deploy worker verticle {}", ApiWorkerVerticle.class.getName(), res.cause());
      } else {
        String depId = res.result();
        deployedVerticles.add(depId);
        log.info("Deployed verticle {} DeploymentID {}", ApiWorkerVerticle.class.getName(), depId);
      }
    });
  }

  private void removeWorker() {
    currentWorkers.decrementAndGet();
    String id = deployedVerticles.pop();
    log.info("Undeploying ID {} #WorkerVerticles {}", id, currentWorkers.get());
    vertx.undeploy(id);
  }

}
