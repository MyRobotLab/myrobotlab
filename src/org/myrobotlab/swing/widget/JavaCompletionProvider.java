package org.myrobotlab.swing.widget;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
 * Implementation of AbstractCompletionProvider for standard Java keywords.
 * 
 * @author SwedaKonsult
 * 
 */
public class JavaCompletionProvider extends DefaultCompletionProvider {
  /**
   * Constructor
   */
  public JavaCompletionProvider() {
    loadCompletions();
  }

  /**
   * Returns whether the specified character is valid in an auto-completion. The
   * default implementation is equivalent to "
   * <code>Character.isLetterOrDigit(ch) || ch=='_'</code>". Subclasses can
   * override this method to change what characters are matched.
   * 
   * @param ch
   *          The character.
   * @return Whether the character is valid.
   */
  @Override
  protected boolean isValidChar(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
  }

  /**
   * Load the completions related to Java.
   */
  protected void loadCompletions() {
    // Add completions for all Java keywords. A BasicCompletion is just
    // a straightforward word completion.
    addCompletion(new BasicCompletion(this, "and"));
    addCompletion(new BasicCompletion(this, "as"));
    addCompletion(new BasicCompletion(this, "assert"));
    addCompletion(new BasicCompletion(this, "break"));
    addCompletion(new BasicCompletion(this, "class"));
    addCompletion(new BasicCompletion(this, "continue"));
    addCompletion(new BasicCompletion(this, "def"));
    addCompletion(new BasicCompletion(this, "del"));
    addCompletion(new BasicCompletion(this, "elif"));
    addCompletion(new BasicCompletion(this, "else"));
    addCompletion(new BasicCompletion(this, "except"));
    addCompletion(new BasicCompletion(this, "exec"));
    addCompletion(new BasicCompletion(this, "finally"));
    addCompletion(new BasicCompletion(this, "for"));
    addCompletion(new BasicCompletion(this, "from"));
    addCompletion(new BasicCompletion(this, "global"));
    addCompletion(new BasicCompletion(this, "if"));
    addCompletion(new BasicCompletion(this, "import"));
    addCompletion(new BasicCompletion(this, "in"));
    addCompletion(new BasicCompletion(this, "is"));
    addCompletion(new BasicCompletion(this, "lambda"));
    addCompletion(new BasicCompletion(this, "not"));
    addCompletion(new BasicCompletion(this, "or"));
    addCompletion(new BasicCompletion(this, "pass"));
    addCompletion(new BasicCompletion(this, "print"));
    addCompletion(new BasicCompletion(this, "raise"));
    addCompletion(new BasicCompletion(this, "return"));
    addCompletion(new BasicCompletion(this, "try"));
    addCompletion(new BasicCompletion(this, "while"));
    addCompletion(new BasicCompletion(this, "with"));
    addCompletion(new BasicCompletion(this, "yield"));
    /*
     * addCompletion(new BasicCompletion(this, "abstract", "blah",
     * "<html><body>hello</body></html>")); addCompletion(new
     * BasicCompletion(this, "assert")); addCompletion(new BasicCompletion(this,
     * "break")); addCompletion(new BasicCompletion(this, "case"));
     * addCompletion(new BasicCompletion(this, "catch")); addCompletion(new
     * BasicCompletion(this, "class")); addCompletion(new BasicCompletion(this,
     * "const")); addCompletion(new BasicCompletion(this, "continue"));
     * addCompletion(new BasicCompletion(this, "default")); addCompletion(new
     * BasicCompletion(this, "do")); addCompletion(new BasicCompletion(this,
     * "else")); addCompletion(new BasicCompletion(this, "enum"));
     * addCompletion(new BasicCompletion(this, "extends")); addCompletion(new
     * BasicCompletion(this, "final")); addCompletion(new BasicCompletion(this,
     * "finally")); addCompletion(new BasicCompletion(this, "for"));
     * addCompletion(new BasicCompletion(this, "goto")); addCompletion(new
     * BasicCompletion(this, "if")); addCompletion(new BasicCompletion(this,
     * "implements")); addCompletion(new BasicCompletion(this, "import"));
     * addCompletion(new BasicCompletion(this, "instanceof")); addCompletion(new
     * BasicCompletion(this, "interface")); addCompletion(new
     * BasicCompletion(this, "native")); addCompletion(new BasicCompletion(this,
     * "new")); addCompletion(new BasicCompletion(this, "package"));
     * addCompletion(new BasicCompletion(this, "private")); addCompletion(new
     * BasicCompletion(this, "protected")); addCompletion(new
     * BasicCompletion(this, "public")); addCompletion(new BasicCompletion(this,
     * "return")); addCompletion(new BasicCompletion(this, "static"));
     * addCompletion(new BasicCompletion(this, "strictfp")); addCompletion(new
     * BasicCompletion(this, "super")); addCompletion(new BasicCompletion(this,
     * "switch")); addCompletion(new BasicCompletion(this, "synchronized"));
     * addCompletion(new BasicCompletion(this, "this")); addCompletion(new
     * BasicCompletion(this, "throw")); addCompletion(new BasicCompletion(this,
     * "throws")); addCompletion(new BasicCompletion(this, "transient"));
     * addCompletion(new BasicCompletion(this, "try")); addCompletion(new
     * BasicCompletion(this, "void")); addCompletion(new BasicCompletion(this,
     * "volatile")); addCompletion(new BasicCompletion(this, "while"));
     * 
     * // Add a couple of "shorthand" completions. These completions don't //
     * require the input text to be the same thing as the replacement text.
     * addCompletion(new ShorthandCompletion(this, "sysout",
     * "System.out.println(", "System.out.println(")); addCompletion(new
     * ShorthandCompletion(this, "syserr", "System.err.println(",
     * "System.err.println("));
     */
  }
}
