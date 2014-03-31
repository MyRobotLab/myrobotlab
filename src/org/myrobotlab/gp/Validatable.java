package org.myrobotlab.gp;

/**
 * This interface can be implemented for extensions of user interface components
 * (like TextField or List) whose contents must be checked for validity before
 * they are accepted.
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public interface Validatable {

	/**
	 * Returns the descriptive text label of this component. This method is just
	 * part of the interface so that you can easily build validation error
	 * messages such as
	 * <code>"The value for " + getLabel() + " must be greater than 0"</code>.
	 * 
	 * @return the text label for the component
	 */
	public String getLabel();

	/**
	 * Formats the data from the internal representation (the model) and
	 * presents it in the user interface component (the view). This method
	 * should be called when the component becomes visible.
	 */
	void display();

	/**
	 * Checks if the user's entries or selections in the user interface
	 * component are valid. For numeric entries in a text field, you will
	 * typically check that the characters entered by the user form a valid
	 * number and that the value lies within a specific range. Usually, you'd
	 * call this method when the user selects "OK" or "Apply" in the dialog
	 * containing this component. <br>
	 * Note that the method is named "check" because "validate" is already used
	 * in an other context by AWT classes Component and Container. Your user
	 * interface elements will most probably be derived from Component, so these
	 * names would clash.
	 * 
	 * @return <code>null</code> if the contents of the component are valid, an
	 *         error message otherwise
	 */
	String check();

	/**
	 * Reads the user's entries or selections from the user interface component
	 * (the view) and transfers them to the internal representation (the model).
	 * Usually, you'd call this method when the user selects "OK" or "Apply" in
	 * the dialog containing this component.
	 */
	void accept();
}
