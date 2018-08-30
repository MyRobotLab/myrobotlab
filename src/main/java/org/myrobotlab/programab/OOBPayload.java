package org.myrobotlab.programab;

import java.io.File;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mrl")
public class OOBPayload {

  private String serviceName;

  private String methodName;
  private ArrayList<String> params;

  public static void main(String[] args) {

    ArrayList<String> params = new ArrayList<String>();
    params.add("bar");
    OOBPayload payload = new OOBPayload("foo", "exec", params);
    File file = new File("C:\\dev\\file.xml");
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(OOBPayload.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      // output pretty printed
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.marshal(payload, file);
      jaxbMarshaller.marshal(payload, System.out);
      
      // String xml =
      // "<mrl><method>exec</method><param>bar</param><service>foo</service></mrl>";
      // StringReader xmlR = new StringReader(xml);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      OOBPayload oob = (OOBPayload) jaxbUnmarshaller.unmarshal(file);
      System.out.println(oob.getServiceName());
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public OOBPayload() {
    // TODO : anything?
  }

  public OOBPayload(String serviceName, String methodName, ArrayList<String> params) {
    this.serviceName = serviceName;
    this.methodName = methodName;
    this.params = params;
  }

  public String getMethodName() {
    return methodName;
  }

  @XmlElement(name = "param")
  public ArrayList<String> getParams() {
    return params;
  }

  public String getServiceName() {
    return serviceName;
  }

  @XmlElement(name = "method")
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public void setParams(ArrayList<String> params) {
    this.params = params;
  }

  @XmlElement(name = "service")
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
  
  public static String asOOBTag(OOBPayload payload) {
    // TODO: this isn't really safe as XML/AIML.. but we don't want to end up double encoding things like 
    // the important  <star/> tags...  So, for now, it's just wrapped in the tags.
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
      // TODO: this could be problematic if the param contains XML chars that are not AIML ...
      oobBuilder.append(param);
      oobBuilder.append("</param>");
    }
    oobBuilder.append("</mrl>");
    oobBuilder.append("</oob>");
    return oobBuilder.toString();
  }

  public static String asBlockingOOBTag(OOBPayload oobTag) {
    return "<sraix>"+OOBPayload.asOOBTag(oobTag)+"</sraix>";
  }

}
