package org.myrobotlab.swing.widget;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class JIntegerField extends JTextField {
  static class IntegerDocument extends PlainDocument {
    /**
    * 
    */
    private static final long serialVersionUID = 1L;

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      if (str != null) {
        try {
          Integer.decode(str);
          super.insertString(offs, str, a);
        } catch (NumberFormatException ex) {

        }
      }
    }
  }

  private static final long serialVersionUID = 1L;

  public JIntegerField() {
    super();
  }

  public JIntegerField(int cols) {
    super(cols);
  }

  @Override
  protected Document createDefaultModel() {
    return new IntegerDocument();
  }

  public int getInt() {
    final String text = getText();
    if (text == null || text.length() == 0) {
      return 0;
    }
    return Integer.parseInt(text);
  }

  public void setInt(int value) {
    setText(String.valueOf(value));
  }
}
