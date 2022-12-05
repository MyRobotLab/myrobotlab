package org.myrobotlab.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class WsServer extends AbstractVerticle {

//    Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new WsServer());
    }

    private Integer port;

    private Router eventBusHandler() {

//        BridgeOptions options = new BridgeOptions()
//                .addOutboundPermitted(new PermittedOptions().setAddressRegex("out"))
//                .addInboundPermitted(new PermittedOptions().setAddressRegex("in"));
        SockJSBridgeOptions options = new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddressRegex("out"))
                .addInboundPermitted(new PermittedOptions().setAddressRegex("in"));

        SharedData data = vertx.sharedData();
        CounterRepository repository = new CounterRepository(data);
        EventBus eventBus = vertx.eventBus();
        CounterHandler counterHandler = new CounterHandler(eventBus, repository);

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        return sockJSHandler.bridge(options, counterHandler);
        // return sockJSHandler;
    }

    private StaticHandler staticHandler() {
        return StaticHandler.create()
                .setCachingEnabled(false);
    }

    public String getDeploymentId() {
        if (context != null) {
            return context.deploymentID();
        }
        return null;
    }

    @Override
    public void start() throws Exception {

        /*
         * vertx.createHttpServer().webSocketHandler(ws ->
         * ws.handler(ws::writeBinaryMessage)).requestHandler(req -> {
         * System.out.println(String.format("bytes read %d", req.bytesRead()));
         * 
         * if (req.uri().equals("/")) {
         * req.response().sendFile("ws.html");
         * }
         * }).listen(8080);
         */

//        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
//        SockJSBridgeOptions options = new SockJSBridgeOptions();

        Router router = Router.router(vertx);

        router.route("/eventbus/*").subRouter(eventBusHandler());// .handler(eventBusHandler());
        router.route().handler(staticHandler());

//        .setSSL(true).setKeyStorePath("server-keystore.jks")
//        .setKeyStorePassword("wibble");" 

        final var certificate = SelfSignedCertificate.create();

        HttpServerOptions options = new HttpServerOptions();
        options.setSsl(true);
        options.setKeyCertOptions(certificate.keyCertOptions());
        options.setTrustOptions(certificate.trustOptions());
        options.setPort(port);
//        .setKeyStorePath("server-keystore.jks")
//        .setKeyStorePassword("changeme")

        vertx.createHttpServer(options).requestHandler(router).listen();
        // .requestHandler(router::accept)
        // .listen(8080);
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
