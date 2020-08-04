package org.myrobotlab.client;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.Scanner;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereSocket;
import org.myrobotlab.framework.HelloRequest;
import org.myrobotlab.framework.Message;
import org.myrobotlab.lang.NameGenerator;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

/**
 * This class is a minimal Java websocket client which can attach to a
 * MyRobotLab running instance. From the command line it should be capable of
 * sending any command using the Cli Api notation. There are system commands
 * such as ls, lp, cd, etc - but most invoking of service methods will be of the
 * form
 * 
 * /{service}/{method}/{param0}/{param1}....
 * 
 * @author GroG
 * 
 *         TODO 0. up arrow with history 0.5 route 1. reconnect logic 2. ls
 *         /runtime/ | grep -i registry 3. help 4. help /runtime/getUptime 5.
 *         test /runtime/subscribe/onRegister 6. colorize with pico
 *
 */
@Command(mixinStandardHelpOptions = true, name = "myrobotlab-client.jar", showDefaultValues = true, version = "0.0.1")
public class Client implements Runnable, Decoder<String, Reader>, Encoder<String, Reader> {

  private transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().disableHtmlEscaping().create();

  /**
   * attached mrl process id
   */
  @Option(names = { "-i", "--id" }, description = "Identity of the client")
  static protected String promptId = null;

  /**
   * generated id of this client
   */
  static protected String clientId;

  /**
   * messages api type sends json encoded messages over persistent connections
   */
  protected String apiType = "messages";

  /**
   * default base connection
   */
  @Option(names = { "-c",
      "--connect" }, defaultValue = "http://localhost:8888", description = "Connects to myrobotlab webgui instance with a websocket connection", showDefaultValue = Visibility.ALWAYS)
  protected String base;

  @Option(names = { "-u", "--user" }, description = "user to authenticate")
  protected static String username = null;

  @Option(names = { "-p", "--password" }, description = "password to authenticate")
  String password = null;

  /**
   * Atmosphere client
   */
  transient private AtmosphereClient client = null;

  /**
   * The underlying client runtime engine (Netty)
   */
  transient private AsyncHttpClient asc = null;

  /**
   * current working directory
   */
  protected String cwd = "/";

  protected boolean done = false;

  /**
   * std in scanner
   */
  protected Scanner in;

  /**
   * std out
   */
  protected PrintStream out = null;

  transient private AtmosphereSocket socket;

  private HelloRequest serverHelloRequest;

  public void connect() {

    RequestBuilder<?> request = null;

    try {

      if (client == null) {
        client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

        // Netty Config ..
        NettyAsyncHttpProviderConfig nettyConfig = new NettyAsyncHttpProviderConfig();
        nettyConfig.addProperty("tcpNoDelay", "true");
        nettyConfig.addProperty("keepAlive", "true");
        nettyConfig.addProperty("reuseAddress", true);
        // nettyConfig.addProperty("connectTimeoutMillis",
        // nettyConnectionTimeout);
        nettyConfig.setWebSocketMaxFrameSize(262144);
        nettyConfig.addProperty("child.tcpNoDelay", "true");
        nettyConfig.addProperty("child.keepAlive", "true");
        // nettyConfig.setWebSocketMaxFrameSize(65536);

        // AsyncHttpClientConfig Config
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        b.setFollowRedirect(true).setMaxRequestRetry(-1).setConnectTimeout(-1).setReadTimeout(30000);
        AsyncHttpClientConfig config = b.setAsyncHttpClientProviderConfig(nettyConfig).build();
        asc = new AsyncHttpClient(config);
      }

      // socket =
      // client.create(client.newOptionsBuilder().reconnect(true).reconnectAttempts(999).runtime(asc).build());
      if (socket == null) {
        request = client.newRequestBuilder();
        request.method(Request.METHOD.GET);
        request.uri(getUrl());
        request.encoder(this).decoder(this);
        // Can request for other transports like WEBSOCKET, SSE, STREAMING,
        // LONG_POLLING
        request.transport(Request.TRANSPORT.WEBSOCKET);

        socket = (AtmosphereSocket) client.create(client.newOptionsBuilder().runtime(asc).build());
        socket.on(Event.CLOSE.name(), new Function<String>() {
          @Override
          public void on(String t) {
            // System.out.println("CLOSE " + t);
          }
        }).on(Event.REOPENED.name(), new Function<String>() {
          @Override
          public void on(String t) {
            // System.out.println("REOPENED " + t);
          }
        }).on(Event.ERROR.name(), new Function<String>() {
          @Override
          public void on(String t) {
            System.out.println("ERROR " + t);
          }
        }).on(Event.MESSAGE.name(), new Function<String>() {
          @Override
          public void on(String t) {
            // all messages
            // System.out.println("MESSAGE {}" + t);
          }
        }).on(new Function<IOException>() {
          @Override
          public void on(IOException ioe) {
            System.out.println(ioe.getMessage());
            socket = null;
            // ioe.printStackTrace();
          }
        }).on(Event.STATUS.name(), new Function<String>() {
          @Override
          public void on(String t) {
            // System.out.println("STATUS " + t);
          }
        }).on(Event.HEADERS.name(), new Function<String>() {
          @Override
          public void on(String t) {
            // System.out.println("HEADERS " + t);
          }
        }).on(Event.MESSAGE_BYTES.name(), new Function<String>() {
          @Override
          public void on(String msg) {
            // System.out.println("MESSAGE_BYTES " + t);
            System.out.println(msg);
          }
        }).on(Event.OPEN.name(), new Function<String>() {
          @Override
          public void on(String t) {
            // System.out.println("OPEN " + t);
          }
        }).open(request.build());
      }

    } catch (Exception e) {
      // the exception is propagated to socket event
      // handler too - no need to double print
      // System.out.println(e.getMessage());
    }
  }

  public String getUrl() {
    return String.format("%s/api/%s?id=%s", base, apiType, clientId);
  }

  @Override
  public Reader decode(Event e, String s) {
    // System.out.println("decode " + s);
    if ("X".equals(s)) {
      return null;
    }
    if ("OPEN".equals(s)) {
      System.out.println("connected to " + getUrl());
      return null;
    }

    if ("CLOSE".equals(s)) {
      System.out.println("disconnected from " + getUrl());
      return null;
    }

    // System.out.println(s);
    Message msg = gson.fromJson(s, Message.class);

    if ("getHelloResponse".equals(msg.method)) {
      serverHelloRequest = gson.fromJson(msg.data[1].toString(), HelloRequest.class);
      promptId = serverHelloRequest.id;
      System.out.println("attaching to id " + promptId);
    }

    if (msg.data == null || msg.data[0] == null) {
      System.out.println("null");
    } else {
      System.out.println(gson.toJson(msg.data[0]));
    }
    prompt();
    return new StringReader(s);
  }

  @Override
  public Reader encode(String s) {
    // System.out.println("encode " + s);
    return new StringReader(s);
  }

  public void process(String cmd) {
    try {

      cmd = cmd.trim();
      String[] parts = cmd.split(" ");

      switch (parts[0]) {
        case "cd":
          if (parts.length == 2) {
            // cd xxx
            if (parts[1].startsWith("/")) {
              cwd = parts[1];
            } else {
              cwd += parts[1];
            }
          }
          write(cwd);
          break;

        case "pwd":
          write(cwd);
          break;

        case "help":
          write(cwd);
          break;

        case "route":
          processServiceCli("runtime", "route");
          break;

        case "connect":
          if (parts.length == 2) {
            base = parts[1];
          }
          connect();
          break;

        case "ls":
          // TODO - ls ../ relative
          // ls /
          // with cwd ...
          String lsPath = cwd;
          if (parts.length == 2) {
            if (parts[1].startsWith("/")) {
              lsPath = parts[1];
            } else {
              lsPath = cwd + parts[1];
            }
          }
          processServiceCli("runtime", "ls", lsPath);
          break;

        case "lp":
          processServiceCli("agent", "lp");
          break;

        case "exit":
          exit();
          break;

        default:
          processServiceCli(cmd);
      }

    } catch (Exception e) {
      System.out.println("cli threw");
      e.printStackTrace();
    }
  }

  private void processServiceCli(String cmd) {
    String service = "runtime";
    String method = "ls";
    Object[] params = null;

    // resolve path
    if (!cmd.startsWith("/")) {
      cmd = cwd + cmd;
    }

    String[] parts = cmd.split("/");

    if (parts.length < 3) {
      params = new Object[] { cmd };
    }

    // fix me diff from 2 & 3 "/"
    if (parts.length >= 3) {
      service = parts[1];
      // prepare the method
      method = parts[2].trim();

      // FIXME - to encode or not to encode that is the question ...
      params = new Object[parts.length - 3];
      for (int i = 3; i < parts.length; ++i) {
        params[i - 3] = parts[i];
      }
    }
    processServiceCli(service, method, params);
  }

  private void processServiceCli(String service, String method, Object... params) {
    try {

      connect();

      String[] data = null;

      if (params != null) {
        data = new String[params.length];
        for (int i = 0; i < params.length; ++i) {
          data[i] = gson.toJson(params[i]);
        }
      }

      Message msg = Message.createMessage(String.format("%s@%s", "runtime", clientId), service, method, data);
      msg.setBlocking();
      if (socket != null) {
        socket.fire(gson.toJson(msg));
      } else {
        // System.out.println("could not send msg - no viable socket");
      }

    } catch (Exception e) {
      System.out.println("could not send command " + e.getMessage());
    }
  }

  protected void prompt() {
    if (out != null) {
      try {
        out.write("\n".getBytes());
        out.write(String.format("[%s@%s %s]%s", "runtime", promptId, cwd, "#").getBytes());
        out.write(" ".getBytes());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void run() {
    prompt();
    while (!done) {
      process(in.nextLine());
      prompt();
    }
  }

  protected void write(String ret) {
    if (out != null) {
      try {
        out.write(ret.getBytes());
        out.write("\n".getBytes());
        prompt();
        out.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  protected void exit() {

    if (clientId.equals(promptId)) {
      if (asc != null) {
        asc.close();
      }

      System.out.println("exiting " + clientId);
      done = true;
      // System.exit(0);
    }

    if (socket != null) {
      // socket.close();
      socket = null;
      System.out.println("exiting " + promptId);
      promptId = clientId;
    }
  }

  public static void main(String[] args) {
    Client client = null;
    try {

      Logging logging = LoggingFactory.getInstance();
      logging.setLevel("WARN");

      clientId = promptId = (username != null) ? (String.format("cli-%s-%d", username, System.currentTimeMillis())) : (String.format("cli-%s-%s",
          InetAddress.getLocalHost().getHostName(), NameGenerator.getName()));
      InetAddress.getLocalHost().getHostAddress();

      client = new Client();

      CommandLine cmdline = new CommandLine(client);

      client.in = new Scanner(System.in);
      client.out = System.out;

      int exitCode = cmdline.execute(args);

      System.exit(exitCode);

    } catch (Exception e) {
      e.printStackTrace();
    }

    if (client != null) {
      client.exit();
    }
  }
}
