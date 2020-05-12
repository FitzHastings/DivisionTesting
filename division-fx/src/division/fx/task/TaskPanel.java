package division.fx.task;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class TaskPanel extends Pane {
  private final ObservableList<Task> tasks = FXCollections.observableArrayList();
  private final Label title = new Label();
  private final double dialogOpacity = 1;
  
  private final VBox dialog = new VBox(5);
  
  public TaskPanel() {
    getStyleClass().add("task-panel");
    title.getStyleClass().add("title-label-panel");
    title.textProperty().bind(Bindings.createStringBinding(() -> "предзаказы: "+tasks.size(), tasks));
    title.setOnMouseClicked(e -> {
      if(getChildren().contains(dialog))
        hideDialog(dialog);
      else show();
    });
    getChildren().add(title);
    dialog.layoutXProperty().bind(widthProperty().subtract(dialog.widthProperty()));
    dialog.layoutYProperty().bind(dialog.heightProperty().negate());
  }
  
  public ObservableList<Task> getTasks() {
    return tasks;
  }
  
  public void show() {
    initTaskListDialog();
    showDialog(dialog);
  }
  
  public void show(long id) {
    tasks.stream().filter(task -> task.getTaskId() == id).forEach(task -> show(task));
  }

  public void show(Task task) {
    initTaskDialog(task);
    showDialog(dialog);
  }
  
  private VBox initTaskListDialog() {
    dialog.getChildren().clear();
    dialog.getChildren().add(createTitleBox(dialog, "Список предзаказов"));
    dialog.setOpacity(0);
    dialog.getStyleClass().clear();
    dialog.getStyleClass().addAll("task-dialog","task-list-dialog");
    for(Task task:tasks)
      dialog.getChildren().add(createTaskItem(task));
    return dialog;
  }
  
  private HBox createTaskItem(Task task) {
    Label itemlabel = new Label(task.nameProperty().getValue());
    itemlabel.getStyleClass().add("item-task-label");
    itemlabel.setOnMouseClicked(e -> show(task));
    HBox itembox = new HBox(3, itemlabel);
    itembox.getStyleClass().add("item-box");
    return itembox;
  }
  
  private VBox initTaskDialog(Task task) {
    dialog.getChildren().clear();
    dialog.getChildren().addAll(createTitleBox(dialog, task), task);
    dialog.setOpacity(0);
    dialog.getStyleClass().clear();
    dialog.getStyleClass().add("task-dialog");
    return dialog;
  }
  
  private HBox createTitleBox(VBox dialog, String titleString) {
    Label titleLabel = new Label(titleString);
    titleLabel.getStyleClass().add("title-label");
    HBox titlebox = new HBox(5, titleLabel, creteConfirmLabel(dialog));
    titlebox.getStyleClass().add("title-box");
    HBox.setHgrow(titleLabel, Priority.ALWAYS);
    return titlebox;
  }
  
  private HBox createTitleBox(VBox dialog, Task task) {
    Label titleLabel = new Label(task.nameProperty().getValue());
    titleLabel.getStyleClass().add("title-label");
    HBox titlebox = new HBox(5, titleLabel, creteConfirmLabel(dialog), creteCloseLabel(dialog, task));
    titlebox.getStyleClass().add("title-box");
    HBox.setHgrow(titleLabel, Priority.ALWAYS);
    return titlebox;
  }
  
  private Label creteConfirmLabel(VBox dialog) {
    Label closeLabel = new Label("-");
    HBox.setHgrow(closeLabel, Priority.NEVER);
    closeLabel.getStyleClass().addAll("title-button","confirm-label");
    closeLabel.setOnMouseClicked(e -> hideDialog(dialog));
    return closeLabel;
  }
  
  private Label creteCloseLabel(VBox dialog, Task task) {
    Label closeLabel = new Label("x");
    HBox.setHgrow(closeLabel, Priority.NEVER);
    closeLabel.getStyleClass().addAll("title-button","close-label");
    closeLabel.setOnMouseClicked(e -> {
      hideDialog(dialog);
      tasks.remove(task);
    });
    return closeLabel;
  }
  
  private void showDialog(VBox dialog) {
    Timeline hideline = new Timeline(new KeyFrame(Duration.millis(dialog.getOpacity() == 0 ? 0 : 500), new KeyValue(dialog.opacityProperty(), dialog.getOpacity()), new KeyValue(dialog.opacityProperty(), 0)));
    hideline.setOnFinished(t -> {
      if(!getChildren().contains(dialog))
        getChildren().add(dialog);
      Timeline showline = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(dialog.opacityProperty(), 0), new KeyValue(dialog.opacityProperty(), dialogOpacity)));
      showline.play();
    });
    hideline.play();
  }
  
  private void hideDialog(VBox dialog) {
    hideDialog(dialog, null);
  }
  
  private void hideDialog(VBox dialog, EventHandler<ActionEvent> onfinished) {
    Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(dialog.opacityProperty(), dialog.getOpacity()), new KeyValue(dialog.opacityProperty(), 0)));
    timeline.setOnFinished(t -> {
      getChildren().remove(dialog);
      if(onfinished != null)
        onfinished.handle(t);
    });
    timeline.play();
  }
}