package org.myrobotlab.service.data;

import java.util.Set;

import java.util.TreeSet;

/**
 * 
 * @author GroG
 * A set of service type names from a result of a query. The query is typically an interface name.
 * A use case is when the UI wants to know all Service Types that implement the SpeechSynthesis interface.
 * These functions already exist in Runtime, however none of them return the original query, so its not
 * practical when the UI does multiple queries for multiple interfaces in an async system.
 *
 */
public class ServiceTypeNameResults {
  
    public ServiceTypeNameResults(String query) {
    this.query = query;
  }

    public String query;
    
    public Set<String> serviceTypes = new TreeSet<String>();

}
