package edu.umich.soar.visualsoar.ruleeditor;

import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.*;

class SoarDocumentTest {

  @Test
  void regression55NegativeAttributeAutoJustify() throws BadLocationException {
    // caret should line up with ^ on previous line for both positive and negative att tests
    int expectedCaretPos = 86;
    String positiveText =
        "sp {rule-name\n" + "  (state <s> ^name state-name\n" + "             ^something test\n";
    String negationText =
        "sp {rule-name\n" + "  (state <s> ^name state-name\n" + "            -^something test\n";
    SoarDocument document = new SoarDocument();
    // Insert text into the document
    document.insertString(0, positiveText, null);

    // Put caret at the very end, meaning user just inserted a newline
    int caretPos = positiveText.length();
    int newCaretPos = document.autoJustify(caretPos);

    assertEquals(expectedCaretPos, newCaretPos, "Positive test: expected caret to align with ^ on previous line");

    // clear the document and test with a negated attribute
    document.remove(0, document.getLength());
    document.insertString(0, negationText, null);
    // caretPos is exactly the same as with positive test example
    newCaretPos = document.autoJustify(caretPos);

    assertEquals(expectedCaretPos, newCaretPos, "Negative test: expected caret to align with ^ on previous line");

  }
}
