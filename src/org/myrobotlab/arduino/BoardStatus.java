package org.myrobotlab.arduino;

/**
 * Status data for the running MRLComm sketch. This data will be returned
 * from the sketch to Java-land to report on the speed and current free
 * memory of the Microcontroller
 */
public class BoardStatus {
	public DeviceSummary[] deviceSummary; // deviceList with types
	public Integer sram;
	public Integer us;

	public BoardStatus(Integer us, Integer sram, DeviceSummary[] deviceSummary) {
		this.us = us;
		this.sram = sram;
		this.deviceSummary = deviceSummary;
	}
}
