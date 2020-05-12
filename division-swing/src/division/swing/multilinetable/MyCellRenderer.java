package division.swing.multilinetable;

import division.swing.table.CellColorController;
import division.util.Utility;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import org.apache.commons.lang3.ArrayUtils;

public class MyCellRenderer extends JTextArea implements TableCellRenderer {
  private int editingColumnIndex = -1;
  private CellColorController cellColorController;
  private int[] ignoreColumns = new int[0];
  private JTable table;
  
  public MyCellRenderer() {
    this(null);
  }
  
  public MyCellRenderer(int[] ignoreColumns) {
    if(ignoreColumns != null)
      this.ignoreColumns = ignoreColumns;
    setOpaque(true);
    setLineWrap(true);
    setWrapStyleWord(true);
  }

  public int[] getIgnoreColumns() {
    return ignoreColumns;
  }

  public void setIgnoreColumns(int[] ignoreColumns) {
    this.ignoreColumns = ignoreColumns;
  }
  
  public CellColorController getCellColorController() {
    return cellColorController;
  }

  public void setCellColorController(CellColorController cellColorController) {
    this.cellColorController = cellColorController;
  }

  public int getEditingColumnIndex() {
    return editingColumnIndex;
  }

  public void setEditingColumnIndex(int editingColumnIndex) {
    this.editingColumnIndex = editingColumnIndex;
  }

  @Override
  public Component getTableCellRendererComponent(final JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if(this.table == null) {
      this.table = table;
      table.getModel().addTableModelListener(new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
          validateRows(table);
        }
      });
    }
    if(value instanceof Timestamp) {
      setText(((Timestamp)value).toLocalDateTime().format(DateTimeFormatter.ofPattern("дата:dd.MM.yyyy\nвремя:HH:mm:ss")));
    }else if(value instanceof Date)
      setText(Utility.format((Date)value));
    else if(value instanceof java.sql.Date)
      setText(Utility.format((java.sql.Date)value));
    else setText(String.valueOf(value));
    if(editingColumnIndex == -1)
      table.setRowHeight(row, getRowHeight(table, row));
    
    setComponentColor(this, isSelected&&!table.isCellEditable(row, column)?table.getSelectionBackground():table.getBackground());
    
    if(cellColorController != null) {
      setComponentColor(this, cellColorController.getCellColor(table, row, column, isSelected, hasFocus));
      setForeground(cellColorController.getCellTextColor(table, row, column, isSelected, hasFocus));
    }
    
    return this;
  }
  
  public String getText(Object value) {
    if(value instanceof Timestamp)
      return Utility.format((Timestamp)value);
    if(value instanceof Date)
      return Utility.format((Date)value);
    if(value instanceof java.sql.Date)
      return Utility.format((java.sql.Date)value);
    else return String.valueOf(value);
  }
  
  public void validateRows(final JTable table) {
    SwingUtilities.invokeLater(() -> {
      for(int i=0;i<table.getRowCount();i++)
        table.setRowHeight(i, getRowHeight(table, i));
    });
  }
  
  private int getRowHeight(JTable table, int row) {
    int[] heights = new int[0];
    FontMetrics metrics = table.getFontMetrics(table.getFont());
    for(int i=0;i<table.getColumnCount();i++) {
      if(ignoreColumns == null || !ArrayUtils.contains(ignoreColumns, i)) {
        int columnWidth = table.getColumnModel().getColumn(i).getWidth()-table.getInsets().left-table.getInsets().right;
        String text = getText(table.getValueAt(row,i));
        int heigthText = metrics.getHeight()+table.getInsets().top+table.getInsets().bottom;
        int rowCount = getRowCount(text, columnWidth-1, metrics);
        int rowHeight = rowCount*heigthText;
        heights = ArrayUtils.add(heights, rowHeight);
      }
    }
    Arrays.sort(heights);
    return heights[heights.length-1]+20;
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
  
  private void setComponentColor(JComponent com, Color c) {
    com.setBackground(c);
    for(Component component:com.getComponents())
      if(component instanceof JComponent)
        setComponentColor((JComponent)component,c);
      else component.setBackground(c);
  }
}