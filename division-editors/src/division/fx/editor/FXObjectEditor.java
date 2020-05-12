package division.fx.editor;

import bum.editors.util.ObjectLoader;
import division.fx.PropertyMap;
import division.fx.FXUtility;
import division.fx.dialog.FXD;
import division.fx.gui.FXDisposable;
import division.fx.gui.FXStorable;
import division.fx.util.MsgTrash;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import mapping.MappingObject;
import org.apache.log4j.Logger;
import util.filter.local.DBFilter;

public abstract class FXObjectEditor implements Initializable, FXStorable, FXDisposable {
  private final BooleanProperty saveable = new SimpleBooleanProperty(true);
  private Class<? extends MappingObject> objectClass;
  private ObjectProperty<PropertyMap> objectProperty = new SimpleObjectProperty<>();
  private PropertyMap   mementoProperty = PropertyMap.create();
  private final StringProperty title = new SimpleStringProperty();
  private BorderPane root      = new BorderPane();
  private boolean result = true;
  private final ObservableList storeControls = FXCollections.observableArrayList();
  private List<FXDisposable> disposables = FXCollections.observableArrayList();
  
  private DBFilter filter;

  public FXObjectEditor() {
    this(null);
  }
  
  public FXObjectEditor(Class<? extends MappingObject> objectClass) {
    FXUtility.initMainCss(this);
    this.objectClass = objectClass;
    
    Parent panel = loadRootPane();
    if(panel != null)
      getRoot().setCenter(panel);
    else initialize(null, null);
    storeControls.add(root);
  }

  public DBFilter getFilter() {
    return filter;
  }

  public void setFilter(DBFilter filter) {
    this.filter = filter;
  }

  public StringProperty titleProperty() {
    return title;
  }
  
  public void setTitle(String title) {
    titleProperty().setValue(title);
  }
  
  public BooleanProperty saveableProperty() {
    return saveable;
  }
  
  @Override
  public List<FXDisposable> disposeList() {
    return disposables;
  }
  
  @Override
  public void finaly() {
  }
  
  public void close(WindowEvent e) {
    //store();
    if(isUpdate()) {
      FXD.ButtonType type = FXD.showWait("Закрыть", root, "Сохранить изменения?", FXD.ButtonType.YES, FXD.ButtonType.NO, FXD.ButtonType.CANCEL).orElseGet(() -> FXD.ButtonType.CANCEL);
      if(type == FXD.ButtonType.YES && !save() || type == FXD.ButtonType.CANCEL) {
        e.consume();
      }else result = type != FXD.ButtonType.NO;
    }else result = false;
  }
  
  public Window getDialog() {
    return root.getScene().getWindow();
  }
  
  public BorderPane getRoot() {
    return root;
  }

  public Class<? extends MappingObject> getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(Class<? extends MappingObject> objectClass) {
    this.objectClass = objectClass;
  }
  
  public FXObjectEditor setObjectProperty(PropertyMap objectProperty) {
    this.objectProperty.setValue(objectProperty);
    mementoProperty = PropertyMap.copy(objectProperty);
    mementoProperty.printDifferenceProperty().setValue(true);
    initData();
    return this;
  }
  
  public Parent loadRootPane() {
    try {
      String fxml = getFxmlPath();
      FXMLLoader loader;
      if(fxml == null)
        loader = FXUtility.getLoader(this);
      else loader = FXUtility.getLoader(fxml);
      loader.setController(this);
      Parent p = loader.load();
      loadFinish();
      return p;
    }catch(Exception ex) {
      Logger.getLogger(getClass()).warn(ex);
    }
    return null;
  }
  
  public void loadFinish() {}
  public abstract void initData();
  public void beforeShow() {};

  @Override
  public void initialize(URL location, ResourceBundle resources) {
  }
  
  
  public String getFxmlPath() {
    return null;
  }
  
  public String getCssPath() {
    return null;
  }

  @Override
  public void dispose() {
    store();
    FXDisposable.super.dispose();
    root = null;
  }
  
  public boolean isUpdate() {
    return getObjectProperty().is("tmp") || !mementoProperty.equals(getObjectProperty());
  }
  
  public String validate() {
    return null;
  }
  
  public boolean save() {
    return save(new String[0]);
  }
  
  public boolean save(String... fields) {
    return save(false, fields);
  }
  
  public boolean save(boolean tmp) {
    return save(tmp, new String[0]);
  }
  
  public boolean save(boolean tmp,String... fields) {
    String msg = validate();
    if(msg != null && !"".equals(msg)) {
      FXD.showWait("Ошибка!", getRoot(), msg, FXD.ButtonType.OK);
      return false;
    }else {
      if(saveable.getValue()) {
        try {
          getObjectProperty().setValue("tmp", tmp);
          if(getObjectProperty().isNullOrEmpty("id")) {
            getObjectProperty().setValue("id", ObjectLoader.createObject(
                    objectClass, 
                    fields == null || fields.length == 0 ? getObjectProperty() : PropertyMap.copy(getObjectProperty().getSimpleMap(fields)), 
                    true));
            setMementoProperty(getObjectProperty().copy());
            return true;
          }else {
            if(ObjectLoader.saveObject(
                    objectClass, 
                    fields == null || fields.length == 0 ? getObjectProperty() : PropertyMap.copy(getObjectProperty().getSimpleMap(fields)), 
                    true)) {
              setMementoProperty(getObjectProperty().copy());
              return true;
            }else return false;
          }
        }catch(Exception ex) {
          MsgTrash.out(ex);
          return false;
        }
      }else return true;
    }
  }
  
  public ReadOnlyObjectProperty<PropertyMap> objectProperty() {
    return objectProperty;
  }

  public PropertyMap getObjectProperty() {
    return objectProperty.getValue();
  }

  public PropertyMap getMementoProperty() {
    return mementoProperty;
  }

  public void setMementoProperty(PropertyMap mementoProperty) {
    this.mementoProperty = mementoProperty;
  }

  @Override
  public ObservableList storeControls() {
    return storeControls;
  }
  
  public boolean showAndWait() {
    return showAndWait(null);
  }

  public boolean showAndWait(Node owner) {
    FXD fxd = FXD.create((FXD.ButtonType t) -> {
        return t == FXD.ButtonType.OK && (!isUpdate() || save());
    }, titleProperty().getValue(), owner, getRoot(), Modality.NONE, StageStyle.DECORATED, true, FXD.ButtonType.OK);
    
    fxd.addEventHandler(WindowEvent.ANY, e -> {
      dialogEvent(e);
      if(e.getEventType() == WindowEvent.WINDOW_HIDING)
        dispose();
      if(e.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
        close(e);
      }
    });
    
    load();
    beforeShow();
    fxd.showDialog();
    return result;
  }

  public void dialogEvent(WindowEvent event) {
  }
  
  public PropertyMap get() {
    return this.get((Node)null);
  }

  public PropertyMap get(Node parent) {
    showAndWait(parent);
    return getObjectProperty();
  }
  
  public static PropertyMap get(Class<? extends FXObjectEditor> editorClass) {
    return get(editorClass, PropertyMap.create());
  }
  
  public static PropertyMap get(Class<? extends FXObjectEditor> editorClass, PropertyMap objectProperty) {
    return get(editorClass, null, objectProperty);
  }
  
  public static PropertyMap get(Class<? extends FXObjectEditor> editorClass, Node parent, PropertyMap objectProperty) {
    try {
      FXObjectEditor editor = editorClass.newInstance().setObjectProperty(objectProperty);
      if(editor.showAndWait(parent))
        return editor.getObjectProperty();
    }catch (Exception ex) {
      MsgTrash.out(ex);
    }
    return null;
  }
}