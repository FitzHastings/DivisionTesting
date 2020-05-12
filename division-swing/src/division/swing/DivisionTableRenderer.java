package division.swing;

import division.swing.table.CellAligmentController;
import division.swing.table.CellColorController;
import division.swing.table.CellFontController;
import division.util.Utility;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Period;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import net.sf.json.JSONObject;

public class DivisionTableRenderer extends DefaultTableCellRenderer {
  private CellColorController    cellColorController;
  private CellFontController     cellFontController;
  private CellAligmentController cellAligmentController;
  private int gridBagConstraints;
  private final Border nofocusborder = new EmptyBorder(1, 1, 1, 1);
  private final NumberFormat df = NumberFormat.getInstance();
  private boolean selectCell = true;
  private JTable table;

  public DivisionTableRenderer() {
    this(null, GridBagConstraints.CENTER);
  }

  public DivisionTableRenderer(CellColorController cellColorController) {
    this(cellColorController, GridBagConstraints.CENTER);
  }

  public DivisionTableRenderer(CellColorController cellColorController, int gridBagConstraint) {
    this.cellColorController = cellColorController;
    this.gridBagConstraints = gridBagConstraint;
    
    df.setMaximumFractionDigits(2);
    df.setMinimumFractionDigits(2);
  }

  public CellAligmentController getCellAligmentController() {
    return cellAligmentController;
  }

  public void setCellAligmentController(CellAligmentController cellAligmentController) {
    this.cellAligmentController = cellAligmentController;
  }

  public CellFontController getCellFontController() {
    return cellFontController;
  }

  public void setCellFontController(CellFontController cellFontController) {
    this.cellFontController = cellFontController;
  }

  public CellColorController getCellColorController() {
    return cellColorController;
  }

  public void setCellColorController(CellColorController cellColorController) {
    this.cellColorController = cellColorController;
  }

  public boolean isSelectCell() {
    return selectCell;
  }

  public void setSelectCell(boolean selectCell) {
    this.selectCell = selectCell;
  }

  private void setComponentColor(JComponent com, Color c) {
    com.setBackground(c);
    for(Component component:com.getComponents())
      if(component instanceof JComponent)
        setComponentColor((JComponent)component,c);
      else component.setBackground(c);
  }

  private void setComponentFont(JComponent com, Font f) {
    com.setFont(f);
    for(Component component:com.getComponents())
      setComponentFont((JComponent)component,f);
  }

  @Override
  public Component getTableCellRendererComponent(
          JTable t, 
          Object value, 
          boolean isSelected, 
          boolean hasFocus, 
          final int row,
          int column) {
    
    //System.out.println(value!=null?value.getClass().getSimpleName()+" "+value:null);
    
    if(table == null)
      table = t;

    int modelRow = t.convertRowIndexToModel(row);
    int modelColumn = t.convertColumnIndexToModel(column);
    JComponent component;
    
    if(value instanceof Boolean)
      value = new JCheckBox("",(Boolean)value);

    if(value instanceof ImageIcon)
      value = new JLabel((ImageIcon)value);

    if(value instanceof JComponent && !(value instanceof JComboBox)) {
      JPanel panel = new JPanel(new GridBagLayout());
      int resize = GridBagConstraints.BOTH;
      if(value instanceof JCheckBox || value instanceof JRadioButton)
        resize = GridBagConstraints.NONE;
      component = (JComponent)value;
      component.setFocusable(false);
      
      panel.setBorder(BorderFactory.createEmptyBorder());
      component.setBorder(BorderFactory.createEmptyBorder());
      component.setFont(table.getFont());
      panel.add(component, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,gridBagConstraints, resize, new Insets(0, 0, 0, 0), 0, 0));

      setColor(table, panel, isSelected, hasFocus, modelRow, modelColumn);
      return panel;
    }
    
    if(value instanceof String && value.toString().startsWith("json"))
      value = JSONObject.fromObject(value.toString().substring(4)).getString("title");

    if(value instanceof Timestamp)
      value = Utility.format((Timestamp)value);
    
    if(value instanceof Date)
      value = Utility.format((Date)value);
    
    if(value instanceof java.sql.Date)
      value = Utility.format((java.sql.Date)value);
    
    if(value instanceof JComboBox)
      value = ((JComboBox)value).getSelectedItem();
    
    if(value instanceof Double)
      value = Float.valueOf(value.toString());
    
    if(value instanceof BigDecimal)
      value = df.format((BigDecimal) value);
    
    if(value instanceof Period)
      value = Utility.format((Period)value);
    
    component = (JComponent)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
    
    
    if(cellAligmentController != null) {
      Integer aligment = cellAligmentController.getCellAligment(table, modelRow, modelColumn, isSelected, hasFocus);
      if(aligment != null)
        ((JLabel)component).setHorizontalAlignment(aligment);
    }
    
    setColor(table, component, isSelected, hasFocus, modelRow, modelColumn);
    return component;
  }

  private void setColor(JTable table, JComponent component, boolean isSelected, boolean hasFocus, int modelRow, int modelColumn) {
    Color color     = null;
    Color textColor = null;
    Font  font      = null;

    if(cellColorController != null) {
      color     = cellColorController.getCellColor(table, modelRow, modelColumn, isSelected, hasFocus);
      textColor = cellColorController.getCellTextColor(table, modelRow, modelColumn, isSelected, hasFocus);
    }
    
    if(cellFontController != null)
      font = cellFontController.getCellFont(table, modelRow, modelColumn, isSelected, hasFocus);
    
    if(font == null)
      font = table.getFont();
    
    if(color == null) {
      if(isSelected && !hasFocus)
        color = table.getSelectionBackground();
      else color = table.getBackground();
    }
    
    if(textColor == null) {
      if(isSelected && !hasFocus)
        textColor = table.getSelectionForeground();
      else textColor = table.getForeground();
    }
    
    component.setFont(font);
    component.setForeground(textColor);
    setComponentColor(component, color);
    
    if(hasFocus) {
      if(color == null)
        component.setBackground(table.getBackground());
      component.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
    }else {
      component.setBorder(nofocusborder);
    }
  }
}