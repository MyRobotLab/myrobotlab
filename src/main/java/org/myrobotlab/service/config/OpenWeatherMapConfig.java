package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class OpenWeatherMapConfig extends ServiceConfig {

  public String currentUnits;
  public String currentTown;
  
  // name of peers
  public String httpClient;

  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    
    // default peer names
    httpClient = name + ".httpClient";
    addPeer(plan, name, "httpClient", httpClient, "HttpClient", "HttpClient");
    
    return plan;
  }


}