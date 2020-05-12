package division.demons;

import TextFieldLabel.TextFieldLabel;
import bum.editors.util.DivisionTarget;
import bum.editors.util.ObjectLoader;
import bum.interfaces.CFC;
import bum.interfaces.Comment;
import bum.interfaces.Company;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Equipment;
import bum.interfaces.Group;
import bum.interfaces.Request;
import bum.interfaces.Store;
import bum.interfaces.Worker;
import division.fx.PropertyMap;
import division.fx.dialog.FXD;
import division.fx.editor.FXEditor;
import division.fx.editor.FXObjectEditor;
import division.fx.editor.FXTableEditor;
import division.fx.editor.FXTreeEditor;
import division.fx.table.Column;
import division.fx.table.FXDivisionTable;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.fx.util.MsgTrash;
import division.util.Utility;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class FXRequest extends FXObjectEditor {
  private final TextFieldLabel<String> numberLabel = new TextFieldLabel<>("Номер заявки");
  private final Label    applicant = new Label();
  private final Label    performer = new Label();
  private final TextArea reason = new TextArea();
  private final VBox chat   = new VBox();
  private final ScrollPane chatScroll = new ScrollPane(chat);
  private final TextArea commentTextArea = new TextArea();
  private final Button sendComment = new Button("отправить");
  
  private final FXDivisionTable<PropertyMap> equipments = new FXDivisionTable<>(
          Column.create("Объект"          , "group_name", new ListFilter("group_name")),
          Column.create("Заводской номер" , "identity_value_name", new TextFilter("identity_value_name")),
          Column.create("Адрес"           , "address", new TextFilter("address")),
          Column.create("выбрать"));
  
  private final FXTreeEditor  сfсTable    = new FXTreeEditor(CFC.class);
  private final FXTableEditor workerTable = new FXTableEditor(Worker.class, null, FXCollections.observableArrayList("cfc"), Column.create("Фамилия", "people_surname"), Column.create("Имя", "people_name"), Column.create("Отчество", "people_lastname"));
  
  private final Label    start   = new Label("Заявлено");
  private final CheckBox accept  = new CheckBox("Принято");
  private final Label    execute = new Label();
  private final CheckBox exit    = new CheckBox("Отклонено");
  private final CheckBox finish  = new CheckBox("Исполнено");
  private final HBox   statusbar = new HBox(start, accept, execute, exit, finish);
  
  public FXRequest() {
    super(Request.class);
    
    сfсTable.selectedItemProperty().addListener((ObservableValue<? extends TreeItem<PropertyMap>> observable, TreeItem<PropertyMap> oldValue, TreeItem<PropertyMap> newValue) -> {
      workerTable.getClientFilter().clear();
      if(newValue != null && !newValue.getValue().isNull("id"))
        workerTable.getClientFilter().AND_EQUAL("cfc", newValue.getValue().getInteger("id"));
      workerTable.initData();
    });

    workerTable.addInitDataListener(ev -> {
      Integer selectId = getObjectProperty().isNull("performerWorker") ? ObjectLoader.getClient().getWorkerId() : getObjectProperty().getInteger("performerWorker");
      workerTable.getTable().getItems().stream().filter(it -> Objects.equals(it.getInteger("id"), selectId)).forEach(it -> {
        workerTable.getTable().getSelectionModel().select(it);
      });
    });

    сfсTable.addInitDataListener(ev -> сfсTable.getTree().getSelectionModel().select(сfсTable.getNodeObject(getObjectProperty().isNull("performer") ? ObjectLoader.getClient().getCfcId() : getObjectProperty().getInteger("performer"))));

    workerTable.setDoubleClickActionType(FXEditor.DoubleClickActionType.SELECT);
    workerTable.setSelectionMode(SelectionMode.SINGLE);
    сfсTable.setSelectionMode(SelectionMode.SINGLE);
    
    
    
    
    applicant.setOnMouseClicked(e -> {
      FXTableEditor companyTable = new FXTableEditor(Company.class,null,
              Column.create("Наименование", "name=query:getCompanyName([Company(id)])",new TextFilter("name")),
              Column.create("ИНН", "inn",new TextFilter()));
      companyTable.setDoubleClickActionType(FXEditor.DoubleClickActionType.SELECT);
      companyTable.setSelectionMode(SelectionMode.SINGLE);
      companyTable.getObjects(this.getRoot(), "Выберите заявителя").stream().forEach(c -> getObjectProperty().setValue("applicant", c.getInteger("id")).setValue("applicant-name",c.getString("name")));
    });
    
    performer.setOnMouseClicked(e -> {
      сfсTable.initData();
      if(FXD.showWait("Выбор исполнителя", this.getRoot(), new SplitPane(сfсTable, workerTable), FXD.ButtonType.OK).orElseGet(() -> FXD.ButtonType.CANCEL) == FXD.ButtonType.OK && !сfсTable.getSelectedObjects().isEmpty() && !workerTable.getSelectedObjects().isEmpty()) {
        getObjectProperty()
                .setValue("performer", workerTable.getSelectedObjects().get(0).getInteger("cfc"))
                .setValue("performerWorker", workerTable.getSelectedIds()[0])
                .setValue("performer-name", сfсTable.getSelectedObjects().get(0).getString("name")+": "+workerTable.getSelectedObjects().get(0).getString("people_surname")+" "+workerTable.getSelectedObjects().get(0).getString("people_name")+" "+workerTable.getSelectedObjects().get(0).getString("people_lastname"));
      }
    });
    
    reason.setPromptText("Причина обращения...");
    
    commentTextArea.setPromptText("Введите комментарий...");
    
    sendComment.setOnAction(e -> {
      if(!commentTextArea.getText().equals("")) {
        if(getObjectProperty().isNotNull("id"))
          createComment();
        else save();
      }
    });
    
    DivisionTarget.create(this, Comment.class, (DivisionTarget target, String type, Integer[] ids, PropertyMap data) -> {
      Platform.runLater(() -> {
        ObjectLoader.getList(DBFilter.create(Comment.class)
                .AND_EQUAL("objectClass", Request.class.getSimpleName())
                .AND_EQUAL("objectId", getObjectProperty().getInteger("id"))
                .AND_IN("id", ids)).stream().forEach(comment -> {
                  switch(type) {
                    case "CREATE":
                      addComment(comment);
                      ObjectLoader.sendMessage(Request.class, "UPDATE", comment.getInteger("id"));
                      break;
                  }
        });
      });
    });
    
    accept.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> getObjectProperty().setValue("acceptDate", newValue ? "CURRENT_TIMESTAMP" : null));
    
    exit.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      getObjectProperty().setValue("exitDate", newValue ? "CURRENT_TIMESTAMP" : null);
      if(newValue)
        finish.setSelected(false);
    });
   
    finish.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
      getObjectProperty().setValue("finishDate", newValue ? "CURRENT_TIMESTAMP" : null);
      if(newValue)
        exit.setSelected(false);
    });
    
    SplitPane split = new SplitPane(new VBox(5, new Label("Объекты заявки"), equipments), new VBox(5, reason, chatScroll));
    split.setOrientation(Orientation.VERTICAL);
    
    
    storeControls().add(split);
    
    HBox b = new HBox(numberLabel);
    b.setAlignment(Pos.CENTER_RIGHT);
    
    getRoot().setCenter(new VBox(5, 
            b,
            applicant,
            split, 
            new HBox(5, commentTextArea, sendComment)
    ));
    getRoot().setBottom(new VBox(5, statusbar, performer));
    
    chat.prefWidthProperty().bind(chatScroll.widthProperty().subtract(20));
    HBox.setHgrow(commentTextArea, Priority.ALWAYS);
    VBox.setVgrow(split, Priority.ALWAYS);
    VBox.setVgrow(equipments, Priority.ALWAYS);
    VBox.setVgrow(chatScroll, Priority.NEVER);
    
    commentTextArea.setPrefHeight(50);
    
    commentTextArea.setOnKeyReleased(e -> {
      if(e.isControlDown() && e.getCode() == KeyCode.ENTER)
        sendComment.fire();
    });
  }
  
  @Override
  public void initData() {
    storeControls().add(equipments);
    
    numberLabel.valueProperty().setValue(getObjectProperty().getString("number"));
    getObjectProperty().get("number").bind(numberLabel.valueProperty());
    
    start.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("startDate")  || !(getObjectProperty().getValue("startDate")  instanceof LocalDateTime) ? ""          : ("Заявлено\n"  +Utility.format(getObjectProperty().getLocalDateTime("startDate"))), 
            objectProperty(), getObjectProperty().get("startDate")));
    
    accept.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("acceptDate") || !(getObjectProperty().getValue("acceptDate") instanceof LocalDateTime) ? "Принято"   : ("Принято\n"   +Utility.format(getObjectProperty().getLocalDateTime("acceptDate"))), 
            objectProperty(), getObjectProperty().get("acceptDate")));
    
    execute.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("executDate") || !(getObjectProperty().getValue("executDate") instanceof LocalDateTime) ? ""          : ("В работе\n"  +Utility.format(getObjectProperty().getLocalDateTime("executDate"))), 
            objectProperty(), getObjectProperty().get("executDate")));
    
    exit.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("exitDate")   || !(getObjectProperty().getValue("exitDate")   instanceof LocalDateTime) ? "Отклонено" : ("Отклонено\n" +Utility.format(getObjectProperty().getLocalDateTime("exitDate"))), 
            objectProperty(), getObjectProperty().get("exitDate")));
    
    finish.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("finishDate") || !(getObjectProperty().getValue("finishDate") instanceof LocalDateTime) ? "Исполнено" : ("Исполнено\n" +Utility.format(getObjectProperty().getLocalDateTime("finishDate"))), 
            objectProperty(), getObjectProperty().get("finishDate")));
    
    finish.disableProperty().bind(Bindings.createBooleanBinding(() -> getObjectProperty().isNull("performer"), objectProperty(), getObjectProperty().get("performer")));
    accept.disableProperty().bind(Bindings.createBooleanBinding(() -> getObjectProperty().isNotNull("performer"), objectProperty(), getObjectProperty().get("performer")));
    
    accept.setSelected(!getObjectProperty().isNull("acceptDate"));
    exit.setSelected(!getObjectProperty().isNull("exitDate"));
    finish.setSelected(!getObjectProperty().isNull("finishDate"));
    reason.setText(getObjectProperty().getString("reason"));
    
    applicant.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("applicant-name") ? "Заявитель:________________" : "Заявитель: "+getObjectProperty().getString("applicant-name"), 
            objectProperty(), getObjectProperty().get("applicant-name")));
    
    performer.textProperty().bind(Bindings.createStringBinding(() -> 
            getObjectProperty().isNull("performer-name") ? "Исполнитель:________________" : "Исполнитель: "+getObjectProperty().getString("performer-name"), 
            objectProperty(), getObjectProperty().get("performer-name")));
    
    getObjectProperty().get("reason").bind(reason.textProperty());
    
    getObjectProperty().get("applicant").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      equipments.clear();
      if(newValue != null)
        fillEquipmentTable();
    });
    
    getObjectProperty().get("performer").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
      if(getObjectProperty().isNull("executDate"))
        getObjectProperty().setValue("executDate", newValue == null ? null : "CURRENT_TIMESTAMP");
      if(getObjectProperty().isNull("acceptDate"))
        accept.setSelected(newValue != null);
    });
    
    fillEquipmentTable();
    try {
      ObjectLoader.getList(DBFilter.create(Comment.class).AND_EQUAL("objectClass", Request.class.getSimpleName()).AND_EQUAL("objectId", getObjectProperty().getInteger("id"))).stream().forEach(comment -> 
        Platform.runLater(() -> addComment(comment)));
    }catch (Exception ex) {
      MsgTrash.out(ex);
    }
    
    getMementoProperty().copyFrom(getObjectProperty().copyFrom(ObjectLoader.getMap(Request.class, getObjectProperty().getInteger("id"), 
            "applicant-name=query:getCompanyName([Request(applicant)])",
            "performer-name=query:SELECT [Worker(cfc_name)]||': '||[Worker(people_surname)]||' '||[Worker(people_name)]||' '||[Worker(people_lastname)] FROM [Worker] WHERE id=[Request(performerWorker)]")));
  }
  
  private void fillEquipmentTable() {
    try {
      equipments.clear();
      ObservableList<PropertyMap> list = ObjectLoader.getList(DBFilter.create(Equipment.class).AND_IN("store", 
            ObjectLoader.getList(DBFilter.create(Store.class).AND_IN("companyPartition", 
                    ObjectLoader.getList(DBFilter.create(CompanyPartition.class).AND_EQUAL("company", 
                            getObjectProperty().getInteger("applicant")), "id")).AND_EQUAL("objectType", Group.ObjectType.ТМЦ).AND_EQUAL("storeType", Store.StoreType.НАЛИЧНЫЙ), "id")),
            "id",
            "group_name",
            "identity_value_name",
            "address=query:(SELECT name FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND [EquipmentFactorValue(factor)]=(SELECT id FROM [Factor] WHERE name ilike '%адрес установки%'))",
            "выбрать=query:[Equipment(id)] = ANY(ARRAY["+Utility.join(getObjectProperty().getValue("equipments", Integer[].class), ",")+"]::integer[])",
            "sort:выбрать DESC, address");
      list.stream().forEach(equip -> {
        equip.get("выбрать").addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
          if((boolean)newValue)
            getObjectProperty().setValue("equipments", ArrayUtils.add(getObjectProperty().getValue("equipments", Integer[].class), equip.getInteger("id")));
          else getObjectProperty().setValue("equipments", ArrayUtils.removeElement(getObjectProperty().getValue("equipments", Integer[].class), equip.getInteger("id")));
        });
      });
      equipments.getSourceItems().addAll(list);
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }
  
  private PropertyMap createComment() {
    PropertyMap comment = PropertyMap.create()
            .setValue("objectClass", Request.class.getSimpleName())
            .setValue("objectId", getObjectProperty().getInteger("id"))
            .setValue("text", commentTextArea.getText())
            .setValue("type", Comment.Type.PROJECT)
            .setValue("author", Worker.class.getSimpleName()+":"+ObjectLoader.getClient().getWorkerId());
    comment.setValue("id", ObjectLoader.createObject(Comment.class, comment));
    if(!comment.isNull("id"))
      commentTextArea.setText("");
    return comment;
  }
  
  ExecutorService pool = Executors.newSingleThreadExecutor();
  
  public static boolean isOurComment(PropertyMap comment) {
    return comment.getString("author").startsWith(Worker.class.getSimpleName());
  }
  
  public static boolean isMyComment(PropertyMap comment) {
    return comment.getString("author").equals(Worker.class.getSimpleName()+":"+ObjectLoader.getClient().getWorkerId());
  }
  
  private void addComment(PropertyMap commentValue) {
    if(!isOurComment(commentValue) && commentValue.getValue("type", Comment.Type.class) == MappingObject.Type.PROJECT) {
      try {
        ObjectLoader.saveObject(Comment.class, commentValue.setValue("type", Comment.Type.CURRENT));
      }catch(Exception ex) {
        MsgTrash.out(ex);
      }
    }
    
    
    String prefix = isOurComment(commentValue) ? "my" : "client";
    
    Label commentDate = new Label(Utility.format(commentValue.getTimestamp("date")));
    commentDate.getStyleClass().addAll(prefix+"-comment-date","comment-date");

    VBox comment = new VBox(new Label(commentValue.getString("text")));
    comment.getStyleClass().addAll(prefix+"-comment","request-comment");

    VBox commentBox = new VBox(commentDate, comment);
    commentBox.getStyleClass().addAll(prefix+"-comment-box","comment-box");

    chat.getChildren().addAll(commentBox);
    
    pool.submit(() -> {
      try {
        Thread.sleep(100);
      }catch (InterruptedException ex) {}
      Platform.runLater(() -> {
        chatScroll.setVvalue(1.0f);
      });
    });
  }

  @Override
  public boolean isUpdate() {
    return super.isUpdate() || (!getObjectProperty().isNull("id") && !commentTextArea.getText().equals(""));
  }

  @Override
  public boolean save(boolean tmp, String... fields) {
    boolean is = super.save(tmp, fields);
    if(is && !commentTextArea.getText().equals(""))
      createComment();
    return is;
  }

  @Override
  public String validate() {
    String msg = "";
    if(getObjectProperty().isNull("applicant"))
      msg += "  не выбран заявитель\n";
    if(getObjectProperty().isNullOrEmpty("reason"))
      msg += "  не указана причина заявки\n";
    
    if(msg.equals("")) {
      if(getObjectProperty().isNull("startDate"))
        getObjectProperty().setValue("startDate", "CURRENT_TIMESTAMP");
    }
    
    return msg;
  }
}