package org.myrobotlab.programab.models;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

public class Oob {
  
  public String mrljson;
  
  @JacksonXmlElementWrapper(useWrapping = false)
  public List<Mrl> mrl;
}

