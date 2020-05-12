package division.fx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

public class FXToolToggleButton extends ToggleButton {
  public FXToolToggleButton(String text) {
    this(text, "");
    setText(text);
  }
  
  public FXToolToggleButton(String toolTipText, String... classes) {
    this(null, toolTipText, classes);
  }
  
  public FXToolToggleButton(EventHandler<ActionEvent> h, String toolTipText, String... classes) {
    if(h != null)
      setOnAction(h);
    setTooltip(new Tooltip(toolTipText));
    getStyleClass().addAll("tool-button","toggle-tool-button");
    getStyleClass().addAll(classes);
  }
}