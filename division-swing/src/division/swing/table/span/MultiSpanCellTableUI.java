package division.swing.table.span;

import java.awt.*;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;

public class MultiSpanCellTableUI extends BasicTableUI {

  @Override
  public void paint(Graphics g, JComponent c) {
    Rectangle oldClipBounds = g.getClipBounds();
    Rectangle clipBounds    = new Rectangle(oldClipBounds);
    int tableWidth   = table.getColumnModel().getTotalColumnWidth();
    clipBounds.width = Math.min(clipBounds.width, tableWidth);
    g.setClip(clipBounds);
    
    int firstIndex = table.rowAtPoint(new Point(0, clipBounds.y));
    int lastIndex  = table.getRowCount()-1;
    
    Rectangle rowRect = new Rectangle(0,0,
            tableWidth, table.getRowHeight() + table.getRowMargin());
    rowRect.y = firstIndex*rowRect.height;
    
    for(int index = firstIndex; index <= lastIndex; index++) {
      if(rowRect.intersects(clipBounds)) {
        paintRow(g, index);
      }
      rowRect.y += rowRect.height;
    }
    g.setClip(oldClipBounds);
  }
  
  private void paintRow(Graphics g, int row) {
    Rectangle rect = g.getClipBounds();
    boolean drawn  = false;
    
    AttributiveCellTableModel tableModel = (AttributiveCellTableModel)table.getModel();
    CellSpan cellAtt = (CellSpan)tableModel.getCellAttribute();
    int numColumns = table.getColumnCount();
    
    for(int column = 0;column < numColumns;column++) {
      Rectangle cellRect = table.getCellRect(row,column,true);
      int cellRow,cellColumn;
      if(cellAtt.isVisible(row,column)) {
        cellRow    = row;
        cellColumn = column;
      } else {
        cellRow    = row + cellAtt.getSpan(row,column)[CellSpan.ROW];
        cellColumn = column + cellAtt.getSpan(row,column)[CellSpan.COLUMN];
      }
      if(cellRect.intersects(rect)) {
        drawn = true;
        paintCell(g, cellRect, cellRow, cellColumn);
      } else {
        if (drawn) break;
      }
    }
  }

  private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
    int spacingHeight = table.getRowMargin();
    int spacingWidth  = table.getColumnModel().getColumnMargin();
    
    AttributiveCellTableModel tableModel = (AttributiveCellTableModel)table.getModel();
    CellFont cellAtt = (CellFont)tableModel.getCellAttribute();
    CellAlign alignAtt = (CellAlign)tableModel.getCellAttribute();
    ColoredCell cellColor = (ColoredCell)tableModel.getCellAttribute();
    
    Color c = g.getColor();
    g.setColor(table.getGridColor());
    g.drawRect(cellRect.x-1,cellRect.y-1,cellRect.width,cellRect.height);
    g.setColor(c);
    
    cellRect.setBounds(cellRect.x + spacingWidth/2, cellRect.y + spacingHeight/2, cellRect.width, cellRect.height);
    
    if(table.isEditing() && table.getEditingRow()==row && table.getEditingColumn()==column) {
      Component component = table.getEditorComponent();
      component.setFont(cellAtt.getFont(row, column));
      
      if(alignAtt.getAlign(row, column) != -1 && component instanceof JLabel)
        ((JLabel)component).setHorizontalAlignment(alignAtt.getAlign(row, column));
      
      if(cellColor.getBackground(row, column) != null)
        component.setBackground(cellColor.getBackground(row, column));
      //else component.setBackground(table.getBackground());
      
      component.setBounds(cellRect);
      component.validate();
    }else {
      TableCellRenderer renderer = table.getCellRenderer(row, column);
      Component component = table.prepareRenderer(renderer, row, column);
      component.setFont(cellAtt.getFont(row, column));
      
      if(component instanceof JLabel) {
        ((JLabel)component).setHorizontalAlignment(SwingConstants.LEFT);
        if(alignAtt.getAlign(row, column) != -1)
          ((JLabel)component).setHorizontalAlignment(alignAtt.getAlign(row, column));
      }
      
      //System.out.println("cellColor.getBackground("+row+", "+column+") = "+cellColor.getBackground(row, column));
      
      if(cellColor.getBackground(row, column) != null)
        component.setBackground(cellColor.getBackground(row, column));
      //else component.setBackground(table.getBackground());
      
      if(component.getParent() == null) {
        rendererPane.add(component);
      }
      rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true);
    }
  }
}