package division.fx.border.titleborder;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TitleBorderPane extends VBox implements Initializable {
  @FXML
  private HBox titleBorderLabelTool;
  
  //public ObservableList choicevalues = FXCollections.observableArrayList();
  //public ObservableList combovalues  = FXCollections.observableArrayList();
  
  //public enum TitleType {STRING,CHECKBOX,RADIOBUTTON,CHOICEBOX,COMBOBOX,CUSTOM}
  
  //private TitleType type = TitleType.STRING;
  
  public TitleBorderPane() {
    load();
  }
  
  public TitleBorderPane(Parent pane, String title) {
    this(pane, new Label(title));
  }
  
  public TitleBorderPane(Parent pane, Node... nodes) {
    this(nodes);
    setCenter(pane);
  }
  
  public TitleBorderPane(Parent pane, String title, Node... nodes) {
    this(title, nodes);
    setCenter(pane);
  }
  
  public TitleBorderPane(String title, Node... nodes) {
    load();
    setTitle(new Label(title));
    addToTitle(nodes);
  }
  
  public TitleBorderPane(Node... nodes) {
    load();
    setTitle(nodes);
    
    /*Tooltip toolTip = new Tooltip();
    toolTip.textProperty().bind(getTitleLabel().textProperty());
    Tooltip.install(getTitleLabel(), toolTip);
    
    titleBorderLabelTool.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
      Text text = new Text(getTitleLabel().getText());
      text.setFont(getTitleLabel().getFont());
      return text.getLayoutBounds().getWidth() >= getWidth()-60 ? getWidth()-61 : (text.getLayoutBounds().getWidth()+10);
    }, getTitleLabel().textProperty(), widthProperty()));*/
  }
  
  private void load() {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("title-border-pane.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    }catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    titleBorderLabelTool.translateYProperty().bind(titleBorderLabelTool.heightProperty().multiply(-1.1));
  }
  
  public List<Node> getItems() {
    return titleBorderLabelTool.getChildren();
  }
  
  public void addToTitle(Node... nodes) {
    titleBorderLabelTool.getChildren().addAll(nodes);
  }
  
  public void setTitle(Node... nodes) {
    titleBorderLabelTool.getChildren().setAll(nodes);
  }
  
  public Node getTitle() {
    return getTitle(0);
  }
  
  public Node getTitle(int index) {
    return titleBorderLabelTool.getChildren().get(index);
  }
  
  public void setCenter(Node node) {
    ((BorderPane)getChildren().get(0)).setCenter(node);
  }
  
  public void setLeft(Node node) {
    ((BorderPane)getChildren().get(0)).setLeft(node);
  }
  
  public void setBottom(Node node) {
    ((BorderPane)getChildren().get(0)).setBottom(node);
  }
  
  public void setRight(Node node) {
    ((BorderPane)getChildren().get(0)).setRight(node);
  }
  
  public Node getCenter() {
    return ((BorderPane)getChildren().get(0)).getCenter();
  }
  
  public Node getLeft() {
    return ((BorderPane)getChildren().get(0)).getLeft();
  }
  
  public Node getBottom() {
    return ((BorderPane)getChildren().get(0)).getBottom();
  }
  
  public Node getRight() {
    return ((BorderPane)getChildren().get(0)).getRight();
  }
  
  
  
  /*public TitleType getType() {
    return type;
  }

  public void setType(TitleType type) {
    this.type = type;
    switch(type) {
      case CHECKBOX:    titleBorderLabelPane.getChildren().setAll(new CheckBox());break;
      case RADIOBUTTON: titleBorderLabelPane.getChildren().setAll(new RadioButton());break;
      case STRING:      titleBorderLabelPane.getChildren().setAll(new Label());break;
      case CHOICEBOX:
        ChoiceBox choicebox = new ChoiceBox(choicevalues);
        title = new Label(titleBorderLabel.getText());
        titleBorderLabel.setText("");
        titleBorderLabelPane.getChildren().setAll(new HBox(5, title, choicebox));
        break;
      case COMBOBOX:
        ComboBox combobox = new ComboBox(combovalues);
        title = new Label(titleBorderLabel.getText());
        titleBorderLabel.setText("");
        titleBorderLabelPane.getChildren().setAll(new HBox(5, title, combobox));
        break;
    }
  }
  
  public RadioButton getRadioButton() {
    if(getType() == TitleType.RADIOBUTTON)
      return (RadioButton)titleBorderLabel.getGraphic();
    return null;
  }
  
  public CheckBox getCheckBox() {
    if(getType() == TitleType.CHECKBOX)
      return (CheckBox)titleBorderLabel.getGraphic();
    return null;
  }
  
  public ChoiceBox getChoiceBox() {
    if(getType() == TitleType.CHOICEBOX)
      return (ChoiceBox)((HBox)titleBorderLabel.getGraphic()).getChildren().get(1);
    return null;
  }
  
  public ComboBox getComboBox() {
    if(getType() == TitleType.COMBOBOX)
      return (ComboBox)((HBox)titleBorderLabel.getGraphic()).getChildren().get(1);
    return null;
  }

  public String getTitle() {
    return title == null ? titleBorderLabel.getText() : title.getText();
  }

  public void setTitle(String text) {
    if(title == null)
      titleBorderLabel.setText(text);
    else title.setText(text);
  }

  public Label getTitleLabel() {
    return titleBorderLabel;
  }*/
}