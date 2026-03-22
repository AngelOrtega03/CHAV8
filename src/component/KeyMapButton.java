package component;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class KeyMapButton extends JTextField {
    private int index;
    private int value;

    public KeyMapButton(int index) {
        this("",index);
    }

    public KeyMapButton(String value, int index) {
        super(value);
        this.index = index;
        this.value = value.charAt(0);
        this.filterConfigure();
    }

    public int getIndex() {
        return this.index;
    }

    public int getIntValue() {
        return this.value;
    }

    private void filterConfigure() {
        ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null && (fb.getDocument().getLength() + string.length()) <= 1) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null && (fb.getDocument().getLength() - length + text.length()) <= 1) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }
}
