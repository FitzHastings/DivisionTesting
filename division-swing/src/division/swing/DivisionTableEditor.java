package division.swing;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import net.sf.json.JSONObject;

public class DivisionTableEditor extends AbstractCellEditor implements TableCellEditor {
  protected JComponent component;
  protected JTable table;
  private int clickCountToStart = 2;
  private int gridBagConstraints = GridBagConstraints.CENTER;
  private Object editValue;
  private int modelRow;
  private int modelColumn;
  private Class editClass;

  private boolean isCellEdit = true;
  
  private final List<TableEditorAdapter> listeners = new ArrayList<>();


  public DivisionTableEditor(JTable table) {
    this(table, GridBagConstraints.CENTER);
  }
  
  public DivisionTableEditor(JTable table, int gridBagConstraint) {
    this.table = table;
    this.gridBagConstraints = gridBagConstraint;
  }

  public void addTableEditorListener(TableEditorAdapter listener) {
    listeners.add(listener);
    addCellEditorListener(listener);
  }

  public void removeTableEditorListener(TableEditorAdapter listener) {
    listeners.remove(listener);
    removeCellEditorListener(listener);
  }

  public void fireStartEdit(int row, int column, Object editValue) {
    for(TableEditorAdapter listener:listeners)
      listener.start(row,column,editValue);
  }

  @Override
  public Component getTableCellEditorComponent(
          final JTable table,
          Object value, 
          boolean isSelected, 
          final int row, 
          int column) {

    modelRow    = table.convertRowIndexToModel(row);
    modelColumn = table.convertColumnIndexToModel(column);

    table.grabFocus();
    component = new JLabel();
    final JPanel panel = new JPanel(new GridBagLayout());
    int resize = GridBagConstraints.BOTH;
    
    if(value instanceof Boolean) {
      value = new JCheckBox(null,null,(Boolean)value);
      ((JCheckBox)value).addItemListener((ItemEvent e) -> {
        stopCellEditing();
      });
    }
    
    if(value instanceof JComponent) {
      if(value instanceof JCheckBox || value instanceof JRadioButton)
        resize = GridBagConstraints.NONE;
      component = (JComponent)value;
      component.setFocusable(false);
    }
    if(value instanceof JLabel)
      value = ((JLabel)value).getText();

    if(
            value instanceof String || 
            value == null || 
            value instanceof Double ||
            value instanceof Integer ||
            value instanceof Float ||
            value instanceof BigDecimal) {
      
      if(value instanceof String && value.toString().startsWith("json"))
        value = JSONObject.fromObject(value.toString().substring(4)).getString("title");

      editClass = table.getModel().getColumnClass(modelColumn);

      DivisionTextField.Type type = DivisionTextField.Type.ALL;
      if(editClass == Float.class || editClass == BigDecimal.class || editClass == Double.class)
        type = DivisionTextField.Type.FLOAT;
      if(editClass == Integer.class)
        type = DivisionTextField.Type.INTEGER;
      
      
      DivisionTextField text = new DivisionTextField(type);
      text.addActionListener((ActionEvent e) -> {
        stopCellEditing();
        //bum_TableEditor.this.table.setRowSelectionInterval(row,row);
      });
      
      text.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          stopCellEditing();
          //bum_TableEditor.this.table.setRowSelectionInterval(row,row);
        }
      });
      component = (JComponent)new DefaultCellEditor(text).getTableCellEditorComponent(table,value,isSelected,row,column);
      if(value != null) {
        text.setSelectionStart(0);
        text.setSelectionEnd(value.toString().length());
      }
    }
    
    if(value instanceof Date) {
      component = new DivisionCalendarComboBox();
      ((DivisionCalendarComboBox)component).setDateInCalendar((Date)value);
    }
    
    if(value instanceof java.sql.Date) {
      component = new DivisionCalendarComboBox();
      ((DivisionCalendarComboBox)component).setDateInCalendar((java.sql.Date)value);
    }
    
    if(value instanceof java.sql.Timestamp) {
      component = new DivisionCalendarComboBox();
      ((DivisionCalendarComboBox)component).setDateInCalendar((java.sql.Timestamp)value);
    }
    
    if(component instanceof DivisionCalendarComboBox) {
      component.setFocusable(false);
      ((DivisionCalendarComboBox)component).addCalendarChangeListener(new CalendarChangeListener() {
        @Override
        public void CalendarChangeDate(DistanceList dates) {
        }

        @Override
        public void CalendarSelectedDate(DistanceList dates) {
          stopCellEditing();
        }
      });
    }else panel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

    component.setBorder(BorderFactory.createEmptyBorder());
    component.setFont(table.getFont());

    panel.add(component, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,gridBagConstraints, resize, new Insets(0, 0, 0, 0), 0, 0));

    Color rowColor     = null;

    if(rowColor != null)
      rowColor = new Color(rowColor.getRed(), rowColor.getGreen(), rowColor.getBlue(), 100);

    setComponentColor(panel,rowColor);
    return panel;
  }

  @Override
  public Object getCellEditorValue() {
    if(component instanceof JTextComponent) {
      Object value = ((JTextComponent)component).getText();
      if(component instanceof DivisionTextField) {
        if(value == null)
          value = "";
        switch(((DivisionTextField)component).getType()) {
          case INTEGER:
            value = "".equals(value)?0:Integer.valueOf(value.toString());
            break;
          case FLOAT:
            if(editClass == BigDecimal.class)
              value = "".equals(value)?new BigDecimal(0.0):new BigDecimal(value.toString());
            else
              value = "".equals(value)?0.0:Double.valueOf(value.toString());
            break;
        }
      }
      return value;
    }
    if(component instanceof DivisionCalendarComboBox)
      return ((DivisionCalendarComboBox)component).getDate();
    if(component instanceof JCheckBox)
      return ((JCheckBox)component).isSelected();
    if(component instanceof JComboBox)
      return ((JComboBox)component).getSelectedItem();
    return component;
  }
  
  private void setComponentColor(JComponent com, Color c) {
    com.setBackground(c);
    //if(com instanceof JPanel)
    for(Component component:com.getComponents())
      if(component instanceof JComponent)
        setComponentColor((JComponent)component,c);
  }

  public int getClickCountToStart() {
    return clickCountToStart;
  }

  public void setClickCountToStart(int clickCountToStart) {
    this.clickCountToStart = clickCountToStart;
  }

  public Object getEditValue() {
    return editValue;
  }

  @Override
  public void cancelCellEditing() {
    isCellEdit = false;
    super.cancelCellEditing();
    isCellEdit = true;
  }

  @Override
  public boolean stopCellEditing() {
    isCellEdit = false;
    boolean is = super.stopCellEditing();
    isCellEdit = true;
    return true;
  }
  
  @Override
  public boolean isCellEditable(EventObject anEvent) {
    boolean cellEdit = true;
    if(anEvent instanceof MouseEvent) {
      int row = table.rowAtPoint(((MouseEvent)anEvent).getPoint());
      int column = table.columnAtPoint(((MouseEvent)anEvent).getPoint());
      Object value = table.getValueAt(row,column);
      if(value instanceof JComponent || value instanceof Boolean) {
        editValue = value;
        fireStartEdit(row,column,editValue);
        cellEdit = isCellEdit;
      }else {
        cellEdit = ((MouseEvent) anEvent).getClickCount() >= clickCountToStart;
        if(cellEdit) {
          editValue = value;
          fireStartEdit(row,column,editValue);
          cellEdit = isCellEdit;
        }
      }
    }else if(anEvent instanceof KeyEvent) {
      if(((KeyEvent)anEvent).getKeyChar() == KeyEvent.VK_SPACE) {
        final Object value = table.getValueAt(table.getSelectedRow(),table.getSelectedColumn());
        if(value instanceof JComponent) {
          ((JComponent)value).grabFocus();
        }
        editValue = value;
        fireStartEdit(table.getSelectedRow(),table.getSelectedColumn(),editValue);
        cellEdit = isCellEdit;
      }else cellEdit = false;
    }else cellEdit = false;
    isCellEdit = true;
    return cellEdit;
  }
}