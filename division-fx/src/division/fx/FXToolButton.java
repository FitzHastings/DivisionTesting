package division.fx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public class FXToolButton extends Button {

  public FXToolButton(String text) {
    this(text, "");
    setText(text);
  }
  
  public FXToolButton(String toolTipText, String... classes) {
    this(null, toolTipText, classes);
  }
  
  public FXToolButton(EventHandler<ActionEvent> h, String toolTipText, String... classes) {
    if(h != null)
      setOnAction(h);
    setTooltip(new Tooltip(toolTipText));
    getStyleClass().add("tool-button");
    getStyleClass().addAll(classes);
  }
}