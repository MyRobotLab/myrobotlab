package org.myrobotlab.speech;

import java.io.IOException;

import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;

/**
 * A Dialog node behavior that loads a completely new grammar upon entry into
 * the node
 */
public class NewGrammarDialogNodeBehavior extends DialogNodeBehavior {

	/**
	 * creates a NewGrammarDialogNodeBehavior
	 * 
	 * @param grammarName
	 *            the grammar name
	 */
	public NewGrammarDialogNodeBehavior() {
	}

	/**
	 * Returns the name of the grammar. The name of the grammar is the same as
	 * the name of the node
	 * 
	 * @return the grammar name
	 */
	public String getGrammarName() {
		return getName();
	}

	/**
	 * Called with the dialog manager enters this entry
	 */
	@Override
	public void onEntry() throws IOException {
		super.onEntry();
		try {
			getGrammar().loadJSGF(getGrammarName());
		} catch (JSGFGrammarParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSGFGrammarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
