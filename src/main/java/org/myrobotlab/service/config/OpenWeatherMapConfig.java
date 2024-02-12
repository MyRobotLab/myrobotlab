package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class OpenWeatherMapConfig extends HttpClientConfig {

  public String currentUnits;
  public String currentTown;
  
  public Peer httpClient = new Peer("httpClient", "HttpClient");
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    
    addDefaultPeerConfig(plan, name, "httpClient", "HttpClient");
    
    return plan;
  }


}