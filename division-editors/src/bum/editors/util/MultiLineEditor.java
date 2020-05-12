package bum.editors.util;

import division.swing.DivisionTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.StringTokenizer;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import org.apache.commons.lang3.ArrayUtils;

public class MultiLineEditor  extends AbstractCellEditor implements TableCellEditor {
  private JTextArea component;
  private int modelRow;
  private int modelColumn;
  private JTable table;

  public MultiLineEditor(JTable table) {
    this.table = table;
  }

  @Override
  public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
    component = new JTextArea();

    component.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        changeSize(table, row, column);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        changeSize(table, row, column);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        changeSize(table, row, column);
      }
    });

    component.setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
    component.setOpaque(true);
    component.setLineWrap(true);
    component.setWrapStyleWord(true);
    component.setText((value == null) ? "" : value.toString());

    modelRow    = table.convertRowIndexToModel(row);
    modelColumn = table.convertColumnIndexToModel(column);

    if(table instanceof DivisionTable) {
      Color rowColor = null;
      if(((DivisionTable)table).getCellColorController() != null)
        rowColor = ((DivisionTable)table).getCellColorController().getCellColor(table, modelRow, modelColumn, isSelected, false);
      if(rowColor != null)
        component.setBackground(rowColor);
      else {
        component.setBackground(((DivisionTable)table).getBackground());
        component.setForeground(((DivisionTable)table).getForeground());
      }
    }

    changeSize(table, row, column);
    return component;
  }

  /*private void changeSize(JTable table, JTextArea text, int row, int column) {
    FontMetrics fm = text.getFontMetrics(text.getFont());

    int w = table.getColumnModel().getColumn(column).getWidth()-text.getMargin().left-text.getMargin().right;
    int tw = fm.stringWidth(text.getText());
    int lineCount = tw/w+(tw/w<0?1:0)+text.getLineCount();
    int hw = fm.getHeight() * lineCount + fm.getDescent() + fm.getLeading();

    text.setSize(new Dimension(w, hw));
    table.setRowHeight(row, hw+2);
  }*/
  
  private void changeSize(JTable table, int row, int column) {
    int[] heights = new int[0];
    FontMetrics metrics = component.getFontMetrics(component.getFont());
    for(int i=0;i<table.getColumnCount();i++) {
      if(i == 1) {
        int columnWidth = table.getColumnModel().getColumn(i).getWidth()-table.getInsets().left-table.getInsets().right;
        String text = i==column?component.getText():(String)table.getValueAt(row, i);
        int heigthText = metrics.getHeight()+table.getInsets().top+table.getInsets().bottom;
        int rowCount = getRowCount(text, columnWidth, metrics);
        int rowHeight = rowCount*heigthText;
        heights = ArrayUtils.add(heights, rowHeight);
      }
    }
    Arrays.sort(heights);
    component.setSize(component.getWidth(), heights[heights.length-1]);
    table.setRowHeight(row, heights[heights.length-1]);
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

  public JTextArea getTextArea() {
    return component;
  }

  @Override
  public Object getCellEditorValue() {
    return component.getText();
  }

  @Override
  public boolean isCellEditable(EventObject e) {
    if(e instanceof MouseEvent)
      return ((MouseEvent)e).getClickCount() == 2;
    return super.isCellEditable(e);
  }

  public void repaint() {
    if(component != null) {
      Color rowColor = null;
      if(table instanceof DivisionTable)
        if(((DivisionTable)table).getCellColorController() != null)
          rowColor = ((DivisionTable)table).getCellColorController().getCellColor(table, modelRow, modelColumn, true, false);
      if(rowColor != null)
        component.setBackground(rowColor);
      else {
        component.setBackground(table.getSelectionBackground());
        component.setForeground(table.getSelectionForeground());
      }
    }
  }
}