package division.editors.objects.company;

import bum.editors.EditorGui;
import bum.editors.EditorListener;
import bum.editors.TableEditor;
import bum.editors.XMLTemplateEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Document;
import bum.interfaces.DocumentXMLTemplate;
import division.editors.objects.DocumentEditor;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.editor.GLoader;
import division.fx.tree.FXDivisionTreeTable;
import division.fx.tree.TreeColumn;
import division.fx.util.MsgTrash;
import division.swing.DivisionSplitPane;
import division.swing.guimessanger.Messanger;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class CompanyPartitionDocTemplateTable extends EditorGui {
  private Integer companyPartitionId;
  
  private final DivisionSplitPane rootSplit = new DivisionSplitPane(JSplitPane.HORIZONTAL_SPLIT);
  private final DivisionSplitPane leftSplit = new DivisionSplitPane(JSplitPane.VERTICAL_SPLIT);
  
  private final TableEditor documentTableEditor = new TableEditor(
          new String[]{"id","Наименование", "system"}, 
          new String[]{"id","name", "system"}, 
          Document.class, 
          DocumentEditor.class, 
          MappingObject.Type.CURRENT);
  
  private final TableEditor templateTableEditor = new TableEditor(
          new String[]{"id","Наименование",""}, 
          new String[]{"id","name","main"}, 
          DocumentXMLTemplate.class, 
          null, 
          MappingObject.Type.CURRENT);
  
  private final XMLTemplateEditor templateEditor = new XMLTemplateEditor();
  
  public CompanyPartitionDocTemplateTable() {
    super(null, null);
    initComponents();
    initEvents();
  }

  public Integer getCompanyPartitionId() {
    return companyPartitionId;
  }

  public void setCompanyPartitionId(Integer companyPartitionId) {
    this.companyPartitionId = companyPartitionId;
  }

  @Override
  public Boolean okButtonAction() {
    return true;
  }

  @Override
  public void initData() {
    documentTableEditor.initData();
  }

  private void initEvents() {
    documentTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        try {
          templateEditor.setActive(templateTableEditor.getSelectedObjectsCount() > 0);
          templateEditor.setEnabled(templateTableEditor.getSelectedObjectsCount() > 0);
          
          if(documentTableEditor.getSelectedObjectsCount() > 0) {
            templateTableEditor.getClientFilter().clear().AND_EQUAL("companyPartition", companyPartitionId)
                    .AND_EQUAL("document", ids[0]);
            templateTableEditor.initData();
          }else {
            templateEditor.setEditorObject(null);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    templateTableEditor.addEditorListener(new EditorListener() {
      @Override
      public void changeSelection(EditorGui editor, Integer[] ids) {
        try {
          templateEditor.setActive(templateTableEditor.getSelectedObjectsCount() > 0);
          templateEditor.setEnabled(templateTableEditor.getSelectedObjectsCount() > 0);
          
          if(templateTableEditor.getSelectedObjectsCount() > 0) {
            DocumentXMLTemplate template = (DocumentXMLTemplate) ObjectLoader.getObject(DocumentXMLTemplate.class, ids[0]);
            templateEditor.setEnabled(true);
            templateEditor.setEditorObject(template);
          }else {
            templateEditor.setEditorObject(null);
          }
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
    
    templateTableEditor.setAddAction((ActionEvent e) -> {
      if(documentTableEditor.getSelectedObjectsCount() > 0) {
        try {
          Map<String,Object> map = new TreeMap();
          map.put("name", "Новый шаблон");
          map.put("document", documentTableEditor.getSelectedId()[0]);
          map.put("companyPartition", getCompanyPartitionId());
          ObjectLoader.createObject(DocumentXMLTemplate.class, map, true);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      }
    });
  }

  private void initComponents() {
    documentTableEditor.setSortFields("system desc");
    
    JButton customPref = new JButton("Дополнительная настройка");
    templateTableEditor.getToolBar().add(customPref);
    
    customPref.addActionListener(e -> {
      Platform.runLater(() -> {
        TreeTableView<PropertyMap> documents = new FXDivisionTreeTable<>(new TreeItem<PropertyMap>(PropertyMap.create().setValue("name", "Документы")));
        documents.setShowRoot(false);
        
        TreeColumn<PropertyMap,String>  name = new TreeColumn("Наименование", "name", null, true, false);
        TreeColumn<PropertyMap,Boolean> main = new TreeColumn("Использовать по умолчанию", "main", null, true, false);
        
        documents.getColumns().addAll(name, main);
        
        name.setCellFactory((TreeTableColumn<PropertyMap, String> param) -> new TextFieldTreeTableCell<PropertyMap, String>() {
          @Override
          public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setStyle("");
            if(!empty && getTreeTableRow() != null && getTreeTableRow().getItem() != null) {
              if(getTreeTableRow().getTreeItem().getParent().equals(getTreeTableView().getRoot()))
                setStyle("-fx-font-weight:bold");
              else {
                if(getTreeTableRow().getItem().isNull("companyPartition") || getTreeTableRow().getItem().getInteger("companyPartition") == 0)
                  setStyle("-fx-font-style:italic");
                else setStyle("-fx-font-style:normal");
              }
            }
          }
        });
        
        ObjectLoader.getList(DBFilter.create(Document.class).AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false), "id","name")
                .sorted((PropertyMap o1, PropertyMap o2) -> o1.getString("name").compareTo(o2.getString("name")))
                .forEach(d -> {
                  TreeItem<PropertyMap> doc = new TreeItem<>(d);
                  documents.getRoot().getChildren().add(doc);
                  doc.setExpanded(true);
                });
        
        DBFilter filter = DBFilter.create(DocumentXMLTemplate.class).AND_EQUAL("type", Document.Type.CURRENT).AND_EQUAL("tmp", false);
        filter.AND_FILTER().AND_EQUAL("companyPartition", null).OR_EQUAL("companyPartition", getCompanyPartitionId());
        
        ObjectLoader.getList(filter, "id", "name", "document", "main", "companyPartition")
                .sorted((PropertyMap o1, PropertyMap o2) -> (o1.getValue("companyPartition", 0)+o1.getString("name")).compareTo(o2.getValue("companyPartition", 0)+o2.getString("name")))
                .forEach(t -> documents.getRoot().getChildren().filtered(d -> t.getInteger("document").equals(d.getValue().getInteger("id"))).forEach(d -> {
                  t.get("main").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                    if((boolean) newValue)
                      d.getChildren().filtered(c -> !Objects.equals(c.getValue().getInteger("id"), t.getInteger("id"))).forEach(c -> c.getValue().setValue("main", false));
                  });
                  d.getChildren().add(new TreeItem<>(t));
                  d.getValue().getList("templates").add(t);
                }));
        
        PropertyMap cp = ObjectLoader.getMap(CompanyPartition.class, getCompanyPartitionId(), "defaultTemplates");
        Integer[] arr = cp.getArray("defaultTemplates", Integer.class);
        
        Arrays.stream(arr).forEach(defTemp -> 
          documents.getRoot().getChildren().stream()
                  .flatMap(itemDoc -> itemDoc.getChildren().stream()
                          .filter(itemTemp -> Objects.equals(itemTemp.getValue().getInteger("id"), defTemp)))
                  .forEach(itemTemp -> itemTemp.getValue().setValue("main", true)));
        
        List<PropertyMap> memento = documents.getRoot().getChildren().stream().map(it -> it.getValue().copy()).collect(Collectors.toList());
        
        GLoader.load("documents", documents);
        
        FXD fxd = FXD.show(a -> {
          List<PropertyMap> current = documents.getRoot().getChildren().stream().map(it -> it.getValue()).collect(Collectors.toList());
          if(!current.equals(memento)) {
            try {
              return ObjectLoader.saveObject(CompanyPartition.class, 
                      PropertyMap.create().setValue("id", getCompanyPartitionId())
                      .setValue("defaultTemplates", documents.getRoot().getChildren().stream()
                              .flatMap(itemDoc -> itemDoc.getChildren().stream()
                                      .filter(itemTemp -> itemTemp.getValue().is("main"))
                                      .map(itemTemp -> itemTemp.getValue().getInteger("id")))
                              .collect(Collectors.toList()).toArray(new Integer[0])));
            } catch (Exception ex) {
              MsgTrash.out(ex);
              return false;
            }
          }else return true;
        }, "Шаблоны документов", documents);
        
        fxd.setOnHiding(a -> GLoader.store("documents", documents));
      });
    });
    
    addComponentToStore(rootSplit);
    addComponentToStore(leftSplit);
    
    addSubEditorToStore(documentTableEditor);
    addSubEditorToStore(templateTableEditor);
    
    getRootPanel().setLayout(new BorderLayout());
    getRootPanel().add(rootSplit, BorderLayout.CENTER);
    
    documentTableEditor.getTable().setColumnWidthZero(0,2);
    
    documentTableEditor.setAdministration(false);
    
    documentTableEditor.setSingleSelection(true);
    templateTableEditor.setSingleSelection(true);
    
    templateTableEditor.setAddFunction(true);
    
    templateEditor.setVisibleOkButton(false);
    documentTableEditor.setVisibleOkButton(false);
    templateTableEditor.setVisibleOkButton(false);
    
    rootSplit.add(leftSplit, JSplitPane.LEFT);
    rootSplit.add(templateEditor.getGUI(), JSplitPane.RIGHT);
    
    leftSplit.add(documentTableEditor.getGUI(), JSplitPane.TOP);
    leftSplit.add(templateTableEditor.getGUI(), JSplitPane.BOTTOM);
    
    templateEditor.setEnabled(false);
    templateEditor.setActive(false);
    
    documentTableEditor.getTable().setCellFontController((JTable table, int modelRow, int modelColumn, boolean isSelect, boolean hasFocus) -> {
      if((boolean)table.getModel().getValueAt(modelRow, 2))
        return new Font("Dialog", Font.BOLD|Font.ITALIC, 11);
      return null;
    });
  }

  @Override
  public void initTargets() {
  }
}