package division.fx;

import javafx.event.EventHandler;
import javafx.scene.control.Label;

public class LinkLabel extends Label {
  
  public LinkLabel(String text, String... styleclass) {
    this(text, null, styleclass);
  }

  public LinkLabel(String text, EventHandler e, String... styleclass) {
    super(text);
    getStyleClass().addAll(styleclass);
    getStyleClass().add("link-label");
    if(e != null)
      setOnMouseClicked(e);
  }
}