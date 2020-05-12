package division.fx.controller.company.partition;

import conf.P;
import division.fx.DivisionTextField;
import division.fx.FXUtility;
import division.fx.PropertyMap;
import division.fx.editor.FXObjectEditor;
import division.json.RESTReader;
import java.math.BigInteger;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.BigIntegerStringConverter;
import javafx.util.converter.LongStringConverter;

public class FXAccount extends FXObjectEditor {
  private final Label                   accLabel  = new Label("Расч. счёт");
  private final DivisionTextField<BigInteger> accText   = new DivisionTextField<>(new BigIntegerStringConverter());
  
  private final Label                   bikLabel  = new Label("БИК");
  private final DivisionTextField<Long> bikText   = new DivisionTextField<>(new LongStringConverter());
  
  private final Label                   corLabel  = new Label("Корр. счёт");
  private final DivisionTextField<BigInteger> corText   = new DivisionTextField<>(new BigIntegerStringConverter());
  
  private final Label                   nameLabel = new Label("Банк");
  private final TextArea                nameText  = new TextArea();
  
  private final GridPane                grid      = new GridPane();
  
  public FXAccount() {
    new VBox().setPadding(new Insets(5));
    FXUtility.initMainCss(this);
    grid.addColumn(0, accLabel, bikLabel, corLabel);
    grid.addColumn(1, accText, bikText, corText);
    
    grid.getColumnConstraints().addAll(new ColumnConstraints(), new ColumnConstraints());
    grid.getColumnConstraints().get(1).setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().get(0).setHgrow(Priority.NEVER);
    corLabel.setLabelFor(corText);
    
    VBox.setVgrow(nameText, Priority.ALWAYS);
    getRoot().setCenter(new VBox(5, grid, nameLabel, nameText));
    
    RESTReader.create()
            .add(bikText,   P.String("REST.bank"), "suggestions", 3, p -> {
              bikText.setText(p.getMap("data").getString("bic"));
              nameText.setText(p.getMap("data").getMap("name").getString("payment"));
              corText.setText(p.getMap("data").getString("correspondent_account"));
              bikText.positionCaret(bikText.getText().length());
              nameText.positionCaret(nameText.getText().length());
              corText.positionCaret(corText.getText().length());
            },   p -> p.getMap("data").getString("bic")+" - "+p.getMap("data").getMap("name").getString("payment"))
            .add(nameText,   P.String("REST.bank"), "suggestions", 3, p -> {
              bikText.setText(p.getMap("data").getString("bic"));
              nameText.setText(p.getMap("data").getMap("name").getString("payment"));
              corText.setText(p.getMap("data").getString("correspondent_account"));
              bikText.positionCaret(bikText.getText().length());
              nameText.positionCaret(nameText.getText().length());
              corText.positionCaret(corText.getText().length());
            },   p -> p.getMap("data").getMap("name").getString("payment")+" - "+p.getMap("data").getString("bic"));
  }
  
  @Override
  public void initData() {
    accText.setText("");
    bikText.setText("");
    nameText.setText("");
    corText.setText("");
    
    if(getObjectProperty() != null) {
      accText.setDisable(true);
      bikText.setDisable(true);
      nameText.setDisable(true);
      corText.setDisable(true);
      
      accText.setText(getObjectProperty().getString("number"));
      PropertyMap data = RESTReader.create().get(P.String("REST.bank"), getObjectProperty().getString("bank_bik"));
      
      if(data != null && data.getList("suggestions").size() == 1) {
        PropertyMap p = data.getList("suggestions").get(0);
        bikText.setText(p.getMap("data").getString("bic"));
        nameText.setText(p.getMap("data").getMap("name").getString("payment"));
        corText.setText(p.getMap("data").getString("correspondent_account"));
      }else {
        bikText.setText(getObjectProperty().getString("bank_bik"));
        nameText.setText(getObjectProperty().getString("bank_name"));
        corText.setText(getObjectProperty().getString("bank_corrAccount"));
      }
      
      accText.setDisable(false);
      bikText.setDisable(false);
      nameText.setDisable(false);
      corText.setDisable(false);
    }
  }
}