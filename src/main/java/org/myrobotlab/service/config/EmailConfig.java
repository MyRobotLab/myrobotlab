package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

public class EmailConfig extends ServiceConfig {

  @Deprecated /* is supposed to be userfriendly not like a single properties */
  public Map<String,String> properties = new HashMap<>(); 
  
  public String to; // if set sends auto 
  
  public String format = "text/html"; // text/html or text/plain
  
  // elements in the map
   public String user = null;
   public String host = null;
   public int port = 25; /* 465, 587 */
   public String from = null;
   boolean auth = true;
   boolean starttls = true;
   boolean debug = true;
   boolean starttlsRequired = true;   
   String protocols = "TLSv1.2";
   String socketFactory = "javax.net.ssl.SSLSocketFactory";
   
   public String pass = null;

}
