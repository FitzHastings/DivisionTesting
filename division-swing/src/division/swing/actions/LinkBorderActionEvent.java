package division.swing.actions;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

public class LinkBorderActionEvent extends ActionEvent {
  private final MouseEvent mouseEvent;
  private final Rectangle titleBounds;

  public LinkBorderActionEvent(Object source, MouseEvent mouseEvent, Rectangle titleBounds) {
    super(source, 0, "clicked");
    this.mouseEvent = mouseEvent;
    this.titleBounds = titleBounds;
  }

  public Rectangle getTitleBounds() {
    return titleBounds;
  }

  public MouseEvent getMouseEvent() {
    return mouseEvent;
  }
}