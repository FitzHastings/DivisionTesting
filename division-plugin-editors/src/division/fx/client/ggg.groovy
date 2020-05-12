package division.fx.client

import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.stage.Stage

class ggg extends Pane {
  def start(name) {
    new Stage(scene: new Scene(new BorderPane(new Button(focusTraversable: false, layoutY: 23, prefHeight: 30, prefWidth: 30, text: name)))).show();
  }
}