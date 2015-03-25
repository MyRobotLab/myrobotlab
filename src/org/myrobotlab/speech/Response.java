package org.myrobotlab.speech;

/**
 * Template for parsing JSON response
 * 
 * @author Florian Schulz
 */
public class Response {
	public class Hypotheses {
		String utterance;
		float confidence;
	}

	int status;
	String id;

	Hypotheses[] hypotheses;
}
