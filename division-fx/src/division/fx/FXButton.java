package division.fx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class FXButton extends Button {

  public FXButton() {
  }

  public FXButton(String text) {
    super(text);
  }

  public FXButton(String text, Node graphic) {
    super(text, graphic);
  }
  
  public FXButton(String[] styleclass) {
    getStyleClass().addAll(styleclass);
  }

  public FXButton(String text, String[] styleclass) {
    super(text);
    getStyleClass().addAll(styleclass);
  }

  public FXButton(String text, Node graphic, String[] styleclass) {
    super(text, graphic);
    getStyleClass().addAll(styleclass);
  }
  
  public FXButton(EventHandler<ActionEvent> h, String... styleclass) {
    getStyleClass().addAll(styleclass);
    setOnAction(h);
  }
  
  public FXButton(String toolTipText, EventHandler<ActionEvent> h, String... styleclass) {
    this(h, styleclass);
    setTooltip(new Tooltip(toolTipText));
  }

  public FXButton(EventHandler<ActionEvent> h, String text, String[] styleclass) {
    super(text);
    getStyleClass().addAll(styleclass);
    setOnAction(h);
  }

  public FXButton(EventHandler<ActionEvent> h, String text, Node graphic, String[] styleclass) {
    super(text, graphic);
    getStyleClass().addAll(styleclass);
    setOnAction(h);
  }
}
