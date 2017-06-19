package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;

import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.MessageSender;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 *         <pre>
 * 
 * This is a "blocking" API - much like a simple function call with a return value, this
 * will "block" until the transaction has completed and the result is ready.
 * 
 * Usually, it is better "NOT TO BLOCK" - because we have better things to do than wait
 * around for returned data.  The Messages API is asynchronous and is designed to return
 * data without "blocking"
 * 
 * must be in one of the following forms
 * GET http://host/api/services/{name}                  return service's data
 * GET http://host/api/services/{name}/                 list methods
 * GET http://host/api/services/{name}/{method}         (call method - no params)
 * GET http://host/api/services/{name}/{method}         no parameters
 * GET http://host/api/services/{name}/{method}/.../.   parameters
 * 
 * POST http://host/api/services/{name}/{method}        POST data is a json array of json encoded strings for parameters
 *     
 * data can be in post or on parameters
 *         </pre>
 *         
 *         
 * FIXME !!! - this is wrong .. needs to call a "framework" invoke - when a method is called, the notify lists need to be 
 * "notified" !
 *
 */
public class ApiProcessorServices implements ApiProcessor {

  public final static Logger log = LoggerFactory.getLogger(ApiProcessorServices.class);

  public Object process(MessageSender sender, OutputStream out, URI uri, byte[] data) throws Exception {

    // FIXME change to CodecUtils.MIME_TYPE_JSON
    Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_MRL_JSON);
    String path = uri.getPath();
    String[] parts = path.split("/");
    Object ret = null;
    
    if (parts.length < 4){
      return Status.error("api http://{host}:{port}/api/services/{serviceName}/{method}/{param1}/{param2}/... - actual [%s]", uri);
    }

    // post data is expected to be an array of parameters
    // POST vs GET parameters... who has precedence ?

    String name = parts[3];
    ServiceInterface si = Runtime.getService(name);
    if (si == null) {
      throw new IOException(String.format("service %s not found", name));
    }

    if (parts.length == 4) {
      if (out != null) {
        if (path.endsWith("/")) {
          // http://localhost:8888/api/services/runtime/servo/
          ret =  si.getMethodMap();
        } else {
          // http://localhost:8888/api/services/runtime/servo          
          ret =  si;
        }
        codec.encode(out, ret);
        return ret;
      }
    }

    String methodName = parts[4];

    boolean isLocal = si.isLocal();
    Class<?> clazz = si.getClass();
    Class<?>[] paramTypes = null;
    Object[] params = new Object[0];
    Object[] encodedArray = new Object[0];

    if (parts.length > 5) {
      // GET - parameters are part of uri
      // REQUIREMENT must be in an encoded array - even binary
      // 1. array is URI /
      // 2. will need to decode contents of each parameter later based
      // on signature of reflected method

      // get params from uri - its our array
      // difference is initial state regardless of encoding we are
      // guaranteed the URI parts are strings
      // encodedArray = new Object[parts.length - 3];
      encodedArray = new Object[parts.length - 5];

      for (int i = 0; i < encodedArray.length; ++i) {
        String result = URLDecoder.decode(parts[i + 5], "UTF-8");
        encodedArray[i] = result;
      }
      // WE NOW HAVE ORDINAL
    } else if (data != null && data.length > 0) {
      // POST - parameters are in post body
      String json = new String(data);
      encodedArray = codec.decodeArray(json);
    } else {
      // FIXME HANDLE ERROR !!!! EITHER GET WITH URL PARAMS OR POST WITH BODY
    }

    // FETCH AND MERGE METHOD - we have ordinal count now - but NOT the
    // decoded
    // parameters
    // NOW HAVE ORDINAL - fetch the method with its types
    paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), methodName, encodedArray.length);
    // WE NOW HAVE ORDINAL AND TYPES
    params = new Object[encodedArray.length];

    // DECODE AND FILL THE PARAMS
    for (int i = 0; i < params.length; ++i) {
      params[i] = codec.decode(encodedArray[i], paramTypes[i]);
    }

    Method method = clazz.getMethod(methodName, paramTypes);

    // NOTE --------------
    // strategy of find correct method with correct parameter types
    // "name" is the strongest binder - but without a method cache we
    // are condemned to scan through all methods
    // also without a method cache - we have to figure out if the
    // signature would fit with instanceof for each object
    // and "boxed" types as well

    // best to fail - then attempt to resolve through scanning through
    // methods and trying types - then cache the result

    if (isLocal) {
      log.debug("{} is local", name);
      ret = method.invoke(si, params);
    } else {
      // FIXME - create blocking message request
      log.debug("{} is is remote", name);
      // Message msg = Runtime.getInstance().createMessage(si.getName(),
      // CodecUtils.getCallBackName(methodName), params);
      // FIXME MUST DO BLOCKING MSG !!!
      // FIXME - sendBlocking should throw and exception if it can't send !!!
      // NOT JUST RETURN NULL !!!
      ret = sender.sendBlocking(name, methodName, params);
    }

    if (out != null) {
      codec.encode(out, ret);
    }

    MethodCache.cache(clazz, method);

    return ret;

  }
}