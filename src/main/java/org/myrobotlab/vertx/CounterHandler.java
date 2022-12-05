package org.myrobotlab.vertx;

import java.util.Optional;

import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Vertx;
import org.slf4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;

public class CounterHandler implements Handler<BridgeEvent> {

    public final static Logger log = LoggerFactory.getLogger(Vertx.class);
    private final EventBus eventBus;
    private final CounterRepository repository;

    CounterHandler(EventBus eventBus, CounterRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    @Override
    public void handle(BridgeEvent event) {
        if (event.type() == BridgeEventType.SOCKET_CREATED)
            log.info("A socket was created");

        if (event.type() == BridgeEventType.SEND)
            clientToServer();

        event.complete(true);
    }

    private void clientToServer() {
        Optional<Integer> counter = repository.get();
        if (counter.isPresent()) {
            Integer value = counter.get() + 1;
            repository.update(value);
            eventBus.publish("out", Message.createMessage("vertx", "webxr", "sendLeftController", value));
            //eventBus.publish("out", value);
        } else {
            Integer value = 1;
            repository.update(value);
            eventBus.publish("out", value);
        }
    }
}