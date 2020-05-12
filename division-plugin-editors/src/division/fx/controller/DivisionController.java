package division.fx.controller;

import division.ClientMain;
import division.fx.FXUtility;
import java.awt.Cursor;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public abstract class DivisionController extends JFXPanel implements Initializable {
  protected final JDialog dialog = new JDialog(ClientMain.getInstance());
  @FXML protected Parent root;
  private String fxml;
  private String css;

  public DivisionController(String fxml) {
    this.fxml = fxml;
  }
  
  public DivisionController(String fxml,String css) {
    this.fxml = fxml;
    this.css = css;
  }
  
  @Override
  public void show() {
    setCursor(new Cursor(Cursor.WAIT_CURSOR));
    Platform.runLater(() -> {
      try {
        FXMLLoader loader = FXUtility.getLoader(fxml);
        loader.setController(this);
        setScene(new Scene((Parent)loader.load()));
        //getScene().getStylesheets().add(css);
        SwingUtilities.invokeLater(() -> {
          setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        });
      }catch(Exception ex) {
        ex.printStackTrace();
        new Alert(AlertType.ERROR, ex.getMessage(), ButtonType.OK).show();
      }
      
      dialog.setContentPane(this);
      dialog.pack();
      dialog.setLocationRelativeTo(null);
      dialog.setVisible(true);
    });
  }
}
