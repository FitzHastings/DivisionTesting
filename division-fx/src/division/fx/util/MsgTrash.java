package division.fx.util;

import division.fx.dialog.FXD;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class MsgTrash {

  private MsgTrash() {
  }
  
  public static void out(Throwable ex) {
    out(null, ex);
  }
  
  public static void out(Node parent, Throwable ex) {
    ex.printStackTrace();
    Platform.runLater(() -> {
      Label msg = new Label(ex.getMessage());
      msg.setWrapText(true);
      msg.setPrefWidth(400);
      FXD.showWait("Ошибка", parent, msg);
    });
  }
}