package division.fx.task;

import division.fx.PropertyMap;
import division.util.DNDUtil;
import division.util.IDStore;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;

public class Task extends BorderPane {
  private final Long id = IDStore.createID();
  private final StringProperty nameProperty = new SimpleStringProperty();
  private final StringProperty descProperty = new SimpleStringProperty();
  private final PropertyMap taskData = PropertyMap.create();
  
  private HashMap<DataFormat, EventHandler<DragEvent>> dragHandlers = new HashMap<>();
  
  public Task(String name) {
    this(name, "");
    initDragAndDrop();
    getStyleClass().add("task-box");
  }
  
  public Collection<DataFormat> getAcceptFormats() {
    return dragHandlers.keySet();
  }
  
  public void initDragAndDrop() {
    setOnDragDropped(e -> {
      boolean transferDone = false;
      for(DataFormat format:dragHandlers.keySet()) {
        if(e.getDragboard().hasContent(format)) {
          transferDone = true;
          dragHandlers.get(format).handle(e);
        }
      }
      e.setDropCompleted(transferDone);
    });
    
    setOnDragOver(e -> {
      if(!getDataFormatAccess(e).isEmpty())
        e.acceptTransferModes(TransferMode.ANY);
      else e.acceptTransferModes(TransferMode.NONE);
    });
  }
  
  public void setOnDrop(Class dragClass, EventHandler<DragEvent> e) {
    setOnDrop(DNDUtil.getDragFormat(dragClass), e);
  }
  
  public void setOnDrop(DataFormat format, EventHandler<DragEvent> e) {
    dragHandlers.put(format, e);
  }
  
  public void removeDrop(DataFormat format) {
    dragHandlers.remove(format);
  }
  
  public Collection<DataFormat> getDataFormatAccess(DragEvent e) {
    Set<DataFormat> types = e.getDragboard().getContentTypes();
    types.retainAll(getAcceptFormats());
    return types;
  }
  
  
  public Task(String name, String description) {
    nameProperty.setValue(name);
    descProperty.setValue(description);
  }

  public Long getTaskId() {
    return id;
  }

  public StringProperty nameProperty() {
    return nameProperty;
  }

  public StringProperty descProperty() {
    return descProperty;
  }

  public PropertyMap getTaskData() {
    return taskData;
  }
}