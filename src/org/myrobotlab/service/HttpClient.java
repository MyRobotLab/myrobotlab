/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * HttpClient - wrapper for Apache HttpClient
 * 
 * @author greg (at) myrobotlab.org wrapper service for Apache HttpClient
 */
public class HttpClient extends Service {

	public final static Logger log = LoggerFactory.getLogger(HttpClient.class.getCanonicalName());

	private static final long serialVersionUID = 1L;
	Map<String, HttpData> clients = new HashMap<String, HttpData>();

	public class HttpData {
		public org.apache.http.client.HttpClient client = null;
		public HttpResponse response = null;
		public HttpUriRequest request;
		public HashMap<String, String> formFields;
	}

	public String get(String uri) throws ClientProtocolException, IOException {
		byte[] data = get(null, uri);
		if (data != null) {
			return new String(data);
		}
		return null;
	}

	public byte[] get(String name, String uri) throws ClientProtocolException, IOException {

		HttpData clientData = getClient(name);
		HttpGet request = new HttpGet(uri);
		clientData.request = request;
		return getResponse(name);
	}

	public byte[] getResponse(String name) throws IllegalStateException, IOException {
		
		HttpData clientData = clients.get(name);
		clientData.response = clientData.client.execute(clientData.request);

		InputStream is = clientData.response.getEntity().getContent();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();

		return buffer.toByteArray();
	}

	static public String parse(String in, String beginTag, String endTag) {
		if (in == null) {
			return null;
		}
		int pos0 = in.indexOf(beginTag);
		int pos1 = in.indexOf(endTag, pos0);

		String ret = in.substring(pos0 + beginTag.length(), pos1);
		ret = ret.replaceAll("<br />", "");
		return ret;

	}

	public String post(String uri, HashMap<String, String> fields) throws ClientProtocolException, IOException {
		byte[] data =  post(null, uri, fields);
		if (data != null){
			return new String(data);
		}
		return null;
	}

	public HttpData getClient(String name) {
		HttpData data = null;

		if (clients.containsKey(name)) {
			data = clients.get(name);
		} else {
			data = new HttpData();
			data.client = new DefaultHttpClient();

			/*
			 * HttpHost proxy = new HttpHost("localhost", 8888);
			 * client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
			 * proxy);
			 */

			((DefaultHttpClient) data.client).setRedirectStrategy(new LaxRedirectStrategy());
			clients.put(name, data);
		}
		//return data.client;
		return data;
	}

	public byte[] post(String name, String uri, HashMap<String, String> fields) throws ClientProtocolException, IOException {
		HttpData httpData = getClient(name);
		HttpPost post = new HttpPost(uri);

		if (fields != null) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(fields.size());
			for (String nvPairKey : fields.keySet()) {
				nameValuePairs.add(new BasicNameValuePair(nvPairKey, fields.get(nvPairKey)));
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			}
		}
		
		httpData.request = post;
		return getResponse(name);
	}

	public HttpClient(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "connectivity" };
	}

	@Override
	public String getDescription() {
		return "an HTTP client, used to fetch information on the web";
	}

	public byte[] post(String name, String uri, String data) throws ClientProtocolException, IOException {
		return post(name, uri, data.getBytes());
	}

	public byte[] post(String uri, byte[] data) throws ClientProtocolException, IOException {
		return post(null, uri, data);
	}

	public byte[] post(String name, String uri, byte[] data) throws ClientProtocolException, IOException {

		HttpData httpData = getClient(name);
		HttpPost post = new HttpPost(uri);
		post.setEntity(new ByteArrayEntity(data));
		httpData.request = post;
		return getResponse(name);
	}
	
	public int getStatusCode(){
		return getStatusCode(null);
	}

	public int getStatusCode(String name) {
		if (!clients.containsKey(name) || clients.get(name).response == null || clients.get(name).response.getStatusLine() == null){
			return -1;
		}
		return clients.get(name).response.getStatusLine().getStatusCode();
	}

	
	public String post(String uri) throws ClientProtocolException, IOException {
		return post(null, uri);
	}


	public String post(String name, String uri) throws ClientProtocolException, IOException {
		HttpData data = getClient(name);
		byte[] ret = null;
		if (data.formFields != null){
			ret = post(name, uri, data.formFields);
			
		} else {
			ret = post(name, uri, (byte[])null);
		}
		
		if (ret != null){
			return new String(ret);
		}
		
		return null;
		
	}

	
	public String addFormField(String key, String value){
		return addFormField(null, key, value);
	}
	
	public String addFormField(String name, String key, String value) {
		HttpData data = getClient(name);
		if (data.formFields == null){
			data.formFields = new HashMap<String,String>();
		}
		
		return data.formFields.put(key, value);
	}

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			HttpClient client = (HttpClient) Runtime.start("client", "HttpClient");

			// TODO - getByteArray(...)
			String index = client.get("https://servizizucchetti.decathlon.com/portale/index.htm");
			log.info(index);

			String username = "ADIDON26";
			String password = "CRICETO4";
			
			client.addFormField("m_cUserName", username);
			client.addFormField("m_cPassword", password);
			client.addFormField("w_Modal", "N");
			client.addFormField("wSHOWSENDMYPWD", "true");
			client.addFormField("mylink", "M");
			client.addFormField("m_cFailedLoginReason", "");
			client.addFormField("ssotrust", "");
			client.addFormField("GWINLOGON", "");
			client.addFormField("g_codute", "0.0");
			client.addFormField("m_cAction", "login");
			client.addFormField("m_cURL", "");
			client.addFormField("m_cURLOnError", "jsp%2Flogin.jsp");
			client.addFormField("error", "0");
			client.addFormField("m_cForceLogin", "");
			client.addFormField("w_FirstCodAzi", "zDEMO");
			client.addFormField("g_UserCode", "-1");
			client.addFormField("g_UserName", "");
			client.addFormField("ssoStatus", "0");
			client.addFormField("m_cInstance", "");
			client.addFormField("m_cCaptcha", "");
			client.addFormField("g_codazi", "zDEMO");
			client.addFormField("Nodes", "t");
			client.addFormField("memo", "%2C");
			client.addFormField("TITOLO", "f");
			client.addFormField("GLOGOLGINURL", "..%2Floghi%2Flogo+aziendale.png");
			client.addFormField("ERM_GANVERATT", "060600");
			client.addFormField("mylang", "");
			client.addFormField("browserlang", "");
			client.addFormField("GLOGOLOGIN", "");
			client.addFormField("g_UserLang", "");
			client.addFormField("GERMNAME", "HRPortal");
		
			String response = client.post("https://servizizucchetti.decathlon.com/portale/servlet/cp_login");
			int code = client.getStatusCode();
			log.info("code " + code);
			log.info(new String(response));
			
			client.addFormField("m_cID","e2b5dc19ab1f1a933d1beb7e734a340d");
			client.addFormField("verso","E");
			response = client.post("https://servizizucchetti.decathlon.com/portale/servlet/cp_login");
			code = client.getStatusCode();
			log.info("code " + code);
			log.info(new String(response));
			

			client.addFormField("ADATE","2015-10-26");
			client.addFormField("cmdhash","5ddbddb9d57dc9644e08bc3ff5d13446");
			client.addFormField("count","false");
			client.addFormField("rows","16");
			client.addFormField("sqlcmd","ushp_qtimbrus");
			client.addFormField("startrow","0");
			response = client.post("https://servizizucchetti.decathlon.com/portale/servlet/SQLDataProviderServer");
			code = client.getStatusCode();
			log.info("code " + code);
			log.info(new String(response));
			
			client.addFormField("m_cID","d9b7ccef224f3dc50a12895fd036fd90");
			response = client.post("https://servizizucchetti.decathlon.com/portale/servlet/SPPostinCount");
			code = client.getStatusCode();
			log.info("code " + code);
			log.info(new String(response));

			
			

			log.info("---------------------------------------HERE----------------------------------------------");

			/*
			 * String google = new
			 * String(HttpClient.get("http://www.google.com/"));
			 * log.info(google);
			 * 
			 * // String ntest = new String(HttpClient.get("nullTest")); //
			 * log.info(ntest);
			 * 
			 * String script = new String(HttpClient.get(
			 * "https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/master/home/hairygael/InMoov2.full3.byGael.Langevin.1.py"
			 * )); log.info(script);
			 * 
			 * HashMap<String, String> params = new HashMap<String, String>();
			 * params.put("p", "apple"); google = new
			 * String(HttpClient.post("http://www.google.com", params));
			 * log.info(google);
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}

	}
	
}
