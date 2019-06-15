package org.myrobotlab.net;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov;
import org.slf4j.Logger;

public class TcpServer {
  
  public final static Logger log = LoggerFactory.getLogger(TcpServer.class);

  boolean listening;
  int port = 32323;
  ServerSocket listener;
  int nThreads = 2; // 20

  public static void main(String[] args) throws Exception {
    
  }
  
  /**
   * Maximum complexity start
   * @param inPort - listening port
   * @param inThreads - number of handler threads
   */
  public void start(Integer inPort, Integer inThreads) {
    try {
      if (inPort != null) {
        port = inPort;
      }
      if (inThreads != null) {
        nThreads = inThreads;
      }
      listener = new ServerSocket(port);
      log.info("started server");
      ExecutorService pool = Executors.newFixedThreadPool(nThreads);
      while (listening) {
        pool.execute(new TcpThread(listener.accept()));
      }
      listener.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class TcpThread implements Runnable {
    private Socket socket;

    TcpThread(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      log.info("Connected: " + socket);
      try {
        Scanner in = new Scanner(socket.getInputStream());
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        while (in.hasNextLine()) {
          out.println(in.nextLine().toUpperCase());
        }
      } catch (Exception e) {
        log.info("Error:" + socket);
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
          log.error("TcpThread threw", e);
        }
        log.info("Closed: " + socket);
      }
    }
  }
}