package division.fx.client.plugins.deal.test;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Comment;
import bum.interfaces.People;
import division.fx.PropertyMap;
import division.fx.util.MsgTrash;
import division.util.DivisionTask;
import division.util.Utility;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class FXCommentTable extends VBox {
  private final Class<? extends MappingObject> objectClass;
  private final IntegerProperty objectIdProperty = new SimpleIntegerProperty(0);
  
  private final VBox       commentsList       = new VBox();
  private final ScrollPane commentsListScroll = new ScrollPane(commentsList);
  
  private final TextArea   commentEditor      = new TextArea();
  private final Button     commentButton      = new Button();
  private final HBox       commentEditBox     = new HBox(commentEditor,commentButton);

  public FXCommentTable(Class<? extends MappingObject> objectClass) {
    this.objectClass = objectClass;
    getStyleClass().add("comments-box");
    commentsList.prefWidthProperty().bind(commentsListScroll.widthProperty().subtract(20));
    commentsList.getStyleClass().add("comments-list");
    commentEditBox.getStyleClass().add("comment-edit-box");
    commentsListScroll.getStyleClass().add("comments-list-scroll");
    commentEditor.getStyleClass().add("comment-editor");
    commentButton.getStyleClass().add("comment-button");
    getChildren().addAll(commentsListScroll, commentEditBox);
    objectIdProperty.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> initData());
    
    commentsList.minHeightProperty().bind(commentsListScroll.heightProperty());
    
    commentEditor.setPromptText("Введите комментарий...");
    
    commentEditor.setOnKeyPressed(e -> {
      if(e.isControlDown() && e.getCode() == KeyCode.ENTER)
        commentButton.fire();
    });
    
    commentButton.disableProperty().bind(commentEditor.textProperty().isEmpty().or(objectIdProperty.lessThanOrEqualTo(0)));
    commentEditor.disableProperty().bind(objectIdProperty.lessThanOrEqualTo(0));
    
    commentButton.setOnAction(e -> {
      if(!commentEditor.getText().equals("")) {
        try {
          PropertyMap comment = PropertyMap.create()
                  .setValue("objectClass", objectClass.getName())
                  .setValue("objectId", objectIdProperty.getValue())
                  .setValue("text", commentEditor.getText())
                  .setValue("date", new Timestamp(System.currentTimeMillis()));
          comment.setValue("id", ObjectLoader.createObject(bum.interfaces.Comment.class, comment));
          commentEditor.setText("");
          initData();
        } catch (Exception ex) {
          MsgTrash.out(ex);
        }
      }
    });
  }
  
  public void initData() {
    try {
      commentsList.getChildren().clear();
      if(objectClass != null && objectIdProperty.getValue() != null) {
        ObjectLoader.getList(DBFilter.create(bum.interfaces.Comment.class)
                .AND_EQUAL("objectClass", objectClass.getName())
                .AND_EQUAL("objectId", objectIdProperty.getValue()), 
                "id",
                "text",
                "subject:=:name",
                "date",
                "modificationDate",
                "avtor-name=query:(SELECT [Worker(people_surname)]||' '||[Worker(people_name)]||' '||[Worker(people_lastname)] FROM [Worker] WHERE id=[Comment(lastUserId)])",
                "controlDateTime").stream()
                .sorted((PropertyMap o1, PropertyMap o2) -> o1.getTimestamp("date").compareTo(o2.getTimestamp("date")))
                .forEach(comment -> {
                  addComment(comment);
                });
      }
    } catch (Exception ex) {
      MsgTrash.out(ex);
    }
  }
  
  private void addComment(PropertyMap comment) {
    Comment c = new Comment(comment);
    HBox commentRow = new HBox(c);
    commentRow.getStyleClass().add("comment-list-row");
    commentsList.getChildren().add(commentRow);
    commentsListScroll.setViewportBounds(c.getBoundsInLocal());
    
    DivisionTask.start(() -> {
      try {
        Thread.sleep(100);
      }catch (InterruptedException ex) {}
      Platform.runLater(() -> {
        commentsListScroll.setVvalue(1.0f);
      });
    });
  }

  public IntegerProperty objectIdProperty() {
    return objectIdProperty;
  }
  
  class Comment extends VBox {
    private PropertyMap data;

    public Comment(PropertyMap data) {
      this.data = data;
      getStyleClass().add("comment-list-box");
      
      Label text  = new Label(data.getString("text"));
      text.getStyleClass().add("comment-list-box-text");
      
      Label avtor = new Label(data.getString("avtor-name"));
      avtor.getStyleClass().add("comment-list-box-avtor");
      
      Label date  = new Label(Utility.format(data.getTimestamp("date")));
      date.getStyleClass().add("comment-list-box-date");

      getChildren().addAll(date, text, avtor);
      
      setOnMouseClicked(e -> {
        TextArea editor = new TextArea();
        editor.getStyleClass().add("comment-editor-in-realtime");
        
        editor.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
          Text t = new Text(newValue);
          t.setFont(editor.getFont());
          editor.setPrefWidth(t.getBoundsInLocal().getWidth());
          editor.setPrefRowCount((int)(t.getBoundsInLocal().getWidth()/editor.getWidth())+1);
        });
        
        editor.setText(text.getText());
        
        getChildren().remove(1);
        getChildren().add(1, editor);
        
        editor.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
          if(!newValue)
            editor.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, true, false, false));
        });
        
        editor.setOnKeyPressed(ev -> {
          if(ev.getCode() == KeyCode.ENTER && ev.isControlDown()) {
            try {
              data.setValue("text", editor.getText());
              ObjectLoader.update(bum.interfaces.Comment.class, PropertyMap.copy(data, "id", "text"));
            }catch (Exception ex) {
              MsgTrash.out(ex);
              data.setValue("text", text.getText());
            }
            text.setText(data.getString("text"));
            getChildren().remove(1);
            getChildren().add(1, text);
          }
        });
      });
    }

    public PropertyMap getData() {
      return data;
    }
  }
}