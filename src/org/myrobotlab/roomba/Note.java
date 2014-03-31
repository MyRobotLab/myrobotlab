package org.myrobotlab.roomba;

/**
 * Simple wrapper for musical notes
 */
public class Note {
	public int notenum; // midi note number
	public int duration; // in milliseconds

	Note(int anotenum, int aduration) {
		notenum = anotenum;
		duration = aduration;
	}

	public String toString() {
		return "(" + notenum + "," + duration + ")";
	}

	public int toSec64ths() {
		return duration * 64 / 1000;
	}
}
