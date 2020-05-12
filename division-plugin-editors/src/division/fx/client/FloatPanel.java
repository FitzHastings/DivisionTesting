package division.fx.client;

import division.fx.FXUtility;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class FloatPanel extends BorderPane {
  private Label headLabel = new Label();
  private BorderPane header = new BorderPane();
  
  private Button fixfloat = new Button();
  private HBox buttonBox = new HBox(fixfloat);
  
  private BooleanProperty fix = new SimpleBooleanProperty(false);

  public FloatPanel() {
    this("");
  }
  
  public FloatPanel(String headerText) {
    FXUtility.initCss(this);
    header.setCenter(headLabel);
    header.setRight(buttonBox);
    setTop(header);
    headLabel.setText(headerText);
    
    fixfloat.setOnAction((ActionEvent event) -> {
      fix.set(!fix.get());
      fixfloat.pseudoClassStateChanged(PseudoClass.getPseudoClass("fix"), fix.get());
    });
  }
  
  public void setText(String header) {
    headLabel.setText(header);
  }
  
  public BooleanProperty fixProperty() {
    return fix;
  }
}