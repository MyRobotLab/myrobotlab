package org.myrobotlab.service.interfaces;

public interface Translator {
  
    public String translate(String text);
    
    public String translate(String text, String fromLang, String toLang);
}
