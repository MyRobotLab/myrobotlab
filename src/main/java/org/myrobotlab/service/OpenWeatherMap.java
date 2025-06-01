package org.myrobotlab.service;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.OpenWeatherMapConfig;
import org.slf4j.Logger;

/**
 * A service to query into OpenWeatherMap to get the current weather. For more
 * info check out http://openweathermap.org This service requires an API key
 * that is free to register for from Open Weather Map. RECENT CHANGES BECAUSE
 * API LIMITATION : period is 3 hours per index, 40 is maximum for free api key
 * ( 3 hours TO 5 days forecast )
 * 
 */
  public class OpenWeatherMap extends HttpClient<OpenWeatherMapConfig>  {

  private static final long serialVersionUID = 1L;
  private String apiForecast = "http://api.openweathermap.org/data/2.5/forecast/?q=";
  private String units = "imperial"; // or metric
  private String localUnits = "fahrenheit"; // or celcius
  private String lang = "en";
  private String location = null;// "Paris,FR";
  private Integer period = 1; // next 3 hours by default

  // OWM objects
  private Integer weatherCode = 0;
  private String weatherDescription = "error";
  private Double degrees = -459.67;
  private Double minDegrees = -459.67;
  private Double maxDegrees = -459.67;
  private Double humidity = 0.0;
  private Double pressure = 0.0;
  private Double windSpeed = 0.0;
  private Double windOrientation = 0.0;

  public final static Logger log = LoggerFactory.getLogger(OpenWeatherMap.class);

  public OpenWeatherMap(String n, String id) {
    super(n, id);
  }

  /**
   * Return a json from OWM server
   */
  private JSONObject getJsonFromOwm(int hourPeriod) {
    String apiUrl;
    JSONObject obj = null;
    try {
      apiUrl = apiForecast + URLEncoder.encode(location, "utf-8") + "&appid=" + getKey() + "&mode=json&units=" + units + "&lang=" + lang + "&cnt=" + hourPeriod;
      String response = get(apiUrl);
      log.info("apiUrl: {}", apiUrl);
      log.info("Response: {}", response);
      obj = new JSONObject(response);
      if (obj.getInt("cod") != 200) {
        error(obj.getString("message"));
      }
    } catch (Exception e) {
      error("Cannot get json from OWM : %s", e);
      e.printStackTrace();
    }
    return obj;
  }

  /**
   * retrieve a string list of weather for the period indicated by hourPeriod 1
   * greater or equal hourPeriod is 3 hours per index is 24 hours is 8.
   * 
   * @return forcast info
   */
  public String[] fetchForecast() {
    String[] result = new String[11];
    if (units.equals("metric")) {
      // for metric, celsius
      localUnits = "celsius";
    }
    JSONObject jsonObj = getJsonFromOwm(period);
    if (jsonObj == null) {
      error("OWM can't parse a NULL json !");
      return null;
    } else {

      // log.info(jsonObj.toString());
      // Getting the list node
      JSONArray list = null;
      try {
        list = jsonObj.getJSONArray("list");
      } catch (Exception e) {
        if (jsonObj.toString().contains("city not found")) {
          error("OpenWeatherMap : The city : " + location + " is not recognized !");
        } else if (jsonObj.toString().contains("Invalid API key")) {
          error("OpenWeatherMap : Invalid API key !");
        } else {
          error("OpenWeatherMap error %s", e);
        }
        return null;
      }
      // Getting the required element from list by dayIndex
      JSONObject item;
      try {
        item = list.getJSONObject(period - 1);

        result[0] = item.getJSONArray("weather").getJSONObject(0).get("description").toString();
        JSONObject temp = item.getJSONObject("main");
        result[1] = temp.get("temp").toString();
        result[2] = location;
        result[3] = item.getJSONArray("weather").getJSONObject(0).get("id").toString();
        result[4] = temp.get("pressure").toString();
        result[5] = temp.get("humidity").toString();
        result[6] = temp.get("temp_min").toString();
        result[7] = temp.get("temp_max").toString();
        result[8] = item.getJSONObject("wind").get("speed").toString();
        result[9] = item.getJSONObject("wind").get("deg").toString();
        result[10] = localUnits;

        weatherDescription = result[0];
        degrees = Double.parseDouble(result[1]);
        weatherCode = Integer.parseInt(result[3]);
        pressure = Double.parseDouble(result[4]);
        humidity = Double.parseDouble(result[5]);
        minDegrees = Double.parseDouble(result[6]);
        maxDegrees = Double.parseDouble(result[7]);
        windSpeed = Double.parseDouble(result[8]);
        windOrientation = Double.parseDouble(result[9]);

      } catch (JSONException e) {
        error("Problem parsing OWM json parameters : %s", e);
      }

    }
    return result;
  }

  // ---------------------------------------------------------------
  // setters & getters
  // ---------------------------------------------------------------

  /**
   * @param units
   *          The units, can be either imperial or metric.
   */
  public void setUnits(String units) {
    this.units = units;
  }

  /**
   * @param apiKey
   *          REQUIRED: specify your API key with this method.
   */
  public void setKey(String apiKey) {
    Security security = Runtime.getSecurity();
    security.setKey("OPENWEATHERMAP", apiKey);
  }

  // TODO use locale
  public void setLang(String lang) {
    this.lang = lang;
  }

  /**
   * @param location
   *          format : town,country code
   */
  public void setLocation(String location) {
    this.location = location;
    if (location != null && !location.contains(",")) {
      warn("Recommended location for OWM is TOWN,COUNTRY CODE, exemple : paris,FR");
    }
  }

  /**
   * @param period
   *          period : integer from 1 to 40 ( 1 = 3 hours )
   */
  public void setPeriod(Integer period) {
    if ((period > 0) && (period <= 40)) {
      this.period = period;
    } else {
      error("OpenWeatherMap : Index is out of range ( 1 to 40 ), hourPeriod is 3 hours per index, 40 is maximum for free api key ( 5 days )");
    }
  }

  public String getKey() {
    Security security = Runtime.getSecurity();
    return security.getKey("OPENWEATHERMAP");
  }

  public String getLocation() {
    return location;
  }

  public String getUnits() {
    return units;
  }

  public Integer getWeatherCode() {
    fetchForecast();
    return weatherCode;
  }

  public String getWeatherDescription() {
    fetchForecast();
    return weatherDescription;
  }

  public Double getDegrees() {
    fetchForecast();
    return degrees;
  }

  public Double getMinDegrees() {
    fetchForecast();
    return minDegrees;
  }

  public Double getMaxDegrees() {
    fetchForecast();
    return maxDegrees;
  }

  public Double getWindSpeed() {
    fetchForecast();
    return windSpeed;
  }

  public Double getWindOrientation() {
    fetchForecast();
    return windOrientation;
  }

  public Double getHumidity() {
    fetchForecast();
    return humidity;
  }

  public Double getPressure() {
    fetchForecast();
    return pressure;
  }

  public String getLocalUnits() {
    return localUnits;
  }

  public String getApiKey() {
    return Runtime.getSecurity().getKey("OPENWEATHERMAP");
  }

  @Override
  public OpenWeatherMapConfig getConfig() {
    super.getConfig();
    // FIXME - remove local fields in favor of only config
    config.currentUnits = units;
    config.currentTown = location;
    return config;
  }

  @Override
  public OpenWeatherMapConfig apply(OpenWeatherMapConfig c) {
    super.apply(c);
    // FIXME - remove local fields in favor of only config
    if (c.currentUnits != null) {
      setUnits(c.currentUnits);
    }
    if (c.currentTown != null) {
      setLocation(c.currentTown);
    }
    return c;
  }

  public static void main(String[] args) {
    OpenWeatherMap owm = (OpenWeatherMap) Runtime.start("weather", "OpenWeatherMap");
    // owm.setKey("XXX");
    // owm.setLocation("Paris,FR");
    owm.setLocation("Portland,US");
    // owm.setPeriod(1);

    // tomorrow is 8 ( 3 * 8 )
    // owm.setUnits("metric");
    String sentence = "( Raw code : " + owm.getWeatherCode() + "), In " + owm.getLocation() + " the weather is " + owm.getWeatherDescription() + ".  " + owm.getDegrees()
        + " degrees " + owm.getLocalUnits() + " humidity " + owm.getHumidity() + " Min Degrees " + owm.getMinDegrees() + " max Degrees " + owm.getMaxDegrees() + " pressure "
        + owm.getPressure() + " Wind Speed " + owm.getWindSpeed() + " Wind Orientation " + owm.getWindOrientation();
    log.info(sentence);
  }

}
