package division.fx.desktop;

import division.fx.FXUtility;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;

public class FXDesktopPane extends BorderPane {
  private FlowPane frameBar = new FlowPane();
  
  private Pane desktop  = new Pane();

  public FXDesktopPane(FXInternalFrame... frames) {
    FXUtility.initCss(this);
    setCenter(desktop);
    setBottom(frameBar);
    add(frames);
  }
  
  public void add(FXInternalFrame... frames) {
    for(FXInternalFrame frame:frames) {
      desktop.getChildren().add(frame);

      ToggleButton button = new ToggleButton(frame.getTitle());

      frame.minimizeProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        button.setSelected(newValue);
      });
      
      button.setOnAction((ActionEvent event) -> frame.minimizeProperty().set(!frame.minimizeProperty().get()));

      frameBar.getChildren().add(button);
      

      frame.closedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        if(newValue) {
          frameBar.getChildren().remove(button);
          getChildren().remove(frame);
          frame.dispose();
        }
      });
    }
  }
}