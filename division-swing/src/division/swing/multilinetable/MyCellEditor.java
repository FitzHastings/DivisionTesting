package division.swing.multilinetable;

import division.swing.CalendarChangeListener;
import division.swing.DistanceList;
import division.swing.DivisionCalendarComboBox;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.security.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;
import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import org.apache.commons.lang3.ArrayUtils;

public class MyCellEditor extends AbstractCellEditor implements TableCellEditor {
  private MyCellRenderer renderer;
  private JComponent component;
  
  public MyCellEditor(MyCellRenderer renderer) {
    this.renderer = renderer;
  }
  
  @Override
  public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
    renderer.setEditingColumnIndex(column);
    
    if(value instanceof Timestamp)
      component = new DivisionCalendarComboBox((java.sql.Timestamp)value);
    else if(value instanceof Date)
      component = new DivisionCalendarComboBox((Date)value);
    else if(value instanceof java.sql.Date)
      component = new DivisionCalendarComboBox((java.sql.Date)value);
    else {
      component = new JTextArea();
      component.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          stopCellEditing();
        }
      });
      component.setOpaque(true);
      ((JTextArea)component).setLineWrap(true);
      ((JTextArea)component).setWrapStyleWord(true);
      ((JTextArea)component).getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
          setSize(table, row, column);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
          setSize(table, row, column);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          setSize(table, row, column);
        }
      });

      ((JTextArea)component).setText(String.valueOf(value));
      //component.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      
      setSize(table, row, column);
    }
    
    if(component instanceof DivisionCalendarComboBox) {
      ((DivisionCalendarComboBox)component).addCalendarChangeListener(new CalendarChangeListener() {
        @Override
        public void CalendarChangeDate(DistanceList dates) {
        }

        @Override
        public void CalendarSelectedDate(DistanceList dates) {
          stopCellEditing();
        }
      });
    }
    
    return component;
  }
  
  private void setSize(JTable table, int row, int column) {
    int[] heights = new int[0];
    FontMetrics metrics = component.getFontMetrics(component.getFont());
    for(int i=0;i<table.getColumnCount();i++) {
      if(renderer.getIgnoreColumns() == null || !ArrayUtils.contains(renderer.getIgnoreColumns(), i)) {
        int columnWidth = table.getColumnModel().getColumn(i).getWidth()-table.getInsets().left-table.getInsets().right;
        String text = i==column?((JTextArea)component).getText():renderer.getText(table.getValueAt(row, i));
        int heigthText = metrics.getHeight()+table.getInsets().top+table.getInsets().bottom;
        int rowCount = getRowCount(text, columnWidth, metrics);
        int rowHeight = rowCount*heigthText;
        heights = ArrayUtils.add(heights, rowHeight);
      }
    }
    Arrays.sort(heights);
    component.setSize(component.getWidth(), heights[heights.length-1]);
    table.setRowHeight(row, heights[heights.length-1]+20);
  }
  
  private int getRowCount(String text, int width, FontMetrics metrics) {
    int count = 1;
    if(text != null) {
      String rowText = "";
      StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r", true);
      while(tokenizer.hasMoreElements()) {
        String word = tokenizer.nextToken();
        if(metrics.stringWidth(word) > width) {
          if(!rowText.equals(""))
            count++;
          rowText = "";
          for(int i=0;i<word.length();i++) {
            rowText += word.charAt(i);
            if(metrics.stringWidth(rowText) > width) {
              count++;
              rowText = ""+word.charAt(i);
            }
          }

        }else {
          rowText += word;
          if(word.equals("\n")) {
            count++;
            rowText = "";
          }
        }

        if(metrics.stringWidth(rowText) > width) {
          count++;
          rowText = word;
        }
      }
    }
    return count;
  }
  
  @Override
  public Object getCellEditorValue() {
    if(component instanceof DivisionCalendarComboBox)
      return ((DivisionCalendarComboBox) component).getDate();
    else return ((JTextArea)component).getText();
  }

  /*@Override
  public boolean isCellEditable(EventObject e) {
    return true;
  }*/
}