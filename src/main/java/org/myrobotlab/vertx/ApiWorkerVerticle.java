package org.myrobotlab.vertx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import rx.Scheduler;
import rx.schedulers.Schedulers;

// Worker verticle to asynchronously process requests
public class ApiWorkerVerticle extends AbstractVerticle {
  
    public final static Logger log = LoggerFactory.getLogger(ApiWorkerVerticle.class);
  
    private Integer         maxDelayMs;
    // private JsonObject      m_config = null;
    private ExecutorService workerExecutor = null;
    private Scheduler       scheduler;
    private AtomicLong      latency = new AtomicLong(0);
    private MessageConsumer<String> workerConsumer = null;
    private ConcurrentHashMap<String, JsonObject> activeTransactions = new ConcurrentHashMap<>();

    public ApiWorkerVerticle() {
        super();
    }

    @Override
    public void start() throws Exception {

      maxDelayMs = 1000; 
      Integer workerPoolSize = 3; //Runtime.getRuntime().availableProcessors() * 2;
      log.info("max_delay_milliseconds={} workerPoolSize={}", maxDelayMs, workerPoolSize);
      workerExecutor = Executors.newFixedThreadPool(workerPoolSize);
      scheduler = Schedulers.from(workerExecutor);
      // identified as a consumer of WORKER events
      workerConsumer = vertx.eventBus().consumer("WORKER");
      workerConsumer.handler(m -> {
          handleRequest(m);
      });      
    }

    private void handleRequest(Message<String> m) {
        JsonObject requestObject = new JsonObject(m.body());
        final String tid = requestObject.getString("tid");
        activeTransactions.put(tid, requestObject);
        requestObject.put("status", TransactionStatus.PENDING.value());
        requestObject.put("type", "transaction-status");
        m.reply(requestObject.encode());
        handleTransaction(tid);
    }

    private void handleTransaction(final String tid) {
        vertx.setTimer(5000, x -> {
            updateTransaction(tid);
        });
    }

    private void updateTransaction(final String tid) {
        JsonObject requestObject = activeTransactions.get(tid);
        if (requestObject != null) {
            TransactionStatus status = TransactionStatus.valueOf(requestObject.getString("status"));
            if (status.ordinal() < TransactionStatus.COMPLETE.ordinal()) {
                TransactionStatus nextStatus = TransactionStatus.values()[status.ordinal() + 1];
                requestObject.put("status", nextStatus.value()); 
                requestObject.put("type", "transaction-status");
                activeTransactions.put(tid, requestObject);
                publishTransaction(requestObject);
                if (nextStatus.ordinal() < TransactionStatus.COMPLETE.ordinal()) {
                    handleTransaction(tid);
                } else {
                    activeTransactions.remove(tid);
                }
            }
        }
    }

    private void publishTransaction(final JsonObject obj) {
        vertx.eventBus().publisher(obj.getString("customer")).write(obj.encode());
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping ApiWorkerVerticle");
        workerExecutor.shutdown();
    }
}