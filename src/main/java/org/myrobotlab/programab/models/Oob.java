package org.myrobotlab.programab.models;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

/**
 * AIML 2.0 Oob Out Of Band xml defined with mrl - legacy and mrljson - json
 * typed message
 * 
 * @author GroG
 *
 */
public class Oob {

  public String mrljson;

  @JacksonXmlElementWrapper(useWrapping = false)
  public List<Mrl> mrl;
}
