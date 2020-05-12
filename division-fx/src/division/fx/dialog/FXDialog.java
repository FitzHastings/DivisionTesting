package division.fx.dialog;

import division.fx.FXUtility;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class FXDialog extends Stage {
  public enum Type {OK,CANCEL,CLOSE,YES,NO}
  
  public enum ButtonGroup {
    OK,
    OK_CANCEL,
    OK_CLOSE,
    CANCEL,
    CLOSE,
    YES_NO,
    YES_NO_CANCEL,
    YES_NO_CLOSE}
  
  private Type result = null;
  
  private final HBox buttonPanel = new HBox(5);
  private final BorderPane root = new BorderPane();
    
  public FXDialog(Node parent, Node pane) {
    super(StageStyle.UTILITY);
    initModality(Modality.APPLICATION_MODAL);
    if(parent != null)
      initOwner(parent.getScene().getWindow());
    else setAlwaysOnTop(true);
    buttonPanel.setPadding(new Insets(10, 0, 0, 0));
    buttonPanel.setAlignment(Pos.CENTER_RIGHT);
    root.setCenter(pane);
    root.setBottom(buttonPanel);
    root.setPadding(new Insets(10));
    setScene(new Scene(root));
    FXUtility.initCss(FXDialog.this);
    if(parent != null) {
      FXUtility.copyStylesheets(parent, getScene());
      //FXUtility.copyStylesheets(pane, getScene());
    }
    getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> FXUtility.reloadCss(getScene()));
    requestFocus();
  }

  public HBox getButtonPanel() {
    return buttonPanel;
  }
  
  public Button getButton(Type type) {
    for(Node b:getButtonPanel().getChildren()) {
      if(b instanceof TypeButton && ((TypeButton)b).getType() == type)
        return (Button)b;
    }
    return null;
  }
  
  private void setButtons(Button... buttons) {
    buttonPanel.getChildren().addAll(buttons);
  }
  
  public Type getResult() {
    return result;
  }
  
  public void setResult(Type result) {
    this.result = result;
  }
  
  
  public static Type show(Node parent, Node pane, String title) {
    return show(parent, pane, new SimpleStringProperty(title), ButtonGroup.OK);
  }
  
  public static Type show(Node parent, Node pane, StringProperty title) {
    return show(parent, pane, title, ButtonGroup.OK);
  }
  
  
  public static Type show(Node parent, Node pane, String title, ButtonGroup bg) {
    return show(parent, pane, new SimpleStringProperty(title), bg, (Type type) -> true);
  }
  
  public static Type show(Node parent, Node pane, StringProperty title, ButtonGroup bg) {
    return show(parent, pane, title, bg, (Type type) -> true);
  }
  
  
  public static Type show(Node parent, Node pane, String title, ButtonGroup bg, DialogButtonListener dialogButtonListener) {
    return show(parent, pane, new SimpleStringProperty(title), bg, dialogButtonListener, null);
  }
  
  public static Type show(Node parent, Node pane, StringProperty title, ButtonGroup bg, DialogButtonListener dialogButtonListener) {
    return show(parent, pane, title, bg, dialogButtonListener, null);
  }
  
  public static Type show(Node content, Node pane, String title, ButtonGroup buttonGroup, boolean b) {
    return show(content, pane, new SimpleStringProperty(title), buttonGroup, b, (Type type) -> true, null);
  }
  
  public static Type show(
          Node parent, 
          Node pane, 
          String title, 
          ButtonGroup bg, 
          DialogButtonListener dialogButtonListener,
          EventHandler<WindowEvent> windowHandler) {
    return show(parent, pane, new SimpleStringProperty(title), bg, dialogButtonListener, windowHandler);
  }
  
  public static Type show(
          Node parent, 
          Node pane, 
          String title, 
          ButtonGroup bg, 
          EventHandler<WindowEvent> windowHandler) {
    return show(parent, pane, new SimpleStringProperty(title), bg, (Type type) -> true, windowHandler);
  }
  
  public static Type show(
          Node parent, 
          Node pane, 
          StringProperty title, 
          ButtonGroup bg, 
          DialogButtonListener dialogButtonListener,
          EventHandler<WindowEvent> windowHandler) {
    return show(parent, pane, title, bg, true, dialogButtonListener, windowHandler);
  }
  
  public static Type show(
          Node parent, 
          Node pane, 
          StringProperty title, 
          ButtonGroup bg, 
          boolean resizeable,
          DialogButtonListener dialogButtonListener,
          EventHandler<WindowEvent> windowHandler) {
    FXDialog dialog = create(parent, pane, bg, dialogButtonListener);
    dialog.setResizable(resizeable);
    dialog.setMaximized(resizeable);
    if(title != null)
      dialog.titleProperty().bind(title);
    dialog.toFront();
    if(windowHandler != null)
      dialog.addEventFilter(WindowEvent.ANY, windowHandler);
    dialog.requestFocus();
    dialog.showAndWait();
    return dialog.result;
  }
  
  public static FXDialog create(Node parent, Node pane) {
    return create(parent, pane, ButtonGroup.OK);
  }
  
  public static FXDialog create(Node parent, Node pane, ButtonGroup bg) {
    return create(parent, pane, bg, null);
  }
  
  public static FXDialog create(String title, Node parent, Node pane, ButtonGroup bg) {
    FXDialog dialog = create(parent, pane, bg, null);
    dialog.setTitle(title);
    return dialog;
  }
  
  public static FXDialog create(String title, Node parent, Node pane) {
    FXDialog dialog = create(parent, pane, ButtonGroup.OK, null);
    dialog.setTitle(title);
    return dialog;
  }
  
  public static FXDialog create(String title, Node parent, Node pane, ButtonGroup bg, DialogButtonListener dialogButtonListener) {
    FXDialog dialog = create(parent, pane, bg, dialogButtonListener);
    dialog.setTitle(title);
    return dialog;
  }
  
  public static FXDialog create(String title, Node parent, Node pane, ButtonGroup bg, DialogButtonListener dialogButtonListener, EventHandler<WindowEvent> windowHandler) {
    FXDialog dialog = create(parent, pane, bg, dialogButtonListener);
    if(windowHandler != null)
      dialog.addEventFilter(WindowEvent.ANY, windowHandler);
    dialog.setTitle(title);
    return dialog;
  }
  
  public static FXDialog create(Node parent, Node pane, ButtonGroup bg, DialogButtonListener dialogButtonListener) {
    if(dialogButtonListener == null)
      dialogButtonListener = (Type type) -> true;
    final DialogButtonListener dbl = dialogButtonListener;
    FXDialog dialog = new FXDialog(parent, pane);
    switch(bg) {
      case CANCEL:
        dialog.setButtons(create(Type.CANCEL, "Отмена",  e -> {dialog.result = Type.CANCEL; if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case CLOSE:
        dialog.setButtons(create(Type.CLOSE, "Закрыть", e -> {dialog.result = Type.CLOSE;  if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case OK:
        dialog.setButtons(create(Type.OK, "Ok",      e -> {dialog.result = Type.OK;     if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case OK_CANCEL:
        dialog.setButtons(create(Type.OK, "Ok",      e -> {dialog.result = Type.OK;     if(dbl.valid(dialog.result))dialog.close();}), 
                          create(Type.CANCEL, "Отмена",  e -> {dialog.result = Type.CANCEL; if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case OK_CLOSE:
        dialog.setButtons(create(Type.OK, "Ok",      e -> {dialog.result = Type.OK;     if(dbl.valid(dialog.result))dialog.close();}), 
                          create(Type.CLOSE, "Закрыть", e -> {dialog.result = Type.CLOSE;  if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case YES_NO:
        dialog.setButtons(create(Type.YES, "Да",      e -> {dialog.result = Type.YES;    if(dbl.valid(dialog.result))dialog.close();}),
                          create(Type.NO, "Нет",     e -> {dialog.result = Type.NO;     if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case YES_NO_CANCEL:
        dialog.setButtons(create(Type.YES, "Да",      e -> {dialog.result = Type.YES;    if(dbl.valid(dialog.result))dialog.close();}),
                          create(Type.NO, "Нет",     e -> {dialog.result = Type.NO;     if(dbl.valid(dialog.result))dialog.close();}),
                          create(Type.CANCEL, "Отмена",  e -> {dialog.result = Type.CANCEL; if(dbl.valid(dialog.result))dialog.close();}));
        break;
      case YES_NO_CLOSE:
        dialog.setButtons(create(Type.YES, "Да",      e -> {dialog.result = Type.YES;    if(dbl.valid(dialog.result))dialog.close();}),
                          create(Type.NO, "Нет",     e -> {dialog.result = Type.NO;     if(dbl.valid(dialog.result))dialog.close();}),
                          create(Type.CLOSE, "Закрыть", e -> {dialog.result = Type.CLOSE;  if(dbl.valid(dialog.result))dialog.close();}));
        break;
    }
    return dialog;
  }
  
  public static Button create(Type type, String title, EventHandler<ActionEvent> handler) {
    Button button = new TypeButton(type, title);
    button.setOnAction(handler);
    return button;
  }
  
  static class TypeButton extends Button {
    private Type type;

    public TypeButton(Type type) {
      this.type = type;
    }

    public TypeButton(Type type, String text) {
      super(text);
      this.type = type;
    }

    public TypeButton(Type type, String text, Node graphic) {
      super(text, graphic);
      this.type = type;
    }

    public Type getType() {
      return type;
    }
  }
}