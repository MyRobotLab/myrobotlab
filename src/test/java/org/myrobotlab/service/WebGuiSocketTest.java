package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebGuiSocketTest {

  protected final static Logger log = LoggerFactory.getLogger(WebGuiSocketTest.class);

  protected WebSocket webSocket;
  protected WebSocketListener webSocketListener;
  protected BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>();
  protected WebGui webgui2;

  @Before
  public void setUp() {
    webgui2 = (WebGui) Runtime.create("webgui2", "WebGui");
    webgui2.autoStartBrowser(false);
    webgui2.setPort(8889);
    webgui2.startService();

    Service.sleep(3);
    OkHttpClient okHttpClient = new OkHttpClient();
    Request request = new Request.Builder()
        .url("ws://localhost:8889/api/messages?user=root&pwd=pwd&session_id=2309adf3dlkdk&id=webgui-client").build();
    webSocketListener = new WebSocketListener() {
      @Override
      public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        // WebSocket connection is established
        log.info("onOpen");
      }

      @Override
      public void onMessage(WebSocket webSocket, String msg) {
        log.info("onMessage {}", msg);
        msgQueue.add(msg);
      }

      @Override
      public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        // Handle WebSocket failure
        log.info("onFailure");
      }
    };

    webSocket = okHttpClient.newWebSocket(request, webSocketListener);
  }

  @After
  public void teardown() {
    webSocket.cancel();
  }

  @Test
  public void testWebSocketConnection() throws InterruptedException {
    // Use a CountDownLatch to wait for the WebSocket connection to be
    // established
    // CountDownLatch latch = new CountDownLatch(1);
    // webSocket.listener().onOpen(webSocket, null);
    // Wait for the connection to be established
    // assertTrue("WebSocket connection timeout", latch.await(5, TimeUnit.SECONDS));
    
    
    // if sucessfully connected we'll get an 
    // 1. addListener from its runtime for describe
    // 2. then a describe is sent with a parameter that describes the requesting platform
    
    String json = msgQueue.poll(5, TimeUnit.SECONDS);
    LinkedHashMap<String, Object>msg = (LinkedHashMap<String, Object>)CodecUtils.fromJson(json);
    assertEquals("runtime", msg.get("name"));
    assertEquals("addListener", msg.get("method"));
    Object data = msg.get("data");
    List<Object> p0 = (List<Object>)msg.get("data");
    MRLListener listener = CodecUtils.fromJson((String)p0.get(0), MRLListener.class);
    assertEquals("describe", listener.topicMethod);
    assertEquals("onDescribe", listener.callbackMethod);
    assertEquals(String.format("runtime@%s", webgui2.getId()), listener.callbackName);
    
    // the client can optionally do the same thing
    // send an addListener for describe
    // then send a describe
    
    String addListener = "{\n"
    + "    \"msgId\": 1690173331106,\n"
    + "    \"name\": \"runtime\",\n"
    + "    \"method\": \"addListener\",\n"
    + "    \"sender\": \"runtime@p1\",\n"
    + "    \"sendingMethod\": \"sendTo\",\n"
    + "    \"data\": [\n"
    + "        \"\\\"describe\\\"\",\n"
    + "        \"\\\"runtime@p1\\\"\"\n"
    + "    ],\n"
    + "    \"encoding\": \"json\"\n"
    + "}";
    
    // FIXME - make describe 
    // String describe = 
    
    // assert describe info
    
    //.info(json);
    log.info("here");
    
  }

  @Test
  public void testWebSocketMessage() throws InterruptedException {
    // Use a CountDownLatch to wait for the WebSocket message
//    CountDownLatch latch = new CountDownLatch(1);
//
//    String expectedMessage = "Hello, WebSocket!";
//    webSocket.listener().onMessage(webSocket, expectedMessage);
//
//    // Wait for the message to be received
//    assertTrue("WebSocket message timeout", latch.await(5, TimeUnit.SECONDS));
  }

}
