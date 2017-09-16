package org.myrobotlab.service;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * A service to query into OpenWeatherMap to get the current weather.
 * For more info check out http://openweathermap.org 
 * This service requires an API key that is free to register for from Open Weather Map.
 * 
 */
public class OpenWeatherMap extends HttpClient {

  private static final long serialVersionUID = 1L;
  private String apiBase = "http://api.openweathermap.org/data/2.5/weather?q=";
  private String units = "imperial";
  private String lang = "en";
  private String apiKey = "GET_API_KEY_FROM_OPEN_WEATHER_MAP";
  public final static Logger log = LoggerFactory.getLogger(OpenWeatherMap.class);
  
  public OpenWeatherMap(String reservedKey) {
    super(reservedKey);
  }

  // Return a sentence describing the weather 
  
  private JSONObject fetch(String location) throws ClientProtocolException, IOException, JSONException {
	  String apiUrl = apiBase + URLEncoder.encode(location,"utf-8") +  "&appid=" + apiKey + "&units=" + units + "&lang=" + lang; 
	  String response = this.get(apiUrl);
	  log.debug("Respnse: {}" , response);
	  JSONObject obj = new JSONObject(response); 
	  return obj;
	  
  }
  

  public String[] fetchRaw(String location) throws ClientProtocolException, IOException, JSONException {
	  String[] result = new String[10];
	  JSONObject obj = fetch(location);
	  result[0] = obj.getJSONArray("weather").getJSONObject(0).get("description").toString();
	  result[1] = obj.getJSONObject("main").get("temp").toString();
	  result[2] = location;
	  result[3] = obj.getJSONArray("weather").getJSONObject(0).get("id").toString();
	  result[4] = obj.getJSONObject("main").get("pressure").toString();
	  result[5] = obj.getJSONObject("main").get("humidity").toString();
	  result[6] = obj.getJSONObject("main").get("temp_min").toString();
	  result[7] = obj.getJSONObject("main").get("temp_max").toString();
	  result[8] = obj.getJSONObject("wind").get("speed").toString();
	  result[9] = obj.getJSONObject("wind").get("deg").toString();
	  return result;
  	}
  
  public String fetchWeather(String location) throws ClientProtocolException, IOException, JSONException {
    String[] result=fetchRaw(location);
    String description=result[0];
    String degrees=result[1];
    
    // if we're imperial it's fahrenheit
    String localUnits = "fahrenheit";
    if (units.equals("metric")) {
      // for metric, celsius
      localUnits = "celsius";
    }
    int deg = (int)Double.valueOf(degrees.toString()).doubleValue();
    String sentence = "In " + location + " the weather is " + description + ".  " + deg + " degrees " + localUnits;
    return sentence;
  }
  
  public String getApiBase() {
    return apiBase;
  }

  /**
   * @param apiBase The base url that is assocaited with the open weather map api.
   */
  public void setApiBase(String apiBase) {
    this.apiBase = apiBase;
  }

  public String getUnits() {
    return units;
  }

  /**
   * @param units The units, can be either imperial or metric.
   */
  public void setUnits(String units) {
    this.units = units;
  }

  public String getApiKey() {
    return apiKey;
  }

  /**
   * @param apiKey REQUIRED: specify your API key with this method.
   */
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
  
  public void setLang(String lang) {
	    this.lang = lang;
	  }
	  
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(OpenWeatherMap.class.getCanonicalName());
    meta.addDescription("This service will query OpenWeatherMap for the current weather.  Get an API key at http://openweathermap.org/");
    meta.addCategory("data","weather");
    meta.setCloudService(true);
    return meta;
  }
  
  
  public static void main(String[] args) {
    
    OpenWeatherMap owm = new OpenWeatherMap("weather");
    owm.startService();
    try {
      String response = owm.fetchWeather("Boston, MA");
    } catch (ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
}
