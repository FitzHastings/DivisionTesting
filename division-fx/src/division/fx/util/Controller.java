package division.fx.util;

import division.fx.FXUtility;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;

public interface Controller extends Initializable {
  
  public default Parent loadRootPane() {
    try {
      String fxml = getFxmlPath();
      FXMLLoader loader;
      if(fxml == null)
        loader = FXUtility.getLoader(this);
      else loader = FXUtility.getLoader(fxml);
      loader.setController(this);
      return (Parent)loader.load();
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    return null;
  }
  
  public default String getFxmlPath() {
    return null;
  }
  
  public default String getCssPath() {
    return null;
  }
}