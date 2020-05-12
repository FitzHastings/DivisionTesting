package division.editors.products;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TableEditor;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.Document;
import bum.interfaces.ProductDocument;
import bum.interfaces.ProductDocument.ActionType;
import division.editors.objects.DocumentEditor;
import division.fx.PropertyMap;
import division.swing.DivisionScrollPane;
import division.swing.DivisionTable;
import division.swing.DivisionToolButton;
import division.swing.guimessanger.Messanger;
import division.swing.table.CellColorController;
import division.util.FileLoader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class ProductDocumentPanel extends EditorGui {
  private final DivisionTable      startTable      = new DivisionTable();
  private final DivisionScrollPane startScroll     = new DivisionScrollPane(startTable);
  
  private final DivisionTable      dispatchTable   = new DivisionTable();
  private final DivisionScrollPane dispatchScroll  = new DivisionScrollPane(dispatchTable);
  
  private final DivisionTable      payTable        = new DivisionTable();
  private final DivisionScrollPane payScroll       = new DivisionScrollPane(payTable);
  
  private ExecutorService pool = Executors.newFixedThreadPool(5);
  private InitDataTask initDataTask;
  
  private final CellColorController cellColorController = new CellColorController() {
    @Override
    public Color getCellColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
      return (boolean)table.getModel().getValueAt(modelRow, 2) ? Color.LIGHT_GRAY : null;
    }

    @Override
    public Color getCellTextColor(JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) {
      return null;
    }
  };
  
  private Integer[] products     = new Integer[0];
  
  private EditorListener startButtonListener;
  private EditorListener dispatchButtonListener;
  private EditorListener payButtonListener;

  public ProductDocumentPanel() {
    super(null, null);
    initComponents();
    initEvents();
    initTargets();
  }
  
  private JToolBar createActionToolBar(ActionType actionType) {
    JToolBar tool = new JToolBar(JToolBar.VERTICAL);
    tool.setFloatable(false);
    DivisionToolButton addToolButton     = new DivisionToolButton(FileLoader.getIcon("Add16.gif"),"Добавить");
    DivisionToolButton editToolButton    = new DivisionToolButton(FileLoader.getIcon("Edit16.gif"),"Редактировать");
    DivisionToolButton removeToolButton  = new DivisionToolButton(FileLoader.getIcon("Delete16.gif"),"Удалить");
    tool.add(addToolButton);
    tool.add(editToolButton);
    tool.add(removeToolButton);
    addToolButton.addActionListener((ActionEvent e) -> addDocument(actionType));
    editToolButton.addActionListener((ActionEvent e) -> editDocument(actionType));
    removeToolButton.addActionListener((ActionEvent e) -> removeDocument(actionType));
    
    addToolButton.setEnabled(false);
    editToolButton.setEnabled(false);
    removeToolButton.setEnabled(false);
    
    DivisionTable table = actionType==ActionType.СТАРТ ? startTable : actionType==ActionType.ОТГРУЗКА ? dispatchTable : payTable;
    
    final EditorListener buttonListener = new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        addToolButton.setEnabled(ids.length > 0);
        editToolButton.setEnabled(table.getSelectedRowCount() > 0 && ids.length > 0);
        removeToolButton.setEnabled(table.getSelectedRowCount() > 0 && ids.length > 0);
      }
    };
    
    table.addTableSelectionListener((int[] oldSelection, int[] newSelection) -> {
      editToolButton.setEnabled(table.getSelectedRowCount()>0);
      removeToolButton.setEnabled(table.getSelectedRowCount()>0 && !(boolean)table.getTableModel().getValueAt(table.getSelectedRow(), 2));
    });
    
    switch(actionType) {
      case СТАРТ:
        startButtonListener = buttonListener;
        break;
      case ОТГРУЗКА:
        dispatchButtonListener = buttonListener;
        break;
      case ОПЛАТА:
        payButtonListener = buttonListener;
        break;
    }
    
    return tool;
  }
  
  private void addDocument(ActionType actionType) {
    RemoteSession session = null;
    try {
      if(products.length > 0) {
        TableEditor documentTableEditor = new TableEditor(
        new String[]{"id","Наименование"},
        new String[]{"id","name"},
        Document.class,
        DocumentEditor.class,
        "Документы",
        MappingObject.Type.CURRENT);
        documentTableEditor.setAutoLoad(true);
        documentTableEditor.setAutoStore(true);
        documentTableEditor.getClientFilter().AND_EQUAL("system", false);
        documentTableEditor.initData();
        Integer[] documents = documentTableEditor.get();
        if(documents.length > 0) {
          session = ObjectLoader.createSession();
          List<List> data = session.executeQuery("SELECT MAX(id) FROM [ProductDocument]");
          int startId = data.isEmpty()||data.get(0).get(0)==null?0:(Integer)data.get(0).get(0);

          String query = "INSERT INTO [ProductDocument] ([ProductDocument(product)],[ProductDocument(document)],[ProductDocument(actionType)]) VALUES ";
          for(Integer productId:products)
            for(Integer documentId:documents)
              query += "("+productId+","+documentId+",'"+actionType+"'),";

          session.executeUpdate(query.substring(0, query.length()-1));
          data = session.executeQuery("SELECT id FROM [ProductDocument] WHERE id > "+startId);
          Integer[] ids = new Integer[0];
          for(List d:data)
            ids = (Integer[]) ArrayUtils.add(ids, d.get(0));
          session.addEvent(ProductDocument.class, "CREATE", ids);
          session.commit();
        }
      }
    }catch(Exception ex) {
      ObjectLoader.rollBackSession(session);
      Messanger.showErrorMessage(ex);
    }
  }
  
  private void editDocument(ActionType actionType) {
    DivisionTable table = actionType==ActionType.СТАРТ ? startTable : actionType==ActionType.ОТГРУЗКА ? dispatchTable : payTable;
    if(table.getSelectedRowCount() > 0) {
      try {
        DocumentEditor editor = new DocumentEditor();
        editor.setAutoLoadAndStore(true);
        editor.setEditorObject((Document) ObjectLoader.getObject(Document.class, (Integer) table.getTableModel().getValueAt(table.getSelectedRow(), 1)));
        editor.createDialog().setVisible(true);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
  }
  
  private void removeDocument(ActionType actionType) {
    DivisionTable table = actionType==ActionType.СТАРТ ? startTable : actionType==ActionType.ОТГРУЗКА ? dispatchTable : payTable;
    if(table.getSelectedRowCount() > 0 && 
            JOptionPane.showConfirmDialog(getGUI(), "Удалить?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == 0) {
      Integer[] ids = new Integer[0];
      for(int row:table.getSelectedRows())
        ids = (Integer[]) ArrayUtils.addAll(ids, (Integer[]) table.getTableModel().getValueAt(row, 0));
      ObjectLoader.removeObjects(ProductDocument.class, ids);
    }
  }
  
  private int getRowId(Integer id, ActionType actionType) {
    DivisionTable table = actionType==ActionType.СТАРТ ? startTable : actionType==ActionType.ОТГРУЗКА ? dispatchTable : payTable;
    for(int i=0;i<table.getTableModel().getRowCount();i++)
      if(!(boolean)table.getTableModel().getValueAt(i, 2) && 
              ArrayUtils.contains((Integer[]) table.getTableModel().getValueAt(i, 0), id))
        return i;
    return -1;
  }
  
  private void initComponents() {
    startTable.setColumns("id","docid","system","Документ","");
    dispatchTable.setColumns("id","docid","system","Документ","");
    payTable.setColumns("id","docid","system","Документ","");
    
    startTable.setColumnWidthZero(0,1,2);
    dispatchTable.setColumnWidthZero(0,1,2);
    payTable.setColumnWidthZero(0,1,2);
    
    startTable.setCellEditableController((JTable table, int modelRow, int modelColumn) -> {
      return products.length > 0 && (boolean)table.getModel().getValueAt(modelRow, 2);
    });
    dispatchTable.setCellEditableController((JTable table, int modelRow, int modelColumn) -> {
      return products.length > 0 && (boolean)table.getModel().getValueAt(modelRow, 2);
    });
    payTable.setCellEditableController((JTable table, int modelRow, int modelColumn) -> {
      return products.length > 0 && (boolean)table.getModel().getValueAt(modelRow, 2);
    });
    
    JPanel startPanel = new JPanel(new BorderLayout());
    startPanel.setBorder(BorderFactory.createTitledBorder("СТАРТ"));
    startPanel.add(createActionToolBar(ActionType.СТАРТ), BorderLayout.WEST);
    startPanel.add(startScroll, BorderLayout.CENTER);
    
    JPanel dispatchPanel = new JPanel(new BorderLayout());
    dispatchPanel.setBorder(BorderFactory.createTitledBorder("ОТГРУЗКА"));
    dispatchPanel.add(createActionToolBar(ActionType.ОТГРУЗКА), BorderLayout.WEST);
    dispatchPanel.add(dispatchScroll, BorderLayout.CENTER);
    
    JPanel payPanel = new JPanel(new BorderLayout());
    payPanel.setBorder(BorderFactory.createTitledBorder("ОПЛАТА"));
    payPanel.add(createActionToolBar(ActionType.ОПЛАТА), BorderLayout.WEST);
    payPanel.add(payScroll, BorderLayout.CENTER);
    
    getRootPanel().setLayout(new GridLayout(1, 3));
    
    getRootPanel().add(startPanel);
    getRootPanel().add(dispatchPanel);
    getRootPanel().add(payPanel);
    
    startTable.setCellColorController(cellColorController);
    dispatchTable.setCellColorController(cellColorController);
    payTable.setCellColorController(cellColorController);
    
    addComponentToStore(startTable);
    addComponentToStore(dispatchTable);
    addComponentToStore(payTable);
  }
  
  private int removeProductDocument(ActionType actionType, Integer id) {
    int row = getRowId(id, actionType);
    DivisionTable table = actionType==ActionType.СТАРТ ? startTable : actionType==ActionType.ОТГРУЗКА ? dispatchTable : payTable;
    if(row >= 0) {
      Integer[] pids = (Integer[]) ((Vector)table.getTableModel().getDataVector().get(row)).get(0);
      pids = (Integer[]) ArrayUtils.removeElement(pids, id);
      if(pids.length > 0)
        ((Vector)table.getTableModel().getDataVector().get(row)).set(0, pids);
      else {
        ((Vector)table.getTableModel().getDataVector()).remove(row);
        table.getTableModel().fireTableDataChanged();
      }
    }
    return row;
  }

  @Override
  public void initTargets() {
    addTarget(new DivisionTarget(ProductDocument.class) {
      @Override
      public void messageReceived(String type, Integer[] ids, PropertyMap objectEventProperty) {
        if(isActive()) {
          int[] startRows    = startTable.getSelectedRows();
          int[] dispatchRows = dispatchTable.getSelectedRows();
          int[] payRows      = payTable.getSelectedRows();
          if("REMOVE".equals(type)) {
            for(Integer id:ids) {
              int row = -1;
              if((row = removeProductDocument(ActionType.СТАРТ, id)) >= 0)
                startRows = ArrayUtils.removeElement(startRows, row);
              
              if((row = removeProductDocument(ActionType.ОТГРУЗКА, id)) >= 0)
                dispatchRows = ArrayUtils.removeElement(dispatchRows, row);
              
              if((row = removeProductDocument(ActionType.ОПЛАТА, id)) >= 0)
                payRows = ArrayUtils.removeElement(payRows, row);
            }
          }
          if("CREATE".equals(type)) {
            if(ids.length > 0) {
              startTable.getTableModel().getDataVector().addAll(initActionTable(ActionType.СТАРТ, ObjectLoader.isSatisfy(DBFilter.create(ProductDocument.class).AND_IN("product", products).
                    AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false).AND_EQUAL("actionType", ActionType.СТАРТ), ids)));
              startTable.getTableModel().fireTableDataChanged();
              
              dispatchTable.getTableModel().getDataVector().addAll(initActionTable(ActionType.ОТГРУЗКА, ObjectLoader.isSatisfy(DBFilter.create(ProductDocument.class).AND_IN("product", products).
                    AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false).AND_EQUAL("actionType", ActionType.ОТГРУЗКА), ids)));
              dispatchTable.getTableModel().fireTableDataChanged();
              
              payTable.getTableModel().getDataVector().addAll(initActionTable(ActionType.ОПЛАТА, ObjectLoader.isSatisfy(DBFilter.create(ProductDocument.class).AND_IN("product", products).
                    AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false).AND_EQUAL("actionType", ActionType.ОПЛАТА), ids)));
              payTable.getTableModel().fireTableDataChanged();
            }
          }
        }
      }
    });
  }

  private void initEvents() {
    startTable.getTableModel().addTableModelListener((TableModelEvent e) -> updateTable(e, ActionType.СТАРТ));
    dispatchTable.getTableModel().addTableModelListener((TableModelEvent e) -> updateTable(e, ActionType.ОТГРУЗКА));
    payTable.getTableModel().addTableModelListener((TableModelEvent e) -> updateTable(e, ActionType.ОПЛАТА));
  }
  
  private void updateTable(TableModelEvent e, ActionType actionType) {
    if(e.getType() == TableModelEvent.UPDATE) {
        int row = e.getLastRow();
        int col = e.getColumn();
        if(col == 4) {
          if(!(Boolean) ((DefaultTableModel)e.getSource()).getValueAt(row, col)) {
            Integer document = (Integer) ((DefaultTableModel)e.getSource()).getValueAt(row, 1);
            for(Integer product:products) {
              Map<String,Object> map = new TreeMap<>();
              map.put("actionType", actionType);
              map.put("product",    product);
              map.put("document",   document);
              ObjectLoader.createObject(ProductDocument.class, map);
            }
          }else {
            ObjectLoader.removeObjects(ProductDocument.class, (Integer[])((DefaultTableModel)e.getSource()).getValueAt(row, 0));
          }
        }
      }
  }
  
  private List<List> initActionTable(ActionType actionType, Integer[] pids) {
    final List<List> data = new Vector<>();
    final List<List> documentData = new Vector<>();
    
    if(pids == null) {
      documentData.addAll(ObjectLoader.getData(DBFilter.create(Document.class).AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false)
              .AND_EQUAL("system", true).AND_EQUAL("actionType", actionType), "id","system","name","query:true"));
      documentData.stream().forEach(d -> {
        d.add(0, new Integer[0]);
      });
      
      data.addAll(ObjectLoader.getData(DBFilter.create(ProductDocument.class).AND_IN("product", products).AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false).AND_EQUAL("actionType", actionType), 
              new String[]{"id","document","document_name","document-system","query:null"}, "document_name"));
    }else data.addAll(ObjectLoader.getData(ProductDocument.class, pids, new String[]{"id","document","document_name","document-system","query:null"}, new String[]{"document_name"}));
    
    for(int i=data.size()-1;i>=0;i--) {
      if((boolean)data.get(i).get(3)) {
        for(List doc:documentData) {
          if(doc.get(1).equals(data.get(i).get(1))) {
            doc.set(0, ArrayUtils.add((Integer[])doc.get(0), data.get(i).get(0)));
            doc.set(4, false);
          }
        }
        data.remove(i);
      }
    }
    
    for(int i=0;i<data.size();i++) {
      Integer[] ids = new Integer[]{(Integer)data.get(i).get(0)};
      for(int j=data.size()-1;j>=0;j--)
        if(i != j && data.get(j).get(1).equals(data.get(i).get(1)))
          ids = (Integer[]) ArrayUtils.add(ids, data.remove(j).get(0));
      data.get(i).set(0, ids);
      data.get(i).remove(3);
      data.get(i).add(2, false);
    }
    documentData.addAll(data);
    return documentData;
  }
  
  @Override
  public void clear() {
    clear(ActionType.СТАРТ);
    clear(ActionType.ОТГРУЗКА);
    clear(ActionType.ОПЛАТА);
  }

  public void clear(ActionType actionType) {
    DivisionTable table = actionType==ActionType.СТАРТ ? startTable : actionType==ActionType.ОТГРУЗКА ? dispatchTable : payTable;
    table.getTableModel().clear();
  }
  
  @Override
  public void initData() {
    clear();
    if(initDataTask != null) {
      initDataTask.shutDown();
      initDataTask = null;
    }
    if(isActive())
      pool.submit(initDataTask = new InitDataTask());
  }
  
  @Override
  public void dispose() {
  }
  

  @Override
  public Boolean okButtonAction() {
    return true;
  }
  
  @Override
  public void changeSelection(EditorGui editor, Object... params) {
    SwingUtilities.invokeLater(() -> {
      products = ((ProductTree)editor).getSelectedProductId(false, true);
      if(products.length == 0)
        products = ((ProductTree)editor).getSelectedProductId(true, true);
      //products = ((Integer[]) params[0]).length==0?(Integer[]) params[1]:(Integer[]) params[0];
      startButtonListener.changeSelection(editor, products);
      dispatchButtonListener.changeSelection(editor, products);
      payButtonListener.changeSelection(editor, products);
      initData();
    });
  }
  
  class InitDataTask implements Runnable {
    private boolean shutdown = false;

    @Override
    public void run() {
      if(shutdown)
        return;
      waitCursor();
      final List<List> startData    = initActionTable(ActionType.СТАРТ, null);
      if(shutdown)
        return;
      final List<List> dispatchData = initActionTable(ActionType.ОТГРУЗКА, null);
      if(shutdown)
        return;
      final List<List> payData      = initActionTable(ActionType.ОПЛАТА, null);
      if(shutdown)
        return;
      SwingUtilities.invokeLater(() -> {
        if(shutdown)
          return;
        startTable.getTableModel().getDataVector().addAll(startData);
        dispatchTable.getTableModel().getDataVector().addAll(dispatchData);
        payTable.getTableModel().getDataVector().addAll(payData);
        
        startTable.getTableModel().fireTableDataChanged();
        dispatchTable.getTableModel().fireTableDataChanged();
        payTable.getTableModel().fireTableDataChanged();
        defaultCursor();
      });
    }
    
    public void shutDown() {
      shutdown = true;
      defaultCursor();
    }
  }
}