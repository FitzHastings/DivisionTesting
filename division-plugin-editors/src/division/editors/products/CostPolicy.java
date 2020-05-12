package division.editors.products;

import bum.editors.EditorGui;
import bum.editors.TableEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Factor;
import bum.interfaces.Product;
import bum.interfaces.ProductFactorRule;
import division.editors.tables.FactorTableEditor;
import division.editors.tables.ProductFactorRuleTableEditor;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionToolButton;
import division.swing.table.CellColorController;
import division.util.FileLoader;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.Element;
import util.RemoteSession;
import util.filter.local.DBExpretion.EqualType;

public class CostPolicy extends EditorGui {
  private final DivisionToolButton up   = new DivisionToolButton(FileLoader.getIcon("arrow1_up.GIF"));
  private final DivisionToolButton down = new DivisionToolButton(FileLoader.getIcon("arrow1_down.GIF"));
  
  private final JSplitPane split = new JSplitPane();
  
  private final ProductFactorRuleTableEditor productFactorRuleTableEditor = new ProductFactorRuleTableEditor();
  
  private final TableEditor   factorTableEditor   = new TableEditor(
          new String[]{"id","productFactor","Наименование","Ед. изм."/*,"порядок применения"*/},
          new String[]{"id","productFactor","name","unit"},
          Factor.class,
          null,
          "",
          MappingObject.Type.CURRENT);
  
  private Integer[] products;
  
  public CostPolicy() {
    super("Ценовая политика", null);
    initComponents();
    initEvents();
  }

  public Integer[] getProducts() {
    return products;
  }

  public void setProducts(Integer[] products) {
    this.products = products;
    factorTableEditor.clear();
    factorTableEditor.getClientFilter().clear();
    productFactorRuleTableEditor.clear();
    productFactorRuleTableEditor.getClientFilter().clear();
    if(products != null && products.length > 0) {
      try {
        List<List> data = ObjectLoader.executeQuery("SELECT DISTINCT a.[!Factor(groups):object] "
                + "FROM [Factor(groups):table] a "
                + "WHERE (SELECT [Factor(productFactor)] FROM [Factor] WHERE [Factor(id)]=a.[!Factor(groups):object]) = false AND "
                + "array(SELECT DISTINCT [Product(group)] FROM [Product] WHERE [Product(id)]=ANY(?)) = "
                + "(SELECT array_agg(c) FROM unnest(array(SELECT DISTINCT [Product(group)] FROM [Product] WHERE [Product(id)]=ANY(?)"
                + ")) c WHERE c IN (SELECT DISTINCT b.[!Factor(groups):target] FROM [Factor(groups):table] b WHERE a.[!Factor(groups):object]=b.[!Factor(groups):object]))", 
                true, new Object[]{products,products});
        Integer[] ids = new Integer[0];
        for(List d:data)
          ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
        
        factorTableEditor.setSortFields(new String[]{"productFactor"});
        factorTableEditor.getAddButton().setEnabled(products.length > 0);
        factorTableEditor.getClientFilter().clear();
        factorTableEditor.getClientFilter().AND_IN("products", products);
        if(ids.length > 0)
          factorTableEditor.getClientFilter().OR_IN("id", ids);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  private void initComponents() {
    factorTableEditor.setPrintFunction(false);
    factorTableEditor.setExportFunction(false);
    factorTableEditor.setImportFunction(false);
    
    factorTableEditor.getToolBar().addSeparator();
    factorTableEditor.getToolBar().add(up);
    factorTableEditor.getToolBar().add(down);
    factorTableEditor.getToolBar().setOrientation(JToolBar.VERTICAL);
    factorTableEditor.getlPanel().setLayout(new GridLayout(1, 1));
    factorTableEditor.getlPanel().add(factorTableEditor.getToolBar());
    
    factorTableEditor.setSingleSelection(true);
    factorTableEditor.getTable().setColumnWidthZero(0,1);
    
    productFactorRuleTableEditor.setPrintFunction(false);
    productFactorRuleTableEditor.setExportFunction(false);
    productFactorRuleTableEditor.setImportFunction(false);
    
    productFactorRuleTableEditor.getToolBar().setOrientation(JToolBar.VERTICAL);
    productFactorRuleTableEditor.getlPanel().setLayout(new GridLayout(1, 1));
    productFactorRuleTableEditor.getlPanel().add(productFactorRuleTableEditor.getToolBar());
    
    addComponentToStore(split);
    addSubEditorToStore(factorTableEditor, "productFactorTableEditorForCostPolicy");
    addSubEditorToStore(productFactorRuleTableEditor, "productFactorValueTableEditorForCostPolicy");
    
    getRootPanel().setLayout(new GridBagLayout());
    factorTableEditor.setVisibleOkButton(false);
    productFactorRuleTableEditor.setVisibleOkButton(false);
    split.add(factorTableEditor.getGUI(), JSplitPane.LEFT);
    split.add(productFactorRuleTableEditor.getGUI(), JSplitPane.RIGHT);
    
    getRootPanel().add(split,       new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    
    factorTableEditor.getTable().setCellColorController(new CellColorController() {
      @Override
      public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(!(Boolean)table.getModel().getValueAt(modelRow, 1))
          return Color.LIGHT_GRAY;
        else {
          if(isSelect)
            return table.getSelectionBackground();
          else
            return table.getBackground();
        }
      }

      @Override
      public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
        if(isSelect)
          return table.getSelectionForeground();
        else
          return table.getForeground();
      }
    });
  }
  
  private Integer[] saveFormula() {
    try {
      Integer[] formula = new Integer[0];
      for(int i=0;i<factorTableEditor.getTable().getRowCount();i++)
        formula = (Integer[]) ArrayUtils.add(formula, factorTableEditor.getTable().getValueAt(i, 0));
      ObjectLoader.executeUpdate("UPDATE [Product] SET [Product(formula)]=? WHERE "+Element.getQuery("id", products, EqualType.IN), new Object[]{formula}, true);
      return formula;
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return null;
  }
  
  private void changeNumber(boolean up) {
    int row = factorTableEditor.getTable().getSelectedRow();
    if(row >= 0) {
      int newRow = up?row-1:(row+1);
      if(newRow >= 0 && newRow < factorTableEditor.getTable().getRowCount()) {
        factorTableEditor.getTable().getTableModel().moveRow(row, row, newRow);
        factorTableEditor.getTable().setRowSelectionInterval(newRow, newRow);
        saveFormula();
      }
    }
  }

  private void initEvents() {
    up.addActionListener(e -> changeNumber(true));
    down.addActionListener(e -> changeNumber(false));
    factorTableEditor.addEditorListener(this);
    
    factorTableEditor.setRemoveAction((ActionEvent e) -> {
      removeFactorsFromProducts();
    });

    factorTableEditor.setAddAction((ActionEvent e) -> {
      addFactorsToProducts();
    });
    
    productFactorRuleTableEditor.setAddAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(factorTableEditor.getSelectedObjectsCount() == 1) {
          RemoteSession session = null;
          try {
            session = ObjectLoader.createSession();
            boolean validate = true;
            if(productFactorRuleTableEditor.getAllIds().length > 0) {
              for(List d:session.executeQuery("SELECT [ProductFactorRule(condition)], [ProductFactorRule(parameter)], "
                      + "[ProductFactorRule(operator)], [ProductFactorRule(meaning)] FROM [ProductFactorRule] "
                      + "WHERE "+Element.getQuery("id", productFactorRuleTableEditor.getAllIds(), EqualType.IN))) {
                if(d.contains(null)) {
                  validate = false;
                  break;
                }
              }
            }
            if(!validate) {
             Messanger.alert("Необходимо дооформить правило.", "Необходимо дооформить правило.", JOptionPane.ERROR_MESSAGE);
            }else {
              Integer startId = 0;
              List<List> data = session.executeQuery("SELECT MAX([ProductFactorRule(id)]) FROM [ProductFactorRule]");
              if(!data.isEmpty() && data.get(0).get(0) != null)
                startId = (Integer) data.get(0).get(0);
              String[] querys = new String[0];
              for(Integer pid:products)
                querys = (String[]) ArrayUtils.add(querys, "INSERT INTO [ProductFactorRule]([ProductFactorRule(factor)],[ProductFactorRule(product)]) VALUES "
                        + "("+factorTableEditor.getSelectedId()[0]+","+pid+")");

              session.executeUpdate(querys);
              data = session.executeQuery("SELECT [ProductFactorRule(id)] FROM [ProductFactorRule] WHERE [ProductFactorRule(id)] > "+startId);
              Integer[] ids = new Integer[0];
              for(List d:data)
                ids = (Integer[]) ArrayUtils.add(ids, d.get(0));

              if(ids.length > 0)
                session.addEvent(ProductFactorRule.class, "CREATE", ids);
            }
            session.commit();
          }catch(Exception ex) {
            ObjectLoader.rollBackSession(session);
            Messanger.showErrorMessage(ex);
          }
        }
      }
    });
  }

  @Override
  public void initDataComplited(EditorGui editor) {
    try {
      Integer[] formula = null;
      List<List> formulaData = ObjectLoader.getData(Product.class, products, new String[]{"formula"});
      if(formulaData.isEmpty() || formulaData.get(0).get(0) == null || ((Integer[])formulaData.get(0).get(0)).length == 0)
        formula = saveFormula();
      else formula = (Integer[]) formulaData.get(0).get(0);
      if(formula != null) {
        int newRow = 0;
        for(int i=0;i<formula.length;i++) {
          int oldRow = factorTableEditor.getRowObject(formula[i]);
          if(oldRow >= 0) {
            factorTableEditor.getTable().getTableModel().moveRow(oldRow, oldRow, newRow++);
          }
        }
      }
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void changeSelection(EditorGui editor, Integer[] ids) {
    productFactorRuleTableEditor.clear();
    productFactorRuleTableEditor.getClientFilter().clear();
    productFactorRuleTableEditor.setActive(ids.length == 1);
    if(factorTableEditor.getSelectedObjectsCount() == 1) {
      /*productFactorRuleTableEditor.setProducts(products);
      productFactorRuleTableEditor.setFactor(factorTableEditor.getSelectedId()[0]);*/
      productFactorRuleTableEditor.getClientFilter().AND_EQUAL("factor", ids[0])
              .AND_IN("product", products);
      productFactorRuleTableEditor.initData();
    }
  }
  
  private void addFactorsToProducts() {
    FactorTableEditor fte = new FactorTableEditor();
    fte.setAutoLoad(true);
    fte.setAutoStore(true);
    fte.getClientFilter().AND_EQUAL("productFactor", true).AND_NOT_IN("products", products);
    fte.initData();
    Integer[] ids = fte.get();

    RemoteSession session = null;
    try {
      session = ObjectLoader.createSession(true);
      String[] querys = new String[0];
      Object[][] values = new Object[0][2];
      for(Integer pId:products) {
        for(Integer fId:ids) {
          querys = (String[]) ArrayUtils.add(querys, "INSERT INTO [Product(factors):table]"+
              "([Product(factors):object],[Product(factors):target]) VALUES(?,?)");
          values = (Object[][]) ArrayUtils.add(values, new Object[]{pId,fId});
        }
      }

      session.executeUpdate(querys, values);
      session.addEvent(Factor.class, "UPDATE", ids);
      session.commit();
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void removeFactorsFromProducts() {
    Integer[] ids = factorTableEditor.getSelectedId();

    for(int i=ids.length-1;i>=0;i--) {
      if(!(Boolean)factorTableEditor.getTable().getTableModel().getValueAt(factorTableEditor.getRowObject(ids[i]), 1))
        ids = (Integer[]) ArrayUtils.remove(ids, i);
    }

    if(ids.length > 0) {
      RemoteSession session = null;
      try {
        String[] querys = new String[0];
        for(Integer pId:products) {
          for(Integer fId:ids) {
            querys = (String[]) ArrayUtils.add(querys, "DELETE FROM [Product(factors):table]"+
                " WHERE [Product(factors):object]="+pId+" AND [Product(factors):target]="+fId);
            querys = (String[]) ArrayUtils.add(querys, "DELETE FROM [ProductFactorRule]"+
                " WHERE [ProductFactorRule(product)]="+pId+" AND [ProductFactorRule(factor)]="+fId);
          }
        }
        session = ObjectLoader.createSession();
        session.executeUpdate(querys);
        session.addEvent(Factor.class, "UPDATE", ids);
        session.commit();
      }catch(Exception ex) {
        ObjectLoader.rollBackSession(session);
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  @Override
  public void initData() {
    factorTableEditor.initData();
  }

  @Override
  public void initTargets() {
  }
}