/**
 * 
 */
package org.myrobotlab.sensor;

import java.util.Map;

import com.google.gson.internal.LinkedTreeMap;

/**
 * @author GroG
 *
 */
public class SensorData {

	long startTs = 0;
	long endTs = 0;
	public Map<String, Object> data = new LinkedTreeMap<String, Object>();

	public long getDeltaMs() {
		return endTs - startTs;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
