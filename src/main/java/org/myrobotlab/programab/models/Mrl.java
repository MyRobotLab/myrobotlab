package org.myrobotlab.programab.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

public class Mrl {
  public String service;
  public String method;
  @JacksonXmlElementWrapper(useWrapping = false)
  @JsonProperty("param")
  public List<String> params;
}