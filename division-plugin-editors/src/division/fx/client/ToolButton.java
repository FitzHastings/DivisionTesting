package division.fx.client;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

public class ToolButton extends Group {
  private BooleanProperty selected = new SimpleBooleanProperty(false);
  private Label label = new Label();
  private FloatPanel rootPopPane = new FloatPanel();
  private boolean RESIZE_E;

  public ToolButton(String text) {
    this(text, 0);
  }

  public ToolButton(String text, double value) {
    rootPopPane.setText(text);
    label.setText(text);
    label.setRotate(-90);
    getChildren().add(label);

    rootPopPane.getStyleClass().add("root-pop-pane");

    makeResizable(5);

    label.setOnMouseClicked((MouseEvent event) -> selected.set(!selected.get()));
    selectedProperty().addListener((ob, ol, nw) -> label.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), nw));
  }

  public void makeResizable(double mouseBorderWidth) {
    rootPopPane.setOnMouseMoved(mouseEvent -> {
      if(!rootPopPane.fixProperty().get()) {
        RESIZE_E = mouseEvent.getX() >= rootPopPane.getWidth()-mouseBorderWidth;
        rootPopPane.setCursor(RESIZE_E ? Cursor.E_RESIZE  : Cursor.DEFAULT);
      }
    });
    
    rootPopPane.setOnMouseReleased((MouseEvent mouseEvent) -> {
      rootPopPane.setPrefWidth(rootPopPane.getWidth());
      RESIZE_E = false;
    });
    
    rootPopPane.setOnMouseDragged(mouseEvent -> {
      double mouseX = mouseEvent.getX();
      if(RESIZE_E)
        rootPopPane.setPrefWidth(mouseX);
    });
  }

  public String getText() {
    return label.getText();
  }

  public FloatPanel getRootPopPane() {
    return rootPopPane;
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }
}
