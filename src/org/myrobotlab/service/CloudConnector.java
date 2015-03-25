package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class CloudConnector extends Service {

	class CloudMessage {

		public String robotId;
		public String propertyId;
		public Message message;

		public CloudMessage(String robotId, String propertyId, Message message) {
			this.robotId = robotId;
			this.propertyId = propertyId;
			this.message = message;
		}

	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(CloudConnector.class);
	//
	public String robotId;

	//
	public String propertyId;

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		try {
			Runtime.start("gui", "GUIService");
			CloudConnector cloud = new CloudConnector("cloud");
			cloud.startService();
			cloud.setRobotId("incubator");

			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect("COM12");
			arduino.analogReadPollingStart(3);
			arduino.addListener("publishPin", cloud.getName(), "publishPin", Pin.class);
		} catch (Exception e) {
			Logging.logError(e);
		}

		// Runtime.createAndStart("gui", "GUIService");

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */

	}

	public CloudConnector(String n) {
		super(n);
		// load();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "cloud" };
	}

	@Override
	public String getDescription() {
		return "Cloud Connector";
	}

	@Override
	public boolean preProcessHook(Message m) {
		sendMessage(m);
		return false;
	}

	public void sendMessage(Message message) {
		try {
			String USER_AGENT = "MRL-/5.0";

			String urlParameters = "";
			String url = "http://svns.mobi:9392/robot/property/collection/update";

			Gson gson = new Gson();

			urlParameters = URLEncoder.encode(gson.toJson(new CloudMessage(robotId, propertyId, message)), "UTF-8");

			// String url = "https://selfsolve.apple.com/wcResults.do";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// add reuqest header
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			// urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			log.info("\nSending 'POST' request to URL : " + url);
			log.info("Post parameters : " + urlParameters);
			log.info("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			log.info(response.toString());

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public String setRobotId(String robotId) {
		this.robotId = robotId;
		save();
		return robotId;
	}

}
