package org.myrobotlab.vertx;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConnection {
    private Logger logger;
    private final String id;
    private final SockJSSocket socket;

    private MessageConsumer<String> consumer = null;
    private EventBus eventBus = null;

    public ClientConnection(final String writeHandlerID,
                            final SockJSSocket socket,
                            final EventBus eventBus)
    {
        this.logger = LoggerFactory.getLogger("CC");
        this.id = writeHandlerID; 
        this.socket = socket;
        this.eventBus = eventBus;
    } 

    public void start() {
        socket.handler(
            buffer -> {
                String message = buffer.toString();
                handleMessage(message);
            }
        );
    }

    private void handleMessage(final String messageStr) {
        logger.info("Receive: {}", messageStr);
        JsonObject messageObj = new JsonObject(messageStr);
        String messageType = messageObj.getString("type");
        if (messageType.equalsIgnoreCase("listen")) {
            setupListener(messageObj.getString("customer"));
        }
    }

    private void setupListener(final String customer) {
        if (consumer == null) {
            consumer = eventBus.consumer(customer);
            consumer.handler(event -> {
                socket.write(event.body());
            });
            logger.info("Listener created");
        }
        JsonObject obj = new JsonObject();
        obj.put("type", "listen-ack");
        obj.put("customer", customer);
        sendMessage(obj.encode());
    }

    private void sendMessage(final String message) {
        socket.write(message);
    }

    public void stop() {}
}
