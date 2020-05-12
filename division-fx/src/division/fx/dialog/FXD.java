package division.fx.dialog;

import division.fx.FXButton;
import division.fx.FXUtility;
import division.fx.gui.FXGLoader;
import division.fx.gui.FXStorable;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class FXD extends Stage {
  public enum DialogType {CONFIRMATION, ERROR, INFORMATION, NONE, WARNING}
  public enum ButtonType {OK,CANCEL,CLOSE,YES,NO}
  private ButtonType result = null;
  private final HBox buttonPanel = new HBox(5);
  private final BorderPane root = new BorderPane();
  
  public FXD(FXDHandler handler, String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    super(stageStyle);
    setTitle(title);
    initModality(modality);
    buttonPanel.setPadding(new Insets(10, 0, 0, 0));
    buttonPanel.setAlignment(Pos.CENTER_RIGHT);
    root.setCenter(pane);
    root.setBottom(buttonPanel);
    root.setPadding(new Insets(10));
    setScene(new Scene(root));
    setResizable(resizable);
    
    FXUtility.initMainCss(getRoot());
    FXUtility.initMainCss(this);
    
    if(parent != null) {
      Scene scene = parent.getScene();
      initOwner(scene == null ? null : scene.getWindow());
      if(scene != null)
        FXUtility.copyStylesheets(scene, getScene());
    }
    getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> FXUtility.reloadCss(getScene()));
    requestFocus();
    
    addEventHandler(WindowEvent.WINDOW_SHOWING, e -> {
      if(pane instanceof FXStorable)
        ((FXStorable)pane).load(getTitle());
      else FXGLoader.load(getTitle(), pane);
    });
    
    addEventHandler(WindowEvent.WINDOW_HIDING, e -> {
      if(pane instanceof FXStorable)
        ((FXStorable)pane).store(getTitle());
      else FXGLoader.store(getTitle(), pane);
    });
    
    for(ButtonType bt:buttons)
      buttonPanel.getChildren().add(new FXButton(e -> {
        result = bt;
        if(handler != null) {
          if(handler.isCloseDialog(bt))
            close();
        }else close();
      }, bt.toString(), new String[0]));
  }
  
  public void fire(ButtonType buttonType) {
    buttonPanel.getChildren().stream().filter(b -> ((FXButton)b).getText().equals(buttonType.toString())).forEach(b -> ((FXButton)b).fire());
  }

  public HBox getButtonPanel() {
    return buttonPanel;
  }
  
  public FXButton getButton(FXD.ButtonType type) {
    return getButton(type.toString());
  }
  
  public FXButton getButton(String text) {
    return ((FXButton)getButtonPanel().getChildren().stream().filter(b -> b instanceof Button && ((FXButton)b).getText().toLowerCase().equals(text.toLowerCase())).findFirst().orElseGet(() -> null));
  }

  public BorderPane getRoot() {
    return root;
  }
  
  public static FXD create(Node pane) {
    return create("", pane);
  }
  
  public static FXD create(String title, Node pane) {
    return create(title, pane, ButtonType.OK);
  }
  
  public static FXD create(String title, Node pane, ButtonType... buttons) {
    return create(title, null, pane, buttons);
  }
  
  public static FXD create(String title, Node parent, Node pane, ButtonType... buttons) {
    return create(title, parent, pane, Modality.NONE, buttons);
  }
  
  public static FXD create(String title, Node parent, Node pane, Modality modality, ButtonType... buttons) {
    return create(title, parent, pane, modality, StageStyle.DECORATED, buttons);
  }
  
  public static FXD create(String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, ButtonType... buttons) {
    return create(title, parent, pane, modality, stageStyle, true, buttons);
  }
  
  public static FXD create(String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    return create(null, title, parent, pane, modality, stageStyle, resizable, buttons);
  }
  
  public static FXD create(FXDHandler handler, String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    return new FXD(handler, title, parent, pane, modality, stageStyle, resizable, buttons);
  }
  
  
  /////////////////////////////////////////////////////////
  public static Optional<ButtonType> showWait(Node pane) {
    return showWait("", pane);
  }
  
  public static Optional<ButtonType> showWait(String title, Node pane) {
    return showWait(title, pane, ButtonType.OK);
  }
  
  public static Optional<ButtonType> showWait(String title, Node pane, ButtonType... buttons) {
    return showWait(title, null, pane, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, Node pane, ButtonType... buttons) {
    return showWait(title, parent, pane, Modality.NONE, buttons);
  }
  
  public static Optional<ButtonType> showWait(FXDHandler handler, String title, Node parent, Node pane, ButtonType... buttons) {
    return showWait(handler, title, parent, pane, Modality.NONE, StageStyle.DECORATED, true, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, Node pane, Modality modality, ButtonType... buttons) {
    return showWait(title, parent, pane, modality, StageStyle.DECORATED, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, ButtonType... buttons) {
    return showWait(title, parent, pane, modality, stageStyle, true, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    return showWait(null, title, parent, pane, modality, stageStyle, resizable, buttons);
  }
  
  public static Optional<ButtonType> showWait(FXDHandler handler, String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    FXD fxd = create(handler, title, parent, pane, modality, stageStyle, resizable, buttons);
    fxd.setAlwaysOnTop(true);
    fxd.showAndWait();
    return Optional.ofNullable(fxd.result);
  }
  
  /////////////////////////////////////////////////////////
  public static Optional<ButtonType> showWait(String msg) {
    return showWait("", msg);
  }
  
  public static Optional<ButtonType> showWait(String title, String msg) {
    return showWait(title, msg, ButtonType.OK);
  }
  
  public static Optional<ButtonType> showWait(String title, String msg, ButtonType... buttons) {
    return showWait(title, null, msg, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, String msg, ButtonType... buttons) {
    return showWait(title, parent, msg, Modality.NONE, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, String msg, Modality modality, ButtonType... buttons) {
    return showWait(title, parent, msg, modality, StageStyle.DECORATED, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, String msg, Modality modality, StageStyle stageStyle, ButtonType... buttons) {
    return showWait(title, parent, msg, modality, stageStyle, true, buttons);
  }
  
  public static Optional<ButtonType> showWait(String title, Node parent, String msg, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    return showWait(title, parent, new VBox(new Label(msg)), modality, stageStyle, resizable, buttons);
  }
  
  public ButtonType showDialog() {
    setAlwaysOnTop(true);
    showAndWait();
    return Optional.ofNullable(result).orElseGet(() -> ButtonType.CANCEL);
  }
  
  
  public void showDialog(EventHandler<ActionEvent> buttonHandler) {
    buttonPanel.getChildren().stream().forEach(b -> ((Button)b).setOnAction(e -> buttonHandler.handle(new ActionEvent(ButtonType.valueOf(((Button)b).getText()), b))));
    setAlwaysOnTop(true);
    showAndWait();
  }
  
  
  
  
  
  
  /////////////////////////////////////////////////////////
  public static FXD show(FXDHandler handler, Node pane) {
    return show(handler, "", pane);
  }
  
  public static FXD show(FXDHandler handler, String title, Node pane) {
    return show(handler, title, pane, ButtonType.OK);
  }
  
  public static FXD show(FXDHandler handler, String title, Node pane, ButtonType... buttons) {
    return show(handler, title, null, pane, buttons);
  }
  
  public static FXD show(FXDHandler handler, String title, Node parent, Node pane, ButtonType... buttons) {
    return show(handler, title, parent, pane, Modality.NONE, buttons);
  }
  
  public static FXD show(FXDHandler handler, String title, Node parent, Node pane, Modality modality, ButtonType... buttons) {
    return show(handler, title, parent, pane, modality, StageStyle.DECORATED, buttons);
  }
  
  public static FXD show(FXDHandler handler, String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, ButtonType... buttons) {
    return show(handler, title, parent, pane, modality, stageStyle, true, buttons);
  }
  
  public static FXD show(FXDHandler handler, String title, Node parent, Node pane, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    FXD fxd = create(handler, title, parent, pane, modality, stageStyle, resizable, buttons);
    fxd.show();
    return fxd;
  }
  
  /////////////////////////////////////////////////////////
  public static void show(FXDHandler handler, String msg) {
    show(handler, "", msg);
  }
  
  public static void show(FXDHandler handler, String title, String msg) {
    show(handler, title, msg, ButtonType.OK);
  }
  
  public static void show(FXDHandler handler, String title, String msg, ButtonType... buttons) {
    show(handler, title, null, msg, buttons);
  }
  
  public static void show(FXDHandler handler, String title, Node parent, String msg, ButtonType... buttons) {
    show(handler, title, parent, msg, Modality.NONE, buttons);
  }
  
  public static void show(FXDHandler handler, String title, Node parent, String msg, Modality modality, ButtonType... buttons) {
    show(handler, title, parent, msg, modality, StageStyle.DECORATED, buttons);
  }
  
  public static void show(FXDHandler handler, String title, Node parent, String msg, Modality modality, StageStyle stageStyle, ButtonType... buttons) {
    show(handler, title, parent, msg, modality, stageStyle, true, buttons);
  }
  
  public static void show(FXDHandler handler, String title, Node parent, String msg, Modality modality, StageStyle stageStyle, boolean resizable, ButtonType... buttons) {
    show(handler, title, parent, new Label(msg), modality, stageStyle, resizable, buttons);
  }
  
  public interface FXDHandler {
    public boolean isCloseDialog(ButtonType type);
  }
}