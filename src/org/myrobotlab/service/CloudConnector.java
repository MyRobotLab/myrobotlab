package org.myrobotlab.service;

import com.google.gson.Gson;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;


public class CloudConnector extends Service {

    private static final long serialVersionUID = 1L;

    public final static Logger log = LoggerFactory.getLogger(CloudConnector.class);

    public CloudConnector(String n) {
        super(n);
    }

    @Override
    public String getDescription() {

        return "used as a general template";
    }


    public void sendMessage(Message message) {
        String urlParameters = "";
        String request = "http://svns.mobi/robot/property/collection/update";
        try {
            Gson gson = new Gson();

            urlParameters = URLEncoder.encode(gson.toJson(message), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            System.out.print(e.getMessage());
        }
        try {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches (false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            connection.disconnect();
        }
        catch (ProtocolException e) {
            System.out.print(e.getMessage());
        }
        catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        }
        catch (IOException e) {
            System.out.print(e.getMessage());
        }
    }


    public boolean preProcessHook(Message m) {
        if (m.method.equals("log")) {
            invoke("log", m);
            sendMessage(m);
            return false;
        }
        return true;
    }

    public static void main(String[] args) {

        LoggingFactory.getInstance().configure();
        LoggingFactory.getInstance().setLevel(Level.WARN);

        CloudConnector template = new CloudConnector("template");
        template.startService();

        Runtime.createAndStart("gui", "GUIService");


		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */

    }


}
