package division.swing.table.filter;

import division.swing.DivisionTextField;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FilterTextField extends DivisionTextField implements FilterComponent, DocumentListener {
  private FilterButton filterButton;
  private TableFilter tableFilter;
  private int column;
  private char[] keyChars = new char[]{'(',')','^','$','.','[',']','{','}','*','+','?','|','\\'};
  
  public FilterTextField(TableFilter filter, Type type, int column) {
    this(filter, type, -1, column);
  }

  public FilterTextField(TableFilter filter, Type type, int maxLength, int column) {
    super(type, maxLength);
    this.tableFilter = filter;
    this.column = column;
    filterButton = new FilterButton(this);
    setMinimumSize(new Dimension(0,0));
    getDocument().addDocumentListener(this);
  }
  
  @Override
  public void insertUpdate(DocumentEvent e) {
    if(isOnlineFilter())
      startFilter();
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    if(isOnlineFilter())
      startFilter();
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    if(isOnlineFilter())
      startFilter();
  }
  
  private String getRegex(String regex) {
    String s = "";
    char[] chars = cleanString(regex).toCharArray();
    for(int i=0;i<chars.length;i++) {
      if(Character.isLetter(chars[i]))
        s += "("+String.valueOf(chars[i]).toLowerCase()+"|"+String.valueOf(chars[i]).toUpperCase()+")";
      else s += chars[i];
    }
    return s;
  }

  private String cleanString(String string) {
    String str = "";
    char[] chars = string.toCharArray();
    for(int i=0;i<chars.length;i++) {
      boolean is = false;
      for(char c:keyChars) {
        if(chars[i] == c) {
          str += "\\"+chars[i];
          is = true;
        }
      }
      if(!is)
        str += chars[i];
    }
    return str;
  }

  @Override
  public RowFilter getFilter() {
    if(isFilter()) {
      if(getType() == DivisionTextField.Type.ALL) {
        String reg = getRegex(getText());
        if(reg.startsWith("!="))
          return RowFilter.notFilter(RowFilter.regexFilter(reg.substring(2),new int[]{column}));
        else return RowFilter.regexFilter(reg,new int[]{column});
      }else if(getType() == DivisionTextField.Type.FLOAT || getType() == DivisionTextField.Type.INTEGER) {
        return RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL,Float.valueOf(getText()),new int[]{column});
      }
    }
    return null;
  }
  
  @Override
  public int getColumn() {
    return column;
  }

  @Override
  public FilterButton getFilterButton() {
    return filterButton;
  }

  @Override
  public TableFilter getTableFilter() {
    return tableFilter;
  }

  @Override
  public boolean isFilter() {
    return !getText().equals("");
  }

  @Override
  public JComponent packComponent() {
    Rectangle rec = tableFilter.getTableHeader().getHeaderRect(tableFilter.getTable().convertColumnIndexToView(column));
    setPreferredSize(new Dimension(rec.width-filterButton.getBounds().width-filterButton.getBounds().x-2, filterButton.getPreferredSize().height));
    return this;
  }

  @Override
  public void clearFilter() {
    setText("");
  }

  @Override
  public void startFilter() {
    tableFilter.filter();
  }
}
