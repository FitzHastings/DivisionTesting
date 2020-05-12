package division.fx;

import division.swing.DivisionDialog;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.swing.JLayeredPane;

public abstract class DivisionProcess extends DivisionDialog implements Runnable {
  private final JFXPanel    fxContainer = new JFXPanel();
  private final Label       label       = new Label();
  private final ProgressBar progressBar = new ProgressBar(0);
  private int max;
  
  public DivisionProcess() {
    this(null);
  }
  
  public DivisionProcess(Window owner) {
    this(owner,"Выполняю...");
  }
  
  public DivisionProcess(Window owner, String title) {
    this(owner,"Выполняю...", true);
  }
  
  public DivisionProcess(Window owner, String title, boolean modal) {
    this(owner,"Выполняю...", modal, 100);
  }

  public DivisionProcess(Window owner, String title, boolean modal, int max) {
    super(owner, title);
    if(modal)
      setModalityType(ModalityType.APPLICATION_MODAL);
    this.max = max;
    Platform.runLater(() -> {
      progressBar.setMaxWidth(Double.MAX_VALUE);
      Reflection r = new Reflection();
      r.setFraction(0.9);
      progressBar.setEffect(r);
      label.setStyle("-fx-font-weight: bold;");
      VBox vbox = new VBox(10, label, progressBar); 
      vbox.setAlignment(Pos.CENTER_LEFT);
      vbox.setPadding(new Insets(10, 10, 10, 10));
      vbox.setVgrow(progressBar, Priority.ALWAYS);
      vbox.setFillWidth(true);
      fxContainer.setScene(new Scene(vbox));
    });
    setContentPane(fxContainer);
    fxContainer.setPreferredSize(new Dimension(300, 100));
    pack();
    centerLocation();
  }

  public void setMax(int max) {
    this.max = max;
  }
  
  public void setValue(double val) {
    Platform.runLater(() -> progressBar.setProgress(val/max));
  }
  
  public void setText(String text) {
    Platform.runLater(() -> label.setText(text));
  }
}