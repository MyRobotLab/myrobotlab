package org.myrobotlab.speech;

/**
 * Template for parsing JSON response
 * 
 * @author Florian Schulz
 */
public class Response {
	int status;
	String id;
	Hypotheses[] hypotheses;

	public class Hypotheses {
		String utterance;
		float confidence;
	}
}
