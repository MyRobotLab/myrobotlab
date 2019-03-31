package org.myrobotlab.programab;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class OOBPayload {

  transient public final static Logger log = LoggerFactory.getLogger(OOBPayload.class);
  // TODO: something better than regex to parse the xml.  (Problem is that the service/method/param values 
  // could end up double encoded ... So we had to switch to hamd crafting the aiml for the oob/mrl tag.
  public transient static final Pattern oobPattern = Pattern.compile("<oob>.*?</oob>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  public transient static final Pattern mrlPattern = Pattern.compile("<mrl>.*?</mrl>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  public transient static final Pattern servicePattern = Pattern.compile("<service>(.*?)</service>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  public transient static final Pattern methodPattern = Pattern.compile("<method>(.*?)</method>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
  public transient static final Pattern paramPattern = Pattern.compile("<param>(.*?)</param>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

  private String serviceName;
  private String methodName;
  private ArrayList<String> params;

  public OOBPayload() {
    // TODO: remove the default constructor
  };

  public OOBPayload(String serviceName, String methodName, ArrayList<String> params) {
    this.serviceName = serviceName;
    this.methodName = methodName;
    this.params = params;
  }

  public String getMethodName() {
    return methodName;
  }

  public ArrayList<String> getParams() {
    return params;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public void setParams(ArrayList<String> params) {
    this.params = params;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public static String asOOBTag(OOBPayload payload) {
    // TODO: this isn't really safe as XML/AIML.. but we don't want to end up
    // double encoding things like
    // the important <star/> tags... So, for now, it's just wrapped in the tags.
    StringBuilder oobBuilder = new StringBuilder();
    oobBuilder.append("<oob>");
    oobBuilder.append("<mrl>");
    oobBuilder.append("<service>");
    oobBuilder.append(payload.getServiceName());
    oobBuilder.append("</service>");
    oobBuilder.append("<method>");
    oobBuilder.append(payload.getMethodName());
    oobBuilder.append("</method>");
    for (String param : payload.params) {
      oobBuilder.append("<param>");
      // TODO: this could be problematic if the param contains XML chars that
      // are not AIML ...
      oobBuilder.append(param);
      oobBuilder.append("</param>");
    }
    oobBuilder.append("</mrl>");
    oobBuilder.append("</oob>");
    return oobBuilder.toString();
  }

  public static String asBlockingOOBTag(OOBPayload oobTag) {
    return "<sraix>" + OOBPayload.asOOBTag(oobTag) + "</sraix>";
  }

  public static OOBPayload fromString(String oobPayload) {

    // TODO: fix the damn double encoding issue.
    // we have user entered text in the service/method
    // and params values.
    // grab the service

    Matcher serviceMatcher = servicePattern.matcher(oobPayload);
    serviceMatcher.find();
    String serviceName = serviceMatcher.group(1);

    Matcher methodMatcher = methodPattern.matcher(oobPayload);
    methodMatcher.find();
    String methodName = methodMatcher.group(1);

    Matcher paramMatcher = paramPattern.matcher(oobPayload);
    ArrayList<String> params = new ArrayList<String>();
    while (paramMatcher.find()) {
      // We found some OOB text.
      // assume only one OOB in the text?
      String param = paramMatcher.group(1);
      params.add(param);
    }
    OOBPayload payload = new OOBPayload(serviceName, methodName, params);
    // log.info(payload.toString());
    return payload;
  }
  
  
  public static boolean invokeOOBPayload(OOBPayload payload, String sender, boolean blocking) {
    ServiceInterface s = Runtime.getService(payload.getServiceName());
    // the service must exist and the method name must be set.
    if (s == null || StringUtils.isEmpty(payload.getMethodName())) {
      return false;
    }
    
    
    if (!blocking) {
      s.in(Message.createMessage(sender, payload.getServiceName(), payload.getMethodName(), payload.getParams().toArray()));
      // non-blocking.. fire and forget!
      return true;
    }
    
    // TODO: should you be able to be synchronous for this
    // execution?
    Object result = null;
    if (payload.getParams() != null) {
      result = s.invoke(payload.getMethodName(), payload.getParams().toArray());
    } else {
      result = s.invoke(payload.getMethodName());
    }
    log.info("OOB PROCESSING RESULT: {}", result);
    return true;
  }

  public static ArrayList<OOBPayload> extractOOBPayloads(String text, ProgramAB programAB) {
    ArrayList<OOBPayload> payloads = new ArrayList<OOBPayload>();
    Matcher oobMatcher = OOBPayload.oobPattern.matcher(text);
    while (oobMatcher.find()) {
      // We found some OOB text.
      // assume only one OOB in the text?
      String oobPayload = oobMatcher.group(0);
      Matcher mrlMatcher = OOBPayload.mrlPattern.matcher(oobPayload);
      while (mrlMatcher.find()) {
        String mrlPayload = mrlMatcher.group(0);
        OOBPayload payload = OOBPayload.fromString(mrlPayload);
        payloads.add(payload);
        // TODO: maybe we dont' want this?
        // Notifiy endpoints
        programAB.invoke("publishOOBText", mrlPayload);
        // grab service and invoke method.

      }
    }
    return payloads;
  }

  public static String removeOOBFromString(String res) {
    Matcher matcher = OOBPayload.oobPattern.matcher(res);
    res = matcher.replaceAll("");
    return res;
  }
  
}
