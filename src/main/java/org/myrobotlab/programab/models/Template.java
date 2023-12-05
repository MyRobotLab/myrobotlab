package org.myrobotlab.programab.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

//@JacksonXmlRootElement(localName = "template")
//@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Template {
  // @JacksonXmlElementWrapper(useWrapping = false)
  
  @JacksonXmlProperty(localName = "template")
  
  @JacksonXmlText
  public String text;  
  
  
public Oob oob;
  
//  @JsonProperty("ignorable")
//  public List<Oob> oob;
//
//  public List<Oob> getOob() {
//    return oob;
//  }
//
//  public void setOob(List<Oob> oob) {
//    this.oob = oob;
//  }
  
  public static void main(String[] args) {

    try {
      
      // String xml = "<template>XXX<oob><mrl><service>blah</service><method>method</method></mrl></oob></template>";
      // String xml = "<template>XXXX<oob><mrl><service>blah1</service><method>method1</method></mrl><mrl><service>blah2</service><method>method2</method></mrl></oob></template>";
      String xml = "<template>XXXX<oob><mrl><service>blah1</service><method>method1</method><param>p1</param><param>p2</param><param>p3</param></mrl><mrl><service>blah2</service><method>method2</method></mrl><mrljson>[\"method\":\"doIt\",\"data\":[\"p1\"]]</mrljson></oob></template>";
      
      XmlMapper xmlMapper = new XmlMapper();
      Template template = xmlMapper.readValue(xml, Template.class);
      
      System.out.println(template);
      
    } catch(Exception e) {
      e.printStackTrace();
    }
    }

}
