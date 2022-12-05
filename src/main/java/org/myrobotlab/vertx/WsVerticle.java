package org.myrobotlab.vertx;


import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.VertxMeta;
import org.slf4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class WsVerticle extends AbstractVerticle {

    public final static Logger log = LoggerFactory.getLogger(VertxMeta.class);

    @Override
    public void start() {
        Router router = Router.router(vertx);

        // router.route("/eventbus/*").handler(eventBusHandler());
        // https://github.com/vert-x3/vertx-web/issues/462
        router.mountSubRouter("/api", auctionApiRouter());
        router.route().failureHandler(errorHandler());
        router.route().handler(staticHandler());
        
        vertx.createHttpServer().requestHandler(router).listen(8080);
        // .requestHandler(router::accept).listen(8080);
    }

//    private SockJSHandler eventBusHandler() {
//       // BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddressRegex("auction\\.[0-9]+"));
//        SockJSBridgeOptions options = new SockJSBridgeOptions();
//        return SockJSHandler.create(vertx).bridge(options, event -> {
//            if (event.type() == BridgeEventType.SOCKET_CREATED) {
//                log.info("A socket was created");
//            }
//            event.complete(true);
//        });
//    }

    private Router auctionApiRouter() {
        AuctionRepository repository = new AuctionRepository(vertx.sharedData());
        AuctionValidator validator = new AuctionValidator(repository);
        AuctionHandler handler = new AuctionHandler(repository, validator);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route().consumes("application/json");
        router.route().produces("application/json");

        router.route("/auctions/:id").handler(handler::initAuctionInSharedData);
        router.get("/auctions/:id").handler(handler::handleGetAuction);
        router.patch("/auctions/:id").handler(handler::handleChangeAuctionPrice);

        return router;
    }

    private ErrorHandler errorHandler() {
        return ErrorHandler.create(vertx);
    }

    private StaticHandler staticHandler() {
        return StaticHandler.create()
            .setCachingEnabled(false);
    }
}
