package org.myrobotlab.service.interfaces;

import java.util.HashSet;
import java.util.Map;

import org.myrobotlab.net.http.Response;

public interface HTTPProcessor {

	public HashSet<String> getURIs();

	public Response serve(String uri, String method, Map<String, String> header, Map<String, String> parms, String postBody);
}
