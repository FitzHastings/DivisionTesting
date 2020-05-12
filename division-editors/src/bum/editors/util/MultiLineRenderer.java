package bum.editors.util;

import division.swing.DivisionTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.Arrays;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import org.apache.commons.lang3.ArrayUtils;

public class MultiLineRenderer implements TableCellRenderer {
  private JTextArea component;
  private Border nofocusborder = new EmptyBorder(1, 1, 1, 1);

  public MultiLineRenderer() {
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    component = new JTextArea();
    component.setOpaque(true);
    component.setLineWrap(true);
    component.setWrapStyleWord(true);
    component.setText((value == null) ? "" : value.toString());
    component.setSize(table.getColumnModel().getColumn(column).getWidth(), 1);
    component.setPreferredSize(new Dimension(table.getColumnModel().getColumn(column).getWidth(), 1));
    changeSize(table, row, column);

    int modelRow    = table.convertRowIndexToModel(row);
    int modelColumn = table.convertColumnIndexToModel(column);

    if(table instanceof DivisionTable) {
      Color rowColor = null;
      if(((DivisionTable)table).getCellColorController() != null)
        rowColor = ((DivisionTable)table).getCellColorController().getCellColor(table, modelRow, modelColumn, isSelected, hasFocus);
      if(rowColor != null)
        component.setBackground(rowColor);
      else {
        component.setBackground(((DivisionTable)table).getBackground());
        component.setForeground(((DivisionTable)table).getForeground());
      }
    }

    if(isSelected)
      component.setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
    else component.setBorder(nofocusborder);

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
      //if(i == 1) {
        int columnWidth = table.getColumnModel().getColumn(i).getWidth()-table.getInsets().left-table.getInsets().right;
        String text = i==column?component.getText():(String)table.getValueAt(row, i);
        int heigthText = metrics.getHeight()+table.getInsets().top+table.getInsets().bottom;
        int rowCount = getRowCount(text, columnWidth, metrics);
        int rowHeight = rowCount*heigthText;
        heights = ArrayUtils.add(heights, rowHeight);
      //}
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
}
