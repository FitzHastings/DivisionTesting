package division.editors.tables;

import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.ProductFactorRule;
import division.swing.guimessanger.Messanger;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;

public class ProductFactorRuleTableEditor extends TableEditor {
  private Integer[] products = new Integer[0];
  private Integer factorId;
  
  public ProductFactorRuleTableEditor() {
    super(
            new String[]{"id","Условие","Параметр","Оператор","Значение(Аргумент)","тип","",""},
            new String[]{"id","condition_","parameter","operator","meaning","factor_factorType","factor_listValues","factor"},
            ProductFactorRule.class,
            null,
            "значения реквизитов продукта");
    getTable().setColumnEditable(1, true);
    getTable().setColumnEditable(2, true);
    getTable().setColumnEditable(3, true);
    getTable().setColumnEditable(4, true);
    setAddFunction(true);

    getTable().setColumnWidthZero(new int[]{0,5,6,7});
    
    getTable().getTableModel().addTableModelListener((TableModelEvent e) -> {
      RemoteSession session = null;
      try {
        if(e.getType() == TableModelEvent.UPDATE && e.getLastRow() != -1 && e.getColumn() != -1) {
          session = ObjectLoader.createSession();
          ProductFactorRule pfr;
          int column   = getTable().convertColumnIndexToModel(e.getColumn());
          int row      = getTable().convertRowIndexToModel(e.getLastRow());
          Object value = getTable().getModel().getValueAt(row, column);
          if(column == 2 && !(value instanceof JComboBox)) {
            for(MappingObject o:getSelectedObjects()) {
              pfr = (ProductFactorRule)o;
              pfr.setParameter(value.toString());
              session.saveObject(pfr);
            }
          }
          if(column == 4 && !"".equals(value)) {
            for(MappingObject o:getSelectedObjects()) {
              pfr = (ProductFactorRule)o;
              pfr.setMeaning(Double.valueOf(value.toString()));
              session.saveObject(pfr);
            }
          }
          session.commit();
        }
      } catch (Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    });

    setAddAction((ActionEvent e) -> {
      RemoteSession session = null;
      try {
        session = ObjectLoader.createSession();
        MappingObject object = (MappingObject) session.createEmptyObject(ProductFactorRule.class);
        if(getRootFilter() != null)
          session.toEstablishes(getRootFilter(), object);
        session.saveObject(object);
        session.commit();
      } catch (Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    });
  }

  public Integer getFactor() {
    return factorId;
  }

  public void setFactor(Integer factorId) {
    this.factorId = factorId;
  }
  
  public Integer[] getProducts() {
    return products;
  }

  public void setProducts(Integer[] products) {
    this.products = products;
  }

  private Vector findRow(Vector newRow) {
    Vector<Vector> data = getTable().getTableModel().getDataVector();
    for(Vector row:data) {
      Object condition = ((JComboBox)row.get(1)).getSelectedIndex()==0?null:((JComboBox)row.get(1)).getSelectedItem();
      Object parameter = ((JComboBox)row.get(2)).getSelectedIndex()==0?null:((JComboBox)row.get(2)).getSelectedItem();
      Object operator  = ((JComboBox)row.get(3)).getSelectedIndex()==0?null:((JComboBox)row.get(3)).getSelectedItem();
      if((condition == newRow.get(1) || condition != null && condition.equals(newRow.get(1))) && 
              (parameter == newRow.get(2) || parameter != null && parameter.equals(newRow.get(2))) && 
              (operator == newRow.get(3) || operator != null && operator.equals(newRow.get(3))) && 
              row.subList(4, row.size()).equals(newRow.subList(4, newRow.size())))
        return row;
    }
    return null;
  }

  @Override
  protected void insertData(List<List> data, int startIndex) {
    for(List row:data) {
      Vector r = findRow(new Vector(row));
      if(r != null) {
        r.set(0, ArrayUtils.add((Integer[])r.get(0), row.get(0)));
      }else {
        row.set(0, new Integer[]{(Integer)row.get(0)});
        row.set(3, createComboBox(3, row.get(3), new String[]{"+","-","*"}));
        if(row.get(6) != null && !row.get(6).equals("")) {
          row.set(2, createComboBox(2, row.get(2), ((String)row.get(6)).split(";")));
          row.set(1, createComboBox(1, row.get(1), new String[]{"=","!="}));
        }else row.set(1, createComboBox(1, row.get(1), new String[]{"=",">","<",">=","<="}));
        getTable().getTableModel().getDataVector().insertElementAt(row, startIndex++);
      }
    }
    getTable().getTableModel().fireTableDataChanged();
  }
  
  private JComboBox createComboBox(final int columnIndex, Object value, Object[] values) {
    values = ArrayUtils.add(values, 0, "-----");
    final JComboBox combo = new JComboBox(values);
    if(value != null)
      combo.setSelectedItem(value);
    combo.addItemListener((ItemEvent e) -> {
      TableCellEditor editor = getTable().findTableColumn(columnIndex).getCellEditor();
      if(editor != null)
        editor.stopCellEditing();
      if(e.getStateChange() == ItemEvent.SELECTED) {
        RemoteSession session = null;
        try {
          session = ObjectLoader.createSession();
          String val = combo.getSelectedIndex()==0?null:e.getItem().toString();
          for(MappingObject o:getSelectedObjects()) {
            ProductFactorRule object = (ProductFactorRule)o;
            switch(columnIndex) {
              case 1:
                object.setCondition(val);
                break;
              case 2:
                object.setParameter(val);
                break;
              case 3:
                object.setOperator(val);
                break;
            }
            session.saveObject(object);
          }
          session.commit();
        }catch(Exception ex) {
          ObjectLoader.rollBackSession(session);
          Messanger.showErrorMessage(ex);
        }
      }
    });
    return combo;
  }
  
  @Override
  public String getEmptyObjectTitle() {
    return "[реквизиты продукта]";
  }
}