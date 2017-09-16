package org.myrobotlab.service;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
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
  private String apiForecast = "http://api.openweathermap.org/data/2.5/forecast/daily?q=";
  private String units = "imperial";
  private String lang = "en";
  private String apiKey = "GET_API_KEY_FROM_OPEN_WEATHER_MAP";
  public final static Logger log = LoggerFactory.getLogger(OpenWeatherMap.class);
  
  public OpenWeatherMap(String reservedKey) {
    super(reservedKey);
  }

  /**
    Return a sentence describing the weather 
   */
	private JSONObject fetch(String location) throws ClientProtocolException, IOException, JSONException {
    String apiUrl = apiBase + URLEncoder.encode(location,"utf-8") + "&appid=" + apiKey + "&mode=json&units=" + units + "&lang=" + lang; 
    String response = this.get(apiUrl);
    log.debug("Respnse: {}" , response);
    JSONObject obj = new JSONObject(response); 
    return obj;
  }

  /**
    Return a sentence describing the forecast weather
   */
  private JSONObject fetch(String location, int nbDay) throws ClientProtocolException, IOException, JSONException {
    String apiUrl = apiForecast + URLEncoder.encode(location,"utf-8") + "&appid=" + apiKey + "&mode=json&units=" + units + "&lang=" + lang + "&cnt=" + nbDay; 
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
  
  /**
   Given a string of the form returned by the api call:
   http://api.openweathermap.org/data/2.5/forecast/daily?q=location&appid=XXXXXXXXXXXXXXXXXXXXXXXXX&mode=json&units=metric&cnt=3
   
   retrieve a string list of weather for the day indicated by dayIndex
   0 <= dayIndex <= 16 (Note: 0 would refer to the first day).

   {"city":{"id":2802985,"name":"location","coord":{"lon":5.8581,"lat":50.7019},"country":"FR","population":0},
    "cod":"200",
    "message":0.1309272,
    "cnt":3,
    "list":[
    {"dt":1505386800,
    "temp":{"day":11.62,"min":10.59,"max":12.39,"night":11.01,"eve":10.59,"morn":10.98},
    "pressure":1006.58,
    "humidity":100,
    "weather":[{"id":502,"main":"Rain","description":"heavy intensity rain","icon":"10d"}],
    "speed":6.73,
    "deg":259,
    "clouds":92,
    "rain":17.33},

    {"dt":1505473200,
    "temp":{"day":14.96,"min":8.43,"max":14.96,"night":8.43,"eve":12.71,"morn":9.46},
    "pressure":1014.87,
    "humidity":89,
    "weather":[{"id":500,"main":"Rain","description":"light rain","icon":"10d"}],
    "speed":4.8,
    "deg":249,
    "clouds":20,
    "rain":0.36},

    {"dt":1505559600,
    "temp":{"day":13.85,"min":8.09,"max":14.5,"night":8.44,"eve":12.5,"morn":8.09},
    "pressure":1013.37,
    "humidity":95,
    "weather":[{"id":501,"main":"Rain","description":"moderate rain","icon":"10d"}],
    "speed":5.38,
    "deg":241,
    "clouds":44,
    "rain":5.55}
    ]}
   */
  public String[] fetchForecast(String location, int dayIndex) throws ClientProtocolException, IOException, JSONException {
    String[] result = new String[10];
    
    result[0] = location;

    if ((dayIndex >= 0) && (dayIndex <= 16)) {
      JSONObject jsonObj = fetch(location, (dayIndex + 1));
      
      // Getting the list node
      JSONArray list = jsonObj.getJSONArray("list");
      // Getting the required element from list by dayIndex
      JSONObject item = list.getJSONObject(dayIndex);

      // Weather
      result[1] = item.getJSONArray("weather").getJSONObject(0).get("id").toString();
      result[2] = item.getJSONArray("weather").getJSONObject(0).get("description").toString();
      // Temperature
      JSONObject temp=item.getJSONObject("temp");
      result[3] = temp.get("day").toString();
      result[4] = temp.get("min").toString();
      result[5] = temp.get("max").toString();
      // Rest
      result[6] = item.get("pressure").toString();
      result[7] = item.get("humidity").toString();
      result[8] = item.get("speed").toString();
      result[9] = item.get("deg").toString();
    }
    else
    {
      result[1] = "Index error";
    }
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