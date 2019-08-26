package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Map;

import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.framework.Message;
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
  public Object process(MessageSender webgui, String apiKey, String uri, String uuid, OutputStream out, String data) throws Exception {
    // FIXME what if data & msgFromUri are both present ?
    Object ret = null;
    boolean returnObject = false;

    if (data != null) {
      data = data.trim();
      uri += data;
    }

    Message msgFromUri = uriToMsg(uri);

    // no point in having output if no out pipe exists
    if (out != null && data != null) {
      data = data.trim();
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
        out.write(CodecUtils.toJson(ret).getBytes()); // FIXME - normalize

      } else if ("whoami".equals(data)) {
        ret = Runtime.getConnectionName(uuid);
        out.write(CodecUtils.toJson(ret).getBytes()); // FIXME - normalize

      } else if (data.startsWith("ls")) {
        Runtime runtime = Runtime.getInstance();
        Map<String, Object> c = Runtime.getConnection(uuid);
        ret = runtime.ls(c.get("cwd").toString(), data.substring("ls".length()).trim());
        out.write(CodecUtils.toJson(ret).getBytes()); // FIXME - normalize

      } else if (data.startsWith("attach")) {
        String toUuid = null;
        if (data.length() > "attach".length()) {
          toUuid = data.substring("attach".length()).trim();
        }

        // webgui.attach(me, client, uri-api/cli)
        // webgui.attach(uuid, toUuid, "/api/cli"); -- FIXME !!! implement
        // FIXME - what to attach - change of prompt ???
      } else {
        // ========= HANDLE URI SERVICE CALLS ====================
        returnObject = true;
        ServiceInterface si = Runtime.getService(msgFromUri.name);
        if (si == null) {
          throw new IOException(String.format("service %s not found", msgFromUri.name));
        }

        Class<?> clazz = si.getClass();
        Class<?>[] paramTypes = null;
        Object[] params = new Object[0];
        String[] encodedArray = new String[0];

        if (msgFromUri.data != null) {

          encodedArray = new String[msgFromUri.data.length];

          for (int i = 0; i < encodedArray.length; ++i) {
            String result = URLDecoder.decode((String) msgFromUri.data[i], "UTF-8");
            encodedArray[i] = result;
          }

          paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), msgFromUri.method, encodedArray.length);
          params = new Object[encodedArray.length];

          // DECODE AND FILL THE PARAMS
          for (int i = 0; i < params.length; ++i) {
 //            params[i] = codec.decode(encodedArray[i], paramTypes[i]);
            
            params[i] = CodecUtils.fromJson(encodedArray[i], paramTypes[i]);
          }
        }
        // FIXME - ONE INVOKER !!! ONE METHOD CACHE !!!
        Method method = clazz.getMethod(msgFromUri.method, paramTypes);
        
        // send vs send blocking ...
        // sender.send(msgFromUri);

        if (si.isLocal()) {
          log.debug("{} is local", msgFromUri.name);
          ret = method.invoke(si, params);
        } else {
          // FIXME - create blocking message request
          log.debug("{} is is remote", msgFromUri.name);
          // Message msg = Runtime.getInstance().createMessage(si.getName(),
          // CodecUtils.getCallBackName(methodName), params);
          // FIXME MUST DO BLOCKING MSG !!!
          // FIXME - sendBlocking should throw and exception if it can't send
          // !!!
          // NOT JUST RETURN NULL !!!
          ret = webgui.sendBlocking(msgFromUri.name, msgFromUri.method, params);
        }
        MethodCache.cache(clazz, method);
      } // else service call

      // ==== handle return and prompt ====
      if (returnObject) {
        if (ret == null) {
          out.write("null".getBytes());
        } else {
          out.write(CodecUtils.encodePretty(ret).getBytes());
        }
      }
      out.write("\n".getBytes());
      out.write(getPrompt(uuid).getBytes());
      out.write(" ".getBytes());

    } // out != null && data != null

    return ret;
  }

  public static ApiDescription getDescription() {
    ApiDescription desc = new ApiDescription("message", "{scheme}://{host}:{port}/api/messages", "ws://localhost:8888/api/messages",
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is /api/messages data contains a json encoded Message structure");
    return desc;
  }

}
