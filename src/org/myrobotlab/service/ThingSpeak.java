package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

//import org.slf4j.LoggerFactory;

/**
 * @author GroG &
 * 
 *         References : http://community.thingspeak.com/documentation/api/
 * 
 */

public class ThingSpeak extends Service {

	private static final long serialVersionUID = 1L;

	// public final static Logger log =
	// LoggerFactory.getLogger(ThingSpeak.class.getCanonicalName());
	public final static Logger log = LoggerFactory.getLogger(ThingSpeak.class.getCanonicalName());
	// http://api.thingspeak.com/update?key=AO4DMKQZY4RLWNNU&field1=pin&field2=A0&field3=value&field4=345&status=boink6

	String updateURL = "http://api.thingspeak.com/update";

	String writeKey = "";

	long lastUpdate = 0;

	int intervalSeconds = 20;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			// BasicConfigurator.

			log.info("hello");

			ThingSpeak thingSpeak = new ThingSpeak("thingSpeak");
			thingSpeak.update(33);
			thingSpeak.startService();

			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public ThingSpeak(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "data", "cloud" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public Integer getIntervalSeconds() {
		return intervalSeconds;
	}

	// TODO - add averaging & timing info

	public long getLastUpdate() {
		return lastUpdate;
	}

	public String getWriteKey() {
		return writeKey;
	}

	public void saveConfig() {
		save();
	}

	public void setIntervalSeconds(int intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public void setWriteKey(String writeKey) {
		this.writeKey = writeKey;
	}

	public Integer update(Integer data) {
		return update(new Object[] { data });
	}

	public Integer update(Object[] data) {
		String result = "0";
		try {

			if (System.currentTimeMillis() - lastUpdate < intervalSeconds * 1000) {
				log.debug(String.format("not ready for posting - must be >= %d seconds", intervalSeconds));
				return 0;
			}

			if (data.length > 8) {
				log.warn("data array is larger than 8 - post will be truncated");
			}

			StringBuffer url = new StringBuffer();
			url.append(String.format("%s?key=%s", updateURL, writeKey));

			for (int i = 0; i < data.length && i < 8; ++i) {
				Object o = data[i];
				url.append(String.format("&field%d=%s", i + 1, o.toString()));
			}

			HTTPRequest request = new HTTPRequest(url.toString());
			result = request.getString();
			log.info(String.format("ThingSpeak returned %s", result));

		} catch (IOException e) {
			Logging.logError(e);
		}

		Integer ret = Integer.parseInt(result);
		if (ret > 0) {
			lastUpdate = System.currentTimeMillis();
		}
		return ret;
	}

	public Integer update(Pin pin) {
		return update(new Object[] { pin.pin, pin.value });
	}

	public Integer update(String data) {
		return update(new Object[] { data });
	}

}
