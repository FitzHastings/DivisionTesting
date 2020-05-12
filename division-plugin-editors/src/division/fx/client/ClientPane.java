package division.fx.client;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.Employee;
import division.fx.controller.company.FXCompany;
import division.fx.desktop.FXDesktopPane;
import division.fx.table.Column;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.editor.Storable;
import division.fx.PropertyMap;
import division.fx.table.filter.TextFilter;
import division.fx.FXUtility;
import division.fx.dialog.FXD;
import division.fx.util.MsgTrash;
import division.util.DNDUtil;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.input.MouseEvent;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import org.apache.commons.lang3.ArrayUtils;

public class ClientPane extends Pane implements Storable {
  private final ToolButton    filter    = new ToolButton("Бизнес-среда", -90);
  private final ToolBar       leftTool  = new ToolBar();
  
  private final SplitPane     split     = new SplitPane();
  private final FXDesktopPane desktop   = new FXDesktopPane();
  
  private final FXTreeEditor  CFCTable = new FXTreeEditor(CFC.class, null);
  private final FXTableEditor companyTable = new FXTableEditor(Company.class, FXCompany.class,
          FXCollections.observableArrayList("id"),
          Column.create("Наименование", "name", new TextFilter()),
          Column.create("ИНН", "inn", new TextFilter()));
  private final Label companyLabel = new Label("Предприятия");
  private final FXTableEditor partitionTable = new FXTableEditor(CompanyPartition.class, null,
          FXCollections.observableArrayList("id"),
          Column.create("Наименование","name", new TextFilter()),
          Column.create("kpp","kpp", new TextFilter()));
  private final Label partitionLabel = new Label("Подразделения");
  private final FXTableEditor employeeTable = new FXTableEditor(Employee.class, null,
          FXCollections.observableArrayList("id"),
          Column.create("Фамилия","people-surName", new TextFilter()),
          Column.create("Имя","people-name", new TextFilter()),
          Column.create("Отчество","people-lastName", new TextFilter()));
  private final Label employeeLabel = new Label("Персонал");
  
  private final SplitPane     cfcSplit      = new SplitPane();
  private final Accordion     accordion     = new Accordion();
  private final TitledPane    companyPane   = new TitledPane("", companyTable);
  private final TitledPane    partitionPane = new TitledPane("", partitionTable);
  private final TitledPane    employeePane  = new TitledPane("", employeeTable);

  public ClientPane() {
    FXUtility.initCss(this);
    initComponents();
    initEvents();
  }

  private void initComponents() {
    companyTable.setSelectionMode(SelectionMode.MULTIPLE);
    
    CFCTable.getTree().setShowRoot(true);
    CFCTable.titleProperty().setValue("Центры Финансового Учёта");
    
    CFCTable.setOnDrop(Company.class, e -> {
      PropertyMap cfc = CFCTable.getTreeItemAtPoint(e.getX(), e.getY()).getValue();
      ObservableList<PropertyMap> companyData  = PropertyMap.fromSimple((List<Map>)e.getDragboard().getContent(companyTable.getDragFormat()));
      if(!companyData.isEmpty()) {
        MenuItem copy = new MenuItem("Добавить в группу");
        MenuItem move = new MenuItem("Переместить в группу");

        move.setOnAction(a -> {
          try {
            for(PropertyMap p:companyData)
              ObjectLoader.saveObject(Company.class, p.setValue("cfcs", new Integer[]{cfc.getInteger("id")}));
          }catch(Exception ex) {
            MsgTrash.out(ex);
          }
        });

        copy.setOnAction(a -> {
          try {
            for(PropertyMap p:ObjectLoader.getList(Company.class, PropertyMap.getListFromList(companyData, "id", Integer.TYPE).toArray(new Integer[0]), "id","cfcs")) {
              p.setValue("cfcs", ArrayUtils.add(p.getValue("cfcs", Integer[].class), cfc.getInteger("id")));
              ObjectLoader.saveObject(Company.class, p);
            }
          }catch(Exception ex) {
            MsgTrash.out(ex);
          }
        });

        ContextMenu menu = new ContextMenu(copy, move);
        menu.show(CFCTable.getTree(), e.getScreenX(), e.getScreenY());
      }
    });
    
    CFCTable.setOnDrop(Contract.class, e -> {
      PropertyMap cfc = CFCTable.getTreeItemAtPoint(e.getX(), e.getY()).getValue();
      ObservableList<PropertyMap> contractData = PropertyMap.fromSimple((List<Map>)e.getDragboard().getContent(DNDUtil.getDragFormat(Contract.class)));
      if(!contractData.isEmpty()) {
        FXD.show(type -> {
          try {
            if(type == FXD.ButtonType.OK)
              ObjectLoader.executeUpdate(Contract.class, "sellerCfc", cfc.getInteger("id"), PropertyMap.getListFromList(contractData, "id", Integer.TYPE).toArray(new Integer[0]));
          }catch(Exception ex) {
            MsgTrash.out(ex);
          }finally {
            return true;
          }
        },"Внимание!!!", "Смена исполнителя по договору повлечёт за собой\nсмену исполнителя по всем сделкам этих договоров начиная с сегодняшней даты.", FXD.ButtonType.OK, FXD.ButtonType.CANCEL);
      }
    });
    
    leftTool.getItems().add(filter);
    leftTool.setOrientation(Orientation.VERTICAL);
    
    companyTable.initData();
    CFCTable.initData();
    
    //CFCTable.getTree().setShowRoot(false);
    
    companyLabel.setMinWidth(0);
    companyLabel.prefWidthProperty().bind(companyPane.widthProperty().subtract(50));
    partitionLabel.setMinWidth(0);
    partitionLabel.prefWidthProperty().bind(companyPane.widthProperty().subtract(50));
    
    companyPane.setGraphic(companyLabel);
    partitionPane.setGraphic(partitionLabel);
    employeePane.setGraphic(employeeLabel);
    
    accordion.setMinWidth(0);
    companyPane.setMinWidth(0);
    partitionPane.setMinWidth(0);
    
    accordion.getPanes().addAll(companyPane, partitionPane, employeePane);
    accordion.setExpandedPane(companyPane);
    
    cfcSplit.getItems().addAll(CFCTable, accordion);
    
    cfcSplit.setOrientation(Orientation.VERTICAL);
    filter.getRootPopPane().setCenter(cfcSplit);
    
    split.getItems().add(desktop);
    
    getChildren().addAll(split, leftTool);
    leftTool.setLayoutX(0);
    leftTool.setLayoutY(0);
    leftTool.prefHeightProperty().bind(prefHeightProperty());
    leftTool.minHeightProperty().bind(minHeightProperty());
    leftTool.maxHeightProperty().bind(maxHeightProperty());
    
    /*heightProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        System.out.println(leftTool.getPrefHeight()+" <> "+Client.getRootPane().getHeight());
      }
    });*/
    
    //System.out.println(leftTool.getPrefHeight()+" <> "+Client.getRootPane().getWidth());
    
    split.layoutXProperty().bind(leftTool.widthProperty());
    split.setLayoutY(0);
    split.layoutXProperty().bind(leftTool.layoutXProperty().add(leftTool.widthProperty()));
    split.prefWidthProperty().bind(widthProperty().subtract(split.layoutXProperty()));
    split.prefHeightProperty().bind(heightProperty());
  }

  private void initEvents() {
    leftTool.getItems().addListener((ListChangeListener.Change<? extends Node> c) -> {
      leftTool.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty"), leftTool.getItems().isEmpty());
    });
    
    filter.getRootPopPane().prefHeightProperty().bind(desktop.heightProperty());
    
    filter.getRootPopPane().fixProperty().addListener((ObservableValue<? extends Boolean> ob, Boolean ol, Boolean nw) -> {
      filter.selectedProperty().set(false);
      if(nw) {
        split.getItems().add(0, filter.getRootPopPane());
        leftTool.setLayoutX(-1*leftTool.getWidth());
        split.setDividerPosition(0, (filter.getRootPopPane().getWidth()+leftTool.getWidth())/split.getWidth());
      }else {
        leftTool.setLayoutX(0);
        split.getItems().remove(0);
      }
    });
    
    addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent event) -> {
      if(filter.getRootPopPane().getBoundsInLocal() != null && 
              filter.getRootPopPane().localToScreen(filter.getRootPopPane().getBoundsInLocal()) != null &&
              !filter.getRootPopPane().localToScreen(filter.getRootPopPane().getBoundsInLocal()).contains(event.getScreenX(), event.getScreenY()) &&
              filter.localToScreen(filter.getBoundsInLocal()) != null &&
              !filter.localToScreen(filter.getBoundsInLocal()).contains(event.getScreenX(), event.getScreenY()))
        filter.selectedProperty().set(false);
    });
    
    filter.selectedProperty().addListener(new ChangeListener<Boolean>() {
      Pane popContent = new Pane();
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        if(newValue) {
          popContent.setOpacity(0);
          popContent.getChildren().add(filter.getRootPopPane());
          popContent.setLayoutX(leftTool.getWidth());
          popContent.setLayoutY(leftTool.getLayoutY());
          getChildren().add(popContent);
          new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(popContent.opacityProperty(), 1))).play();
        }else {
          popContent.setOpacity(1);
          Timeline timline = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(popContent.opacityProperty(), 0)));
          timline.setOnFinished((ActionEvent event) -> {
            getChildren().remove(popContent);
            popContent.getChildren().remove(filter.getRootPopPane());
          });
          timline.play();
        }
      }
    });
    
    CFCTable.selectedItemProperty().addListener((ob, ol, nw) -> {
      companyTable.getClientFilter().clear();
      if(nw != null) {
        Integer[] ids = new Integer[0];
        for(PropertyMap tp:CFCTable.getSubObjects(nw))
          ids = ArrayUtils.add(ids, (Integer)tp.getValue("id"));
        companyTable.getClientFilter().AND_IN("cfcs", ids);
      }
      companyTable.initData();
    });
    
    companyTable.selectedItemProperty().addListener((ob, ol, nw) -> {
      partitionTable.getClientFilter().clear();
      partitionTable.getClientFilter().AND_EQUAL("company", nw == null ? null : nw.getValue("id"));
      partitionTable.initData();
    });
    
    partitionTable.selectedItemProperty().addListener((ob, ol, nw) -> {
      employeeTable.getClientFilter().clear();
      employeeTable.getClientFilter().AND_EQUAL("partition", nw == null ? null : nw.getValue("id"));
      employeeTable.initData();
    });
    
    companyPane.setAnimated(true);
    partitionPane.setAnimated(true);
    
    companyTable.selectedItemProperty().addListener((ob, ol, nw)   -> companyLabel  .setText(nw == null ? "Предприятия"   : nw.getValue("name", "Предприятия")));
    partitionTable.selectedItemProperty().addListener((ob, ol, nw) -> partitionLabel.setText(nw == null ? "Подразделения" : nw.getValue("name", "Подразделения")));
    employeeTable.selectedItemProperty().addListener((ob, ol, nw)  -> partitionLabel.setText(nw == null ? "Персонал"      : nw.getValue("name", "Персонал")));
  }

  public FXDesktopPane getDesktop() {
    return desktop;
  }

  public FXTreeEditor getCFCTable() {
    return CFCTable;
  }

  public FXTableEditor getCompanyTable() {
    return companyTable;
  }

  public FXTableEditor getPartitionTable() {
    return partitionTable;
  }

  public FXTableEditor getEmployeeTable() {
    return employeeTable;
  }

  @Override
  public ObservableList<Node> storeControls() {
    return FXCollections.observableArrayList(CFCTable, companyTable, partitionTable, employeeTable);
  }
}