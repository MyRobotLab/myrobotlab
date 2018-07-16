package org.myrobotlab.programab;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alicebot.ab.Chat;
import org.alicebot.ab.SraixHandler;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class MrlSraixHandler implements SraixHandler {
  transient public final static Logger log = LoggerFactory.getLogger(MrlSraixHandler.class);
  Pattern oobPattern = Pattern.compile("<oob>.*?</oob>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  Pattern mrlPattern = Pattern.compile("<mrl>.*?</mrl>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  
  @Override
  public String sraix(Chat chatSession, String input, String defaultResponse, String hint, String host, String botid, String apiKey, String limit) {
    // TODO: what the heck gets passed in here?!?!
    
    // the INPUT has the string we care about.  if this is an OOB tag, let's evaluate it and return the result.  o/w fall back to default behavior 
    // of pannous / pandorabots?
    String response = processInlineOOB(input);
    
    
    // ok.. 
    return response;
  }

  
  private String processInlineOOB(String text) {
    // Find any oob tags
    StringBuilder responseBuilder = new StringBuilder();
    ArrayList<OOBPayload> payloads = new ArrayList<OOBPayload>();
    Matcher oobMatcher = oobPattern.matcher(text);
    int start = 0;
    while (oobMatcher.find()) {
      // We found some OOB text.
      // assume only one OOB in the text?
      // everything from the start to the end of this
      responseBuilder.append(text.substring(start, oobMatcher.start()));
      // update the end to be
      // next segment is from the end of this one to the start of the next one.
      start = oobMatcher.end();
      String oobPayload = oobMatcher.group(0);
      Matcher mrlMatcher = mrlPattern.matcher(oobPayload);
      while (mrlMatcher.find()) {
        String mrlPayload = mrlMatcher.group(0);
        OOBPayload payload = parseOOB(mrlPayload);
        payloads.add(payload);
        // grab service and invoke method.
        ServiceInterface s = Runtime.getService(payload.getServiceName());
        if (s == null) {
          log.warn("Service name in OOB/MRL tag unknown. {}", mrlPayload);
          return null;
        }
        Object result = null;
        if (payload.getParams() != null) {
          result = s.invoke(payload.getMethodName(), payload.getParams().toArray());
        } else {
          result = s.invoke(payload.getMethodName());
        }
        log.info("OOB PROCESSING RESULT: {}", result);
        responseBuilder.append(result);
      }
    }
    // append the last part. (assume the start is set to the end of the last match.. 
    // or zero if no matches found.
    responseBuilder.append(text.substring(start));
    return responseBuilder.toString();    
  }

  
  private OOBPayload parseOOB(String oobPayload) {

    // TODO: fix the damn double encoding issue.
    // we have user entered text in the service/method
    // and params values.
    // grab the service
    Pattern servicePattern = Pattern.compile("<service>(.*?)</service>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    Matcher serviceMatcher = servicePattern.matcher(oobPayload);
    serviceMatcher.find();
    String serviceName = serviceMatcher.group(1);

    Pattern methodPattern = Pattern.compile("<method>(.*?)</method>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    Matcher methodMatcher = methodPattern.matcher(oobPayload);
    methodMatcher.find();
    String methodName = methodMatcher.group(1);

    Pattern paramPattern = Pattern.compile("<param>(.*?)</param>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    Matcher paramMatcher = paramPattern.matcher(oobPayload);
    ArrayList<String> params = new ArrayList<String>();
    while (paramMatcher.find()) {
      // We found some OOB text.
      // assume only one OOB in the text?
      String param = paramMatcher.group(1);
      params.add(param);
    }
    OOBPayload payload = new OOBPayload(serviceName, methodName, params);
    return payload;

  }
}
