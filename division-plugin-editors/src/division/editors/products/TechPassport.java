package division.editors.products;

import bum.editors.EditorGui;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Product;
import division.swing.guimessanger.Messanger;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionToolButton;
import division.swing.multilinetable.MyCellEditor;
import division.swing.multilinetable.MyCellRenderer;
import division.util.FileLoader;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

public class TechPassport extends EditorGui {
  private final JToolBar toolBar = new JToolBar();
  private final DivisionToolButton addColumn = new DivisionToolButton(FileLoader.getIcon("column_add16.png"),"Добавить колонку");
  private final DivisionToolButton delColumn = new DivisionToolButton(FileLoader.getIcon("column_delete16.png"),"Удалить колонку");
  private final DivisionToolButton addRow = new DivisionToolButton(FileLoader.getIcon("row_add16.gif"),"Добавить строку");
  private final DivisionToolButton delRow = new DivisionToolButton(FileLoader.getIcon("row_delete16.png"),"Удалить строку");
  private final DivisionToolButton copy   = new DivisionToolButton(FileLoader.getIcon("copy.gif"),"Копировать паспорт");
  private final DivisionToolButton paste  = new DivisionToolButton(FileLoader.getIcon("paste.gif"),"Вставить паспорт");
  
  private final DivisionTable      table    = new DivisionTable();
  private final DivisionScrollPane scroll   = new DivisionScrollPane(table);
  private final MyCellRenderer     renderer = new MyCellRenderer();
  private final MyCellEditor       editor   = new MyCellEditor(renderer);
  
  private Integer[] products;

  public TechPassport() {
    super(null, null);
    initComponents();
    initEvents();
    setEnabled(false);
  }
  
  private void initComponents() {
    getRootPanel().setLayout(new BorderLayout());
    
    
    toolBar.add(addRow);
    toolBar.add(delRow);
    toolBar.addSeparator();
    toolBar.add(addColumn);
    toolBar.add(delColumn);
    toolBar.addSeparator();
    toolBar.add(copy);
    toolBar.add(paste);
    
    getRootPanel().add(toolBar, BorderLayout.NORTH);
    getRootPanel().add(scroll, BorderLayout.CENTER);
    
    table.setSortable(false);
    table.setFindable(false);
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getTableHeader().setReorderingAllowed(false);
    
    table.setCellEditableController((JTable table1, int modelRow, int modelColumn) -> true);
  }
  
  private void initEvents() {
    copy.addActionListener((ActionEvent e) -> {
      copyAction();
    });
    
    paste.addActionListener((ActionEvent e) -> {
      pasteAction();
    });
    
    addColumn.addActionListener((ActionEvent e) -> {
      renderer.setEditingColumnIndex(-1);
      int index = table.getSelectedColumn();
      
      if(table.getRowCount() == 0)
        table.getTableModel().addRow(new Object[]{""});
      
      Vector<Vector> data = table.getTableModel().getDataVector();
      if(index >= 0)
        data.stream().forEach(d -> d.insertElementAt("", index));
      else data.stream().forEach(d -> d.add(""));
      
      table.getTableModel().addColumn(" ");
      
      table.getTableModel().fireTableDataChanged();
      for(int i=0;i<table.getColumnCount();i++) {
        table.findTableColumn(i).setCellEditor(editor);
        table.findTableColumn(i).setCellRenderer(renderer);
      }
    });

    addRow.addActionListener((ActionEvent e) -> {
      if(table.getColumnCount() == 0)
        table.getTableModel().addColumn(" ");
      
      int index = table.getSelectedRow();
      Object[] row = new Object[table.getColumnCount()];
      Arrays.fill(row, "");
      if(index >= 0)
        table.getTableModel().insertRow(index, row);
      else table.getTableModel().addRow(row);
    });

    delColumn.addActionListener((ActionEvent e) -> {
      renderer.setEditingColumnIndex(-1);
      int index = table.getSelectedColumn();
      if(index >= 0) {
        table.getColumnModel().removeColumn(table.findTableColumn(index));
        table.getTableModel().removeColumn(index);
        if(table.getColumnCount() == 0)
          table.getTableModel().clear();
        table.getTableModel().fireTableStructureChanged();
      }
    });

    delRow.addActionListener((ActionEvent e) -> {
      int index = table.getSelectedRow();
      if(index >= 0) {
        table.getTableModel().removeRow(index);
        if(table.getRowCount() == 0)
          table.setColumns(new Object[0]);
        table.getTableModel().fireTableStructureChanged();
        index = table.getRowCount()>index?index:table.getRowCount()-1;
        if(index >= 0)
          table.setRowSelectionInterval(index, index);
      }
    });
  }
  
  @Override
  public void initData() {
    clear();
    if(isActive() && products != null && products.length > 0) {
      try {
        String passport = null;
        List<List> data = ObjectLoader.getData(Product.class, products, new String[]{"techPassport"});
        if(!data.isEmpty()) {
          passport = (String) data.get(0).get(0);
          if(passport != null) {
            for(List p:data) {
              if(!passport.equals(p.get(0))) {
                passport = null;
                break;
              }
            }
          }
        }
        setPassport(passport);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }

  @Override
  public void clear() {
    super.clear();
    table.getTableModel().clear();
    table.setColumns(new Object[0]);
  }

  @Override
  public void setActive(boolean active) {
    if(!active)
      savePassport();
    super.setActive(active);
  }
  
  private void setPassport(String passport) {
    if(passport != null && !passport.equals("")) {
      Vector<Vector> data = decodeTechPassport(passport);
      if(!data.isEmpty()) {
        Vector columns = new Vector();
        for(Object o:data.get(0))
          columns.add(" ");
        table.getTableModel().setDataVector(data, columns);
        table.getTableModel().fireTableDataChanged();

        for(int i=0;i<table.getColumnCount();i++) {
          table.findTableColumn(i).setCellEditor(editor);
          table.findTableColumn(i).setCellRenderer(renderer);
        }
      }
    }
  }
  
  private String getPassport() {
    String passport = "";
    Vector<Vector> data = table.getTableModel().getDataVector();
    if(!data.isEmpty()) {
      for(Vector dataRow:data) {
        passport += "ROW[";
        for(Object cell:dataRow) {
          passport += "CELL["+(cell==null?"":cell)+"]";
        }
        passport += "]";
      }
    }
    return passport;
  }
  
  public static Vector<Vector> decodeTechPassport(String passport) {
    Vector<Vector> data = new Vector<>();
    for(String row:passport.split("ROW")) {
      if(row != null && !"".equals(row)) {
        Vector dataRow = new Vector();
        for(String cell:row.substring(1, row.length()-1).split("CELL")) {
          if(!cell.equals(""))
            dataRow.add(cell.substring(1, cell.length()-1));
        }
        data.add(dataRow);
      }
    }
    return data;
  }
  
  public void savePassport() {
    if(table.getCellEditor() != null)
      table.getCellEditor().stopCellEditing();
    String passport = getPassport();
    if(isActive() && products != null && products.length > 0)
      ObjectLoader.executeUpdate("UPDATE [Product] SET [Product(techPassport)]=? WHERE id=ANY(?)", new Object[]{passport,products});
  }

  @Override
  public void dispose() {
    savePassport();
    super.dispose();
  }

  @Override
  public Boolean okButtonAction() {
    return false;
  }
  
  private void copyAction() {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(getPassport()), null);
  }
  
  private void pasteAction() {
    try {
      setPassport((String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }

  @Override
  public void changeSelection(EditorGui editor, Object... params) {
    savePassport();
    products = ((Integer[]) params[0]).length==0?(Integer[]) params[1]:(Integer[]) params[0];
    initData();
    setEnabled(products != null && products.length != 0);
  }

  @Override
  public void initTargets() {
  }
}