package division.editors.products;

import bum.editors.EditorGui;
import bum.editors.util.DivisionTarget;
import bum.editors.util.IDColorController;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Factor;
import bum.interfaces.Product;
import bum.interfaces.ProductFactorRule;
import division.fx.PropertyMap;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionToolButton;
import division.util.FileLoader;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import org.apache.commons.lang.ArrayUtils;
import util.filter.local.DBFilter;

public class ProductCalculator extends EditorGui {
  private DivisionTarget pfrDivisionTarget;
  
  private final JToolBar tool = new JToolBar();
  private final DivisionToolButton editToolButton    = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");

  private final DivisionTable      table     = new DivisionTable(new IDColorController());
  private final DivisionScrollPane scroll    = new DivisionScrollPane(table);

  private final JLabel costLabel = new JLabel("ИТОГО:");
  
  private Integer[] products;
  private BigDecimal cost;
  private BigDecimal nds;
  
  public ProductCalculator() {
    super(null, null);
    initComponents();
    initEvents();
  }

  private void initComponents() {
    getToolBar().add(editToolButton);
    table.setColumns("id","Реквизит", "Значение","ед. измерения","Скидка/Надбавка");
    table.setColumnWidthZero(0);
    table.setColumnEditable(2, true);
    table.setRowHeight(25);
    
    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(scroll, BorderLayout.CENTER);
    getRootPanel().add(costLabel, BorderLayout.SOUTH);
  }
  
  @Override
  public ArrayList getComponentsToStore() {
    table.setName("table");
    return new ArrayList(Arrays.asList(new Object[]{table}));
  }

  private void initEvents() {
    try {
      pfrDivisionTarget = new DivisionTarget(ProductFactorRule.class) {
        @Override
        public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
          initData();
        }
      };
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }

    editToolButton.addActionListener((ActionEvent e) -> {
      CostPolicy costPolicy = new CostPolicy();
      costPolicy.setAutoLoad(true);
      costPolicy.setAutoStore(true);
      costPolicy.setProducts(products);
      costPolicy.initData();
      costPolicy.createDialog(true).setVisible(true);
    });
  }


  public Integer[] getProducts() {
    return products;
  }

  public void setProducts(Integer[] products) {
    this.products = products;
  }
  
  private Integer[] saveFormula() {
    Integer[] formula = new Integer[0];
    if(products != null && products.length == 1) {
      try {
        List<List> formulaData = ObjectLoader.getData(DBFilter.create(Factor.class).AND_IN("products", products), new String[]{"id"}, true);
        if(formulaData.isEmpty()) {
          formula = new Integer[0];
          for(List id:formulaData)
            formula = (Integer[]) ArrayUtils.add(formula, id.get(0));
          ObjectLoader.executeUpdate("UPDATE [Product] SET [Product(formula)]=? WHERE id=ANY(?)", new Object[]{formula, products}, true);
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    return formula;
  }
  
  private Integer[] getFormula() {
    Integer[] formula = new Integer[0];
    if(products != null && products.length == 1) {
      try {
        List<List> formulaData = ObjectLoader.getData(Product.class, products, new String[]{"formula"});
        if(formulaData.isEmpty() || formulaData.get(0).get(0) == null || ((Integer[])formulaData.get(0).get(0)).length == 0)
          formula = saveFormula();
        else formula = (Integer[]) formulaData.get(0).get(0);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    return formula;
  }

  private void calculateCost() {
    BigDecimal c = cost;
    Integer[] formula = getFormula();
    if(formula != null && formula.length > 0) {
      for(int i=0;i<table.getRowCount();i++) {
        if(!ArrayUtils.contains(formula, table.getValueAt(i, 0))) {
          Object d = table.getValueAt(i, 4);
          if(d != null) {
            if(d.toString().split(" ")[0].equals("*"))
              c = c.multiply(BigDecimal.valueOf(Double.valueOf(d.toString().split(" ")[1])));
            else c = c.add(BigDecimal.valueOf(Double.valueOf(d.toString().replaceFirst(" ", ""))));
          }
        }
      }
      
      for(Integer id:formula) {
        for(int i=0;i<table.getRowCount();i++) {
          if(table.getValueAt(i, 0).equals(id)) {
            Object d = table.getValueAt(i, 4);
            if(d != null) {
              if(d.toString().split(" ")[0].equals("*"))
                c = c.multiply(BigDecimal.valueOf(Double.valueOf(d.toString().split(" ")[1])));
              else c = c.add(BigDecimal.valueOf(Double.valueOf(d.toString().replaceFirst(" ", ""))));
            }
          }
        }
      }
    }else {
      for(int i=0;i<table.getRowCount();i++) {
        Object d = table.getValueAt(i, 4);
        if(d != null) {
          if(d.toString().split(" ")[0].equals("*"))
            c = c.multiply(BigDecimal.valueOf(Double.valueOf(d.toString().split(" ")[1])));
          else c = c.add(BigDecimal.valueOf(Double.valueOf(d.toString().replaceFirst(" ", ""))));
        }
      }
    }
    costLabel.setText("ИТОГО: "+c+" руб. в т.ч. НДС ("+nds+"%)");
  }

  @Override
  public void initData() {
    ((IDColorController)table.getCellColorController()).clearRowColors();
    table.getTableModel().getDataVector().clear();
    try {
      if(isActive() && products != null && products.length == 1) {
        cost = new BigDecimal(0.0);
        nds = new BigDecimal(0.0);
        List<List> data = ObjectLoader.getData(Product.class, products, new String[]{"cost","nds"});
        if(!data.isEmpty()) {
          cost = (BigDecimal)data.get(0).get(0);
          nds = (BigDecimal)data.get(0).get(1);
        }
        calculateCost();
        
        data = ObjectLoader.getData(DBFilter.create(ProductFactorRule.class).AND_IN("product", products),
                new String[]{
                  "id",                //0
                  "name",              //1
                  "condition",         //2
                  "parameter",         //3
                  "operator",          //4
                  "meaning",           //5
                  "factor_id",         //6
                  "factor_name",       //7
                  "factor_unit",       //8
                  "factor_factorType", //9
                  "factor_listValues", //10
                  "factor_unique"},    //11
                new String[]{"factor"});
        LinkedHashMap<Integer,List<List>> factorRules = new LinkedHashMap<>();

        for(List d:data) {
          List<List> fr = factorRules.get(d.get(6));
          if(fr == null) {
            fr = new Vector<>();
            factorRules.put((Integer)d.get(6), fr);
          }
          fr.add(d);
        }

        List<List> dataVector = new Vector<>();
        for(Integer factorId:factorRules.keySet()) {
          Vector row = new Vector();
          dataVector.add(row);
          row.setSize(table.getColumnCount());
          row.set(0,factorId);
          List<List> rules = factorRules.get(factorId);
          row.set(1, rules.get(0).get(7));
          row.set(3, rules.get(0).get(8));
          row.set(2, createComboBox(dataVector.size()-1, rules));
        }

        if(table.getCellEditor() != null)
          table.getCellEditor().cancelCellEditing();
        table.getTableModel().getDataVector().addAll(dataVector);
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }finally {
      table.getTableModel().fireTableDataChanged();
    }
  }

  private JComboBox createComboBox(final int rowIndex, final List<List> data) {
    final JComboBox combo = new JComboBox(new String[]{"-----"});
    for(List d:data)
      combo.addItem(d.get(2)+" "+d.get(3));
    combo.addItemListener((ItemEvent e) -> {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        table.getCellEditor().stopCellEditing();
        if(combo.getSelectedIndex() == 0)
          table.setValueAt(0, rowIndex, 4);
        else {
          String operator = (String)data.get(combo.getSelectedIndex()-1).get(4);
          Double meaning  = (Double)data.get(combo.getSelectedIndex()-1).get(5);
          table.setValueAt(operator+" "+meaning, rowIndex, 4);
        }
        calculateCost();
      }
    });
    return combo;
  }
  
  @Override
  public void dispose() {
    pfrDivisionTarget.dispose();
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    setProducts(ids);
    initData();
  }

  @Override
  public void initTargets() {
  }
}