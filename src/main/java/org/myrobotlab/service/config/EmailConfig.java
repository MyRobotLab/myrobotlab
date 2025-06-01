package org.myrobotlab.service.config;

/**
 *  Email config gets turned into javax email props
 */
public class EmailConfig extends ServiceConfig {

  // javax email props are so inane they don't preserve typeness
  // and error if typed
  
  public String auth = "true";
  public String debug = "true";
  public String format = "html"; // text/html or text/plain
  public String from = "";
  public String host = "";
  public String pass = "";
  public String protocols = "TLSv1.2";
  public String port = "25"; /* 465, 587 */
  public String socketFactory = "javax.net.ssl.SSLSocketFactory";
  public String starttlsEnabled = "true";
  public String starttlsRequired = "true";
  public String to; // if set sends auto
  public String user = "";

}
