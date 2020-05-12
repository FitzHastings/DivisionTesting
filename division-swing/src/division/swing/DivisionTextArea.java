package division.swing;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextArea;
import javax.swing.text.Document;

public class DivisionTextArea extends JTextArea implements FocusListener {
  private String emptyText = null;
  private Color emptyColor        = Color.LIGHT_GRAY;
  private Color defaultForeground = getForeground();

  public DivisionTextArea(Document doc, String emptyText, String text, int rows, int columns) {
    super(doc, text, rows, columns);
    this.emptyText = emptyText;
    init();
  }

  public DivisionTextArea(Document doc) {
    super(doc);
    init();
  }

  public DivisionTextArea(String text, int rows, int columns) {
    super(text, rows, columns);
    init();
  }

  public DivisionTextArea(int rows, int columns) {
    super(rows, columns);
    init();
  }

  public DivisionTextArea(String emptyText, String text) {
    super(text);
    this.emptyText = emptyText;
    init();
  }

  public DivisionTextArea(String emptyText) {
    init();
    this.emptyText = emptyText;
  }
  
  public DivisionTextArea() {
    init();
  }
  
  private void init() {
    addFocusListener(this);
  }

  public Color getEmptyColor() {
    return emptyColor;
  }

  public void setEmptyColor(Color emptyColor) {
    this.emptyColor = emptyColor;
  }

  public String getEmptyText() {
    return emptyText;
  }

  public void setEmptyText(String emptyText) {
    this.emptyText = emptyText;
  }
  
  @Override
  public String getText() {
    if(emptyText != null && getForeground().equals(emptyColor))
      return "";
    return super.getText();
  }

  @Override
  public void setText(String t) {
    setForeground(defaultForeground);
    super.setText(t);
    focusLost(null);
  }
  
  @Override
  public void focusGained(FocusEvent e) {
    if(emptyText != null && super.getText().equals(emptyText)) {
      setForeground(defaultForeground);
      super.setText("");
    }
  }

  @Override
  public void focusLost(FocusEvent e) {
    if(emptyText != null && super.getText().equals("")) {
      defaultForeground = getForeground();
      setForeground(emptyColor);
      super.setText(emptyText);
    }
  }
}