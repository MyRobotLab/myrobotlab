package org.myrobotlab.programab;

import org.myrobotlab.programab.models.Sraix;
import org.myrobotlab.programab.models.Template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Thread safe fasterjackson xml parser.
 * 
 * @author GroG
 *
 */
public class XmlParser {

  public static Template parseTemplate(String xml) throws JsonMappingException, JsonProcessingException {
    ThreadLocal<XmlMapper> xmlMapperThreadLocal = ThreadLocal.withInitial(XmlMapper::new);
    XmlMapper xmlMapper = xmlMapperThreadLocal.get();
    Template template = xmlMapper.readValue(xml, Template.class);
    return template;
  }

  public static Sraix parseSraix(String xml) throws JsonMappingException, JsonProcessingException {
    ThreadLocal<XmlMapper> xmlMapperThreadLocal = ThreadLocal.withInitial(XmlMapper::new);
    XmlMapper xmlMapper = xmlMapperThreadLocal.get();
    return xmlMapper.readValue(xml, Sraix.class);
  }

}
