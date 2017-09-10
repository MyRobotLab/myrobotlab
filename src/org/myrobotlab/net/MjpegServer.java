package org.myrobotlab.net;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         mjpeg server - allows multiple jpeg streams to be sent to multiple
 *         clients extends the most excellent NanoHTTPD server - multi-part mime
 *         was done with little parts borg'd in from -
 *         http://www.servlets.com/cos/
 *         http://www.damonkohler.com/2010/10/mjpeg-streaming-protocol.html
 * 
 */
public class MjpegServer extends NanoHTTPD {

  public class Connection {
    boolean initialized = false;
    Socket socket;
    OutputStream os;

    public Connection(Socket socket) throws IOException {
      this.socket = socket;
      os = socket.getOutputStream();
    }

    public void close() {
      try {
        socket.close();
        socket = null;
      } catch (IOException e) {
      }
    }
  }

  public class VideoWebClient extends Thread {
    String feed;
    ArrayList<Connection> connections = new ArrayList<Connection>();
    BlockingQueue<SerializableImage> videoFeed;

    VideoWebClient(BlockingQueue<SerializableImage> videoFeed, String feed, Socket socket) throws IOException {
      // super(String.format("stream_%s:%s",
      // socket.getInetAddress().getHostAddress(), socket.getPort()));
      super(String.format("stream_%s", feed));
      this.videoFeed = videoFeed;
      this.feed = feed;
      connections.add(new Connection(socket));
    }

    // TODO - look into buffered output stream
    @Override
    public void run() {
      try {
        while (true) {
          SerializableImage frame = videoFeed.take();
          // ++frameIndex;
          // log.info("frame {}", frameIndex);
          Logging.logTime(String.format("Mjpeg frameIndex %d %d", frame.frameIndex, System.currentTimeMillis()));
          for (Iterator<Connection> iterator = connections.iterator(); iterator.hasNext();) {
            Connection c = iterator.next();

            try {

              if (!c.initialized) {
                c.os.write(
                    ("HTTP/1.0 200 OK\r\n" + "Server: YourServerName\r\n" + "Connection: close\r\n" + "Max-Age: 0\r\n" + "Expires: 0\r\n" + "Cache-Control: no-cache, private\r\n"
                        + "Pragma: no-cache\r\n" + "Content-Type: multipart/x-mixed-replace; " + "boundary=--BoundaryString\r\n\r\n").getBytes());
                c.initialized = true;
              }

              byte[] bytes = frame.getBytes();

              // begin jpg
              c.os.write(("--BoundaryString\r\n" + "Content-type: image/jpg\r\n" + "Content-Length: " + bytes.length + "\r\n\r\n").getBytes());
              // write the jpg
              c.os.write(bytes);

              // end
              c.os.write("\r\n\r\n".getBytes());

              // flush or not to flush that is the question
              c.os.flush();
              Logging.logTime(String.format("Mjpeg frameIndex %d %d SENT", frame.frameIndex, System.currentTimeMillis()));
            } catch (Exception e) {
              Logging.logError(e);
              log.info("removing socket");
              iterator.remove();
              c.close();
            }

          } // for each socket
        }
      } catch (Exception e) {
        // FIXME remove socket from list - continue to run
        Logging.logError(e);
      }

    }
  }

  public final static Logger log = LoggerFactory.getLogger(MjpegServer.class.getCanonicalName());

  transient public HashMap<String, BlockingQueue<SerializableImage>> videoFeeds = new HashMap<String, BlockingQueue<SerializableImage>>();

  transient public HashMap<String, VideoWebClient> clients = new HashMap<String, VideoWebClient>();

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      MjpegServer server = new MjpegServer(9090);
      server.start();
      log.info("here");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public MjpegServer(int port) {
    super(port);
  }

  @Override
  public Response serve(String uri, String method, Properties header, Properties parms, Socket socket) {
    log.info(method + " '" + uri + "' ");

    Enumeration e = header.propertyNames();
    while (e.hasMoreElements()) {
      String value = (String) e.nextElement();
      log.info("  HDR: '" + value + "' = '" + header.getProperty(value) + "'");
    }
    e = parms.propertyNames();
    while (e.hasMoreElements()) {
      String value = (String) e.nextElement();
      log.info("  PRM: '" + value + "' = '" + parms.getProperty(value) + "'");
    }

    String feed = null;

    // look for "file" requests
    if (uri.contains(".")) {
      return serveFile(uri, header, new File("."), true);
    }

    int pos0 = uri.lastIndexOf("/");
    if (pos0 != -1) {
      feed = uri.substring(pos0 + 1);
    }

    if (!videoFeeds.containsKey(feed)) {
      StringBuffer response = new StringBuffer(String.format("<html><body align=center>video feeds<br/>", feed));
      for (Map.Entry<String, BlockingQueue<SerializableImage>> o : videoFeeds.entrySet()) {
        // Map.Entry<String,SerializableImage> pairs = o;
        // response.append(String.format("<a href=\"http://%\" >%s</a><br/>",
        // o.getKey()));
        response.append(String.format("<img src=\"%s\" /><br/>%s<br/>", o.getKey(), o.getKey()));
        log.info(o.getKey());
      }
      if (videoFeeds.size() == 0) {
        response.append("no video feed exist - try attaching a VideoSource to the VideoStreamer");
      }
      response.append("</body></html>");
      return new Response(HTTP_OK, MIME_HTML, response.toString());
    } else {
      try {
        VideoWebClient client = new VideoWebClient(videoFeeds.get(feed), feed, socket);
        client.start();
        clients.put(feed, client);
      } catch (IOException e1) {
        Logging.logError(e1);
      }
    }
    // new Response(HTTP_OK, MIME_HTML, "<html><body>Redirected: <a href=\""
    // + uri + "\">" + uri + "</a></body></html>");

    return null; // serveFile(uri, header, new File("."), true);
  }

}
