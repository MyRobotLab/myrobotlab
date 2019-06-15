package org.myrobotlab.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Serial;
import org.slf4j.Logger;

public class TcpSerialHub implements Runnable {

  private class TcpThread implements Runnable {
    private Socket socket;

    TcpThread(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      log.info("connected: " + socket);
      try {

        InputStream netIn = socket.getInputStream();

        int c;
        while ((c = netIn.read()) != -1) {
          serial.write(c);
        }

      } catch (Exception e) {
        log.info("error:" + socket);
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
        }
        log.info("closed: " + socket);
      }
      
      clients.remove(this);
    }
  }

  public final static Logger log = LoggerFactory.getLogger(TcpSerialHub.class);
  public static void main(String[] args) {
    try {
      TcpSerialHub hub = new TcpSerialHub();
      hub.start();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  ServerSocket listener;
  boolean listening;
  int nThreads = 2; // 20
  transient ExecutorService pool;

  int port = 32323;
  transient Serial serial;

  transient private Thread serverThread;
  
  Set<TcpThread> clients = new HashSet<TcpThread>();

  // Map<String, Socket> socketsx = new HashMap<>();

  public TcpSerialHub() {
  }

  public TcpSerialHub(Integer port) {
    if (port != null) {
      this.port = port;
    }
  }

  public void attach(Serial serial) {
    this.serial = serial;
    //serial.addByteListener(this);
  }

  synchronized public void run() {
    try {
      listener = new ServerSocket(port);
      log.info("started server port {}", port);
      pool = Executors.newFixedThreadPool(nThreads);
      listening = true;
      while (listening) {
        TcpThread client = new TcpThread(listener.accept());
        clients.add(client);
        pool.execute(client);
      }
    } catch (Exception e) {
      log.error("TcpSerialHub threw", e);
    }
    serverThread = null;
    try {
      listener.close();
    } catch (IOException e) {
    }
  }

  public void start() throws IOException {
    start(null, null);
  }

  public void start(Integer port) throws IOException {
    start(port, null);
  }

  /**
   * Maximum complexity start
   * 
   * @param inPort
   *          - listening port
   * @param inThreads
   *          - number of handler threads
   * @throws IOException
   */
  synchronized public void start(Integer inPort, Integer inThreads) throws IOException {
    
    if (inThreads != null) {
      nThreads = inThreads;
    }
    
    if (inPort != null && port != inPort) {
      stop();
      port = inPort;
    }
    
    if (serverThread == null) {
      serverThread = new Thread(this, "tcpserialhub-");
      serverThread.start();
    }
  }

  synchronized public void stop() throws IOException {
    listening = false;
    if (listener != null) {
      listener.close();
    }
    pool.shutdown();
  }

  public void broadcast(Integer newByte) throws IOException {
    for (TcpThread client : clients) {
      OutputStream out = client.socket.getOutputStream();
      out.write(newByte);
    }
  }


}