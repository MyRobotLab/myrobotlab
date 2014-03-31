package org.myrobotlab.gp;

import java.io.StreamTokenizer;
import java.io.StringBufferInputStream;

public class Points {

	private String label;
	private RealPoint[] data;
	private String rawText;

	public Points(String label, String rawText) {
		this.label = label;
		this.rawText = rawText;
		this.data = read(rawText);

	}

	public RealPoint[] get() {
		return data;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Checks if the contents of the edit field are valid, i.e. the user entered
	 * at least 10 x-y pairs.
	 * 
	 * @return <code>null</code> if valid, an error message otherwise.
	 */
	public String check() {
		RealPoint[] temp = read(getText());
		boolean ok = (temp != null && temp.length >= 10);
		if (ok) {
			return null;
		} else {
			return "\"" + label + "\" must contain at least 10 x-y-value pairs.";
		}
	}

	/**
	 * Copies the raw text and the x-y values (read from the same raw text) from
	 * the edit field into the model. This method assumes that the contents of
	 * the field have already been validated.
	 */
	public void accept() {
		rawText = getText();
		data = read(rawText);
	}

	public String getText() {
		String x = "0.003125 -0.125" + "0.003125 -0.1333333333333333" + "0.009375 -0.1166666666666667" + "0.01875 -0.10833333333333339" + "0.025 -0.10833333333333339"
				+ "0.028125 -0.10833333333333339" + "0.034375 -0.1166666666666667" + "0.040625 -0.1166666666666667" + "0.05 -0.125" + "0.053125 -0.125"
				+ "0.0625 -0.1333333333333333" + "0.0875 -0.1499999999999999" + "0.1 -0.15833333333333344" + "0.121875 -0.16666666666666674" + "0.13125 -0.17500000000000004"
				+ "0.1625 -0.17500000000000004" + "0.178125 -0.16666666666666674" + "0.203125 -0.1416666666666666" + "0.23125 -0.10000000000000009" + "0.25 -0.08333333333333326";

		return x;

	}

	static final int MAX_NR_OF_FITNESS_CASES = 100;

	/**
	 * Decodes the contents of the edit field into a series of x-y value pairs.<br>
	 * Note that the strean tokenizer used here does not understand numbers with
	 * an exponent (like 1.23E+3).
	 * 
	 * @return an array of x-y points
	 */
	private RealPoint[] read(String s) {
		StringBufferInputStream stream = new StringBufferInputStream(s);
		StreamTokenizer tokenizer = new StreamTokenizer(stream);
		RealPoint[] temp = new RealPoint[MAX_NR_OF_FITNESS_CASES];
		int count = 0;
		try {
			int token;
			do {
				double x;
				double y;
				token = tokenizer.nextToken();
				if (token == StreamTokenizer.TT_NUMBER) {
					x = tokenizer.nval;
					token = tokenizer.nextToken();
					if (token == StreamTokenizer.TT_NUMBER) {
						y = tokenizer.nval;
						temp[count] = new RealPoint(x, y);
						count++;
					}
				}
			} while (count < MAX_NR_OF_FITNESS_CASES && token != StreamTokenizer.TT_EOF);
		} catch (Exception e) {
			;
		}
		RealPoint[] result = new RealPoint[count];
		System.arraycopy(temp, 0, result, 0, count);
		return result;
	}

}
