package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.framework.HelloRequest;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class ApiCli extends Api {

  public final static Logger log = LoggerFactory.getLogger(ApiCli.class);

  public String getPrompt(String uuid) {
    Map<String, Object> gateway = Runtime.getConnection(uuid);
    String prompt = "root".equals(gateway.get("user")) ? "#" : "$";
    return String.format("[%s@%s %s]%s", gateway.get("user"), gateway.get("host"), gateway.get("cwd"), prompt);
  }

  protected String getDefaultMethod() {
    return "getHelloResponse";
  }

  // FIXME - change to client data instead of httpsession
  @Deprecated /* make better process(uuid) - with maintaining the data points in a keyed hashmap */
  public Object process(MessageSender gateway, String apiKey, String uri, String uuid, OutputStream out, String data) throws Exception {
    // FIXME what if data & msgFromUri are both present ?
    Object ret = null;

    if (data != null) {
      data = data.trim();
      uri += data;
    }

    Message msgFromUri = uriToMsg(uri, getDefaultMethod());

    // no point in having output if no out pipe exists
    if (out != null && data != null) {
      data = data.trim();
      if ("".equals(data)) {
        writePrompt(out, uuid);
        return ret;
      }
      if (data.startsWith("cd")) {
        String path = null;
        if (data.length() > "cd".length()) {
          path = data.substring("cd".length()).trim();
        }
        // absolute or relative ! ..
        // FIXME - must check on validity
        Map<String, Object> c = Runtime.getConnection(uuid);
        c.put("cwd", path);
      } else if ("pwd".equals(data)) {
        Map<String, Object> c = Runtime.getConnection(uuid);
        out.write(c.get("cwd").toString().getBytes());
      } else if ("lc".equals(data)) {
        ret = Runtime.getConnectionNames();
        // ret = Runtime.getClients();
        out.write(CodecUtils.toPrettyJson(ret).getBytes()); // FIXME - normalize

      } else if ("whoami".equals(data)) {
        ret = Runtime.getConnectionName(uuid);
        out.write(CodecUtils.toPrettyJson(ret).getBytes()); // FIXME - normalize

      } else if (data.startsWith("ls")) {
        Runtime runtime = Runtime.getInstance();
        Map<String, Object> c = Runtime.getConnection(uuid);
        ret = runtime.ls(data.substring("ls".length()).trim());
        out.write(CodecUtils.toPrettyJson(ret).getBytes()); // FIXME - normalize

      } else if (data.startsWith("attach")) {
        String id = null;
        if (data.length() > "attach".length()) {
          id = data.substring("attach".length()).trim();
        }
        
        String toUuid = Runtime.getRoute(id);
        
        // get remote connection
        Map<String,Object> conn = Runtime.getConnection(toUuid);
        if (conn == null) {
          log.error("could not find {}", toUuid);
          return null;
        }
        
        // inspect type of connection and api
        HelloRequest remoteInfo = (HelloRequest)conn.get("request");
        String remoteApiKey = (String)conn.get("c-type");
        AtmosphereResource r = (AtmosphereResource)conn.get("c-r");
        
        Broadcaster b = r.getBroadcaster();
        Message msg = Message.createMessage((String)null, "runtime", "getUptime", new Object[0]);
        // FIXME FIXME FIXME - don't we double encode parameters - (maybe not required for typless languages - only for strong typed ?)
        b.broadcast(CodecUtils.toJson(msg)); // FIXME make messages2 api handle         
        // stash relay and conversion - interfaces would protect against exotic types (xmpp, etc)
        

        // webgui.attach(me, client, uri-api/cli)
        // webgui.attach(uuid, toUuid, "/api/cli"); -- FIXME !!! implement
        // FIXME - what to attach - change of prompt ???
      } else {
        // ========= HANDLE URI SERVICE CALLS ====================

        // FIXME - if Runtime.getService(msgFromUri.name) == null && no connections - then is error send <- should expect remote 
        if (Runtime.getInstance().isLocal(msgFromUri)) {
          // local service
          MethodCache cache = MethodCache.getInstance();

          String serviceName = msgFromUri.getName();
          Class<?> clazz = Runtime.getClass(serviceName);
          Object[] params = cache.getDecodedJsonParameters(clazz, msgFromUri.method, msgFromUri.data);

          Method method = cache.getMethod(clazz, msgFromUri.method, params);
          ServiceInterface si = Runtime.getService(serviceName);
          if (method == null) {
            log.error("{} not found", msgFromUri);
            writePrompt(out, uuid);
            return null;
          }
          ret = method.invoke(si, params);
          if (out != null) {
            out.write(CodecUtils.toPrettyJson(ret).getBytes());
          }
        } else {
          // fire it remotely 
          Runtime.getInstance().send(msgFromUri);
        }
      }
      writePrompt(out, uuid);

    } // out != null && data != null

    return ret;
  }

  public void writePrompt(OutputStream out, String uuid) throws IOException {
    out.write("\n".getBytes());
    out.write(getPrompt(uuid).getBytes());
    out.write(" ".getBytes());
  }

  public static ApiDescription getDescription() {
    ApiDescription desc = new ApiDescription("message", "{scheme}://{host}:{port}/api/messages", "ws://localhost:8888/api/messages",
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is /api/messages data contains a json encoded Message structure");
    return desc;
  }

}
