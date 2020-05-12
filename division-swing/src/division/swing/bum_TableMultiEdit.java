package division.swing;

import division.swing.TableMultiEditListener;
import division.swing.DivisionTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

public class bum_TableMultiEdit extends JPanel
{
  public DivisionTable fixedTable, table;
  Object[] columns;
  Object[] editors;
  ArrayList<TableMultiEditListener> listeners = new ArrayList<TableMultiEditListener>();
  
  public bum_TableMultiEdit(Object[] columns, Object[] editors)
  {
    this.columns = columns;
    this.editors = editors;
    init();
    initEditingRow();
    initEvents();
  }

  public DivisionTable getTable()
  {
    return table;
  }
  
  
  
  private void init()
  {
    fixedTable = new DivisionTable(); // фиксированая таблица ввода данных
    fixedTable.setSelectionBackground(Color.WHITE);
    
    fixedTable.setFocusTraversalKeysEnabled(true);
    fixedTable.setColumns(columns); // устанавливаем колонки
    
    TableColumnModel cm = fixedTable.getColumnModel();
    cm.removeColumn(cm.getColumn(0));
   
    table = new DivisionTable();
    table.setColumns(columns);
    table.setColumnWidthZero(new int[]{0});
    
    fixedTable.setRowHeight(22);
    fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    JScrollPane fixedScroll = new JScrollPane( fixedTable );
    fixedScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    fixedScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    fixedScroll.setPreferredSize(new Dimension(500,45));
    fixedScroll.setSize(new Dimension(500, 45));
    fixedScroll.setMinimumSize(new Dimension(500, 45));
    fixedScroll.setMaximumSize(new Dimension(500, 45));
    
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    
    JScrollPane scroll = new JScrollPane( table ) {
      @Override
      public void setColumnHeaderView(Component view) {} // work around
    };
    
    table.setBorder(BorderFactory.createEmptyBorder());
    
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scroll.setPreferredSize(new Dimension(900, 400));
    
    JScrollBar bar = fixedScroll.getVerticalScrollBar();
    final JScrollBar dummyBar = new JScrollBar() {
      @Override
      public void paint(Graphics g) {}
    };
    dummyBar.removeAll();
    dummyBar.setPreferredSize(bar.getPreferredSize());
    
    fixedScroll.setVerticalScrollBar(dummyBar);
    this.setLayout(new GridBagLayout());
    this.add(fixedScroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    this.add(scroll, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }
  
  public void initEditingRow() 
  {
    fixedTable.getTableModel().insertRow(0, editors);
    for (int i=0;i<editors.length-1;i++)
    {
      fixedTable.setColumnEditable(i, true);
      fixedTable.findTableColumn(i+1).setCellRenderer(new fixed_TableRenderer());
      if (editors[i+1] instanceof JComboBox)
        ((JComboBox)editors[i+1]).addFocusListener(new FocusListener() {
          public void focusGained(FocusEvent e){}
          public void focusLost(FocusEvent e){fixedTable.repaint();}
        });
        //fixedTable.findTableColumn(i+1).setCellEditor(new DefaultCellEditor((JComboBox)editors[i+1]));
      //else
      fixedTable.findTableColumn(i+1).setCellEditor(new fixed_TableEditor(editors[i+1]));
    }
  }
  
  private void initEvents()
  {
    /*table.getTableModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e)
      {
        System.out.println("change model!="+table.getTableModel().getRowCount());
        System.out.println("row="+e.getFirstRow());
        table.setRowSelectionInterval(0, 0);
      }
    });*/
    fixedTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() 
    {
      @Override
      public void columnAdded(TableColumnModelEvent e) {}
      @Override
      public void columnRemoved(TableColumnModelEvent e) {}
      @Override
      public void columnMoved(TableColumnModelEvent e) 
      {
        table.getColumnModel().moveColumn(e.getFromIndex()+1, e.getToIndex()+1);
      }
      @Override
      public void columnMarginChanged(ChangeEvent e){setWidths();}
      @Override
      public void columnSelectionChanged(ListSelectionEvent e) {}
    });
    
    fixedTable.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e){}
      @Override
      public void keyPressed(KeyEvent e){}
      @Override
      public void keyReleased(KeyEvent e)
      {
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
          table.grabFocus();
          fixedTable.getEditor().cancelCellEditing();
          fireCreate();
        }
      }
    });
    
    table.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e){}
      @Override
      public void keyPressed(KeyEvent e)
      {
        if (e.getKeyCode() == KeyEvent.VK_TAB)
        {
          fixedTable.grabFocus();
        }
      }
      @Override
      public void keyReleased(KeyEvent e)
      {
        if (e.getKeyCode() == KeyEvent.VK_DELETE)fireRemove();
      }
    });
    
    
  }
  
  private void setWidths()
  {
    for (int i=0; i<fixedTable.getColumnModel().getColumnCount();i++)
      table.getColumnModel().getColumn(i+1).setPreferredWidth(
              fixedTable.getColumnModel().getColumn(i).getPreferredWidth()
              );
  }
  
  public DivisionTableModel getTableModel(){return table.getTableModel();}
  
  private void fireCreate() {
    for(TableMultiEditListener listener:listeners)
      listener.create();
  }
  
  private void fireRemove() {
    for(TableMultiEditListener listener:listeners)
      listener.remove();
  }
  
  public void addEditingListener(TableMultiEditListener editingListener) {
    listeners.add(editingListener);
  }
  
  class fixed_TableRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, 
            boolean hasFocus, int row, int column) {
      Component comp=null;
      
      Color bkground = hasFocus?new Color(250, 250, 220):Color.WHITE;
      //if (!hasFocus && isSelected)
        //bkground = Color.WHITE;
      
      if(value instanceof JComboBox)
      {
        ((JComboBox)value).setBackground(bkground);
        ((JComboBox)value).setBorder(BorderFactory.createEmptyBorder());
        comp=(Component) value;
      }
      else if(value instanceof JCheckBox)
      {
        ((JCheckBox)value).setBackground(bkground);
        
        ((JCheckBox)value).setBorder(BorderFactory.createEmptyBorder());
        comp=(Component) value;
      }else if(value instanceof JTextField)
      {
        ((JTextField)value).setBackground(bkground);
        ((JTextField)value).setBorder(BorderFactory.createEmptyBorder());
        comp=(Component) value;
      }
      return comp;
    }
  }
  
  public class fixed_TableEditor extends AbstractCellEditor implements TableCellEditor
  {
    Object value = null;
    
    public fixed_TableEditor(Object value){this.value = value;}
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
      return (Component)value;
    }

    public Object getCellEditorValue()
    {
      return value;
    }
    
    public boolean shouldSelectCell(EventObject anEvent)
    {
            return false;
    }
  }
}