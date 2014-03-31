package org.myrobotlab.gp;

public class SetData {

	private String label;
	private Object[] choices;
	private int[] selections;

	public SetData(String label, Object[] choices, int[] selections) {
		this.label = label;
		this.choices = choices;
		this.selections = selections;
		if (choices != null) {
			for (int i = 0; i < choices.length; i++) {
				// addItem(choices[i].toString());
			}
		}
	}

	public int countSelections() {
		if (selections == null) {
			return 0;
		} else {
			return selections.length;
		}
	}

	public Object getSelectedItem(int selectionNr) {
		if (choices == null || selections == null) {
			return null;
		} else {
			return choices[selections[selectionNr]];
		}
	}

}
