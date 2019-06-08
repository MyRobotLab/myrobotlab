package org.myrobotlab.net;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {

  boolean listening;
  int port = 323232;

  public static void main(String[] args) throws Exception {
    try {
      ServerSocket listener = new ServerSocket(59898);
      System.out.println("The capitalization server is running...");
      ExecutorService pool = Executors.newFixedThreadPool(2);// (20);
      while (listening) {
        pool.execute(new TcpThread(listener.accept()));
      }
      listener.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void start(int  port) {
    
  }

  private static class TcpThread implements Runnable {
    private Socket socket;

    TcpThread(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      System.out.println("Connected: " + socket);
      try {
        Scanner in = new Scanner(socket.getInputStream());
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        while (in.hasNextLine()) {
          out.println(in.nextLine().toUpperCase());
        }
      } catch (Exception e) {
        System.out.println("Error:" + socket);
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
        }
        System.out.println("Closed: " + socket);
      }
    }
  }
}