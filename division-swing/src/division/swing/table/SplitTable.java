package division.swing.table;

import division.swing.*;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import org.apache.commons.lang3.ArrayUtils;

public class SplitTable extends JPanel {
  private int type = JSplitPane.HORIZONTAL_SPLIT;
  private StatusBar statusBar = new StatusBar();
  private StatusBar statusBar2 = new StatusBar();
  private DivisionTable[] tables;
  private boolean[] tablesactive;
  private DivisionScrollPane[] scrolls;
  private DivisionSplitPane[] splits;
  
  public SplitTable(int tablesCount) {
    this(tablesCount, -1);
  }

  public SplitTable(int tablesCount, int type) {
    if(type != JSplitPane.HORIZONTAL_SPLIT && type != JSplitPane.VERTICAL_SPLIT)
      type = JSplitPane.HORIZONTAL_SPLIT;
    this.type = type;
    
    tables = new DivisionTable[tablesCount];
    tablesactive = new boolean[tablesCount];
    scrolls = new DivisionScrollPane[tablesCount];
    splits = new DivisionSplitPane[tablesCount-1];

    for(int i=0;i<tablesCount;i++) {
      tables[i] = new DivisionTable();
      scrolls[i] = new DivisionScrollPane(tables[i]);
      checkTable(i);
      
      if(i < tablesCount-1) {
        splits[i] = new DivisionSplitPane(type);
        splits[i].setDividerSize(3);
        splits[i].setBorder(BorderFactory.createEmptyBorder());
      }
    }

    for(int i=0;i<splits.length;i++) {
      splits[i].add(scrolls[i], JSplitPane.LEFT);
      if(i == splits.length-1)
        splits[i].add(scrolls[i+1], JSplitPane.RIGHT);
      else splits[i].add(splits[i+1], JSplitPane.RIGHT);
    }

    setLayout(new GridBagLayout());
    add(splits[0],  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    add(statusBar,  new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(statusBar2, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  public StatusBar getStatusBar2() {
    return statusBar2;
  }

  public StatusBar getStatusBar() {
    return statusBar;
  }
  
  private void checkTable(final int index) {
    tables[index].setSortable(false);
    tablesactive[index] = true;
    
    if(type == JSplitPane.HORIZONTAL_SPLIT) {
      tables[index].addTableSelectionListener(new TableSelectionListener() {
        @Override
        public void TableSelectionChanged(int[] oldSelection, int[] newSelection) {
          if(tablesactive[index])
            selectRows(index);
        }
      });
    }
    
    if(type == JSplitPane.HORIZONTAL_SPLIT) {
      scrolls[index].getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
          scroll(index, e);
        }
      });
    }else {
      if(index != 0)
        tables[index].setTableHeader(null);
      else {
        tables[index].getColumnModel().addColumnModelListener(new TableColumnModelListener() {
          @Override
          public void columnAdded(TableColumnModelEvent e) {
          }

          @Override
          public void columnRemoved(TableColumnModelEvent e) {
          }

          @Override
          public void columnMoved(TableColumnModelEvent e) {
          }

          @Override
          public void columnMarginChanged(ChangeEvent e) {
            for(int i=1;i<tables.length;i++)
              for(int j=0;j<tables[i].getColumnCount();j++)
                tables[i].getColumnModel().getColumn(j).setPreferredWidth(tables[0].getColumnModel().getColumn(j).getPreferredWidth());
          }

          @Override
          public void columnSelectionChanged(ListSelectionEvent e) {
          }
        });
      }
      
      scrolls[index].getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
          scroll(index, e);
        }
      });
    }
    
    if(index != tables.length-1) {
      if(type == JSplitPane.HORIZONTAL_SPLIT)
        scrolls[index].getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
      else scrolls[index].getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
    }
  }
  
  public void setTable(final int index, DivisionTable table) {
    tables[index] = table;
    scrolls[index].setViewportView(tables[index]);
    checkTable(index);
  }
  
  public void setSortable(boolean is) {
    for(DivisionTable t:tables)
      t.setSortable(is);
  }

  private void scroll(int index, AdjustmentEvent e) {
    tablesactive[index] = false;
    for(int i=0;i<scrolls.length;i++) {
      if(i != index) {
        if(type == JSplitPane.HORIZONTAL_SPLIT)
          scrolls[i].getVerticalScrollBar().setValue(e.getValue());
        else scrolls[i].getHorizontalScrollBar().setValue(e.getValue());
      }
    }
    tablesactive[index] = true;
  }
  
  public void clear() {
    for(DivisionTable t:tables)
      t.clear();
  }
  
  public DivisionScrollPane getScroll(int index) {
    return scrolls[index];
  }

  public void selectRows(final int index) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        int[] rows = tables[index].getSelectedRows();
        if(rows.length > 0) {
          tablesactive[index] = false;
          int[][] intervals = getIntervals(rows);
          for(int i=0;i<tables.length;i++) {
            if(i != index) {
              tablesactive[i] = false;
              tables[i].clearSelection();
              for(int[] interval:intervals)
                tables[i].addRowSelectionInterval(interval[0], interval[1]);
              tablesactive[i] = true;
            }
          }
          //tables[index].grabFocus();
          tablesactive[index] = true;
        }
      }
    });
  }
  
  public static int[][] getIntervals(int[] rows) {
    int[][] intervals = new int[][]{};
    if(rows.length == 1)
      intervals = (int[][]) ArrayUtils.add(intervals, new int[]{rows[0],rows[0]});
    else {
      Arrays.sort(rows);
      int row = rows[0];
      for(int i=1;i<rows.length;i++) {
        if(rows[i]-rows[i-1] > 1) {
          intervals = (int[][]) ArrayUtils.add(intervals, new int[]{row,rows[i-1]});
          row = rows[i];
        }
      }
      intervals = (int[][]) ArrayUtils.add(intervals, new int[]{row,rows[rows.length-1]});
    }
    return intervals;
  }
  
  public void setColumns(Object[] columnNames) {
    setColumns(columnNames, -1);
  }

  public void setColumns(Object[] columnNames, int tableIndex) {
    if(type == JSplitPane.VERTICAL_SPLIT || tableIndex == -1) {
      for(int i=0;i<tables.length;i++)
        tables[i].setColumns(columnNames);
    }else tables[tableIndex].setColumns(columnNames);
  }

  public synchronized void addRow(Object[] row) {
    if(type == JSplitPane.HORIZONTAL_SPLIT) {
      int startIndex = 0;
      for(int i=0;i<tables.length;i++) {
        if(row.length-startIndex >= tables[i].getColumnCount())
          tables[i].getTableModel().addRow(Arrays.copyOfRange(row, startIndex, startIndex+tables[i].getColumnCount()));
        else {
          Object[] r = new Object[tables[i].getColumnCount()];
          for(int j=startIndex;j<row.length;j++)
            r[j-startIndex] = row[j];
          tables[i].getTableModel().addRow(r);
        }
        startIndex += tables[i].getColumnCount();
      }
    }
  }

  public DivisionTable getTable(int index) {
    return tables[index];
  }

  public DivisionSplitPane[] getSplits() {
    return splits;
  }

  public DivisionTable[] getTables() {
    return tables;
  }
  
  public void setActive(int index, boolean active) {
    tablesactive[index] = active;
  }
}