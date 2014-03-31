package org.myrobotlab.gp;

public class Number {

	private double value;
	private double min;
	private double max;
	private String label;

	public Number(String label, double initValue, double min, double max) {
		this.label = label;
		this.min = min;
		this.max = max;
		this.value = initValue;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public int intValue() {
		return (int) value;
	}

	public double doubleValue() {
		return value;
	}

}
