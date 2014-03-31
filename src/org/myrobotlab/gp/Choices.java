package org.myrobotlab.gp;

import java.awt.Choice;

/**
 * Combines model and view for a choice. The model is an array of Objects, the
 * view presents their string representations and lets the user select one of
 * them.
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public class Choices extends Choice implements Validatable {

	private String label;
	private Object[] choices;
	private int selection;

	/**
	 * @param label
	 *            a descriptive label for the choice component
	 * @param choices
	 *            an array of Objects to pick one from
	 * @param selection
	 *            the index of the initially selected item
	 */
	public Choices(String label, Object[] choices, int selection) {
		super();
		this.label = label;
		this.choices = choices;
		this.selection = selection;
		if (choices != null) {
			for (int i = 0; i < choices.length; i++) {
				addItem(choices[i].toString());
			}
		}
		display();
	}

	public void display() {
		select(selection);
	}

	/**
	 * Checks if the users choice is valid. In my simple case where the user
	 * picks an item from a fixed set of choices, this is always true,
	 * therefore:
	 * 
	 * @return <code>null</code>
	 */
	public String check() {
		return null;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Transfers the user's choice -- the index of the selected item -- from the
	 * visible component to the internal model.
	 */
	public void accept() {
		selection = getSelectedIndex();
	}

	/**
	 * Returns the currently selected item from the array of choices.
	 * 
	 * @return the current selection or <code>null</code> if no item is selected
	 */
	public Object getSelection() {
		if (choices != null) {
			return choices[selection];
		} else {
			return null;
		}
	}
}
