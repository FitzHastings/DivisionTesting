package division.fx.client.plugins.deal.master;

import division.fx.dialog.FXD;
import division.fx.gui.FXStorable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class MasterPane extends BorderPane implements FXStorable {
  private final Button cancel = new Button("Отменить");
  private final Button next   = new Button("Далее");
  private final Button prev   = new Button("Назад");
  private final Button ok     = new Button("Завершить");
  
  private final BorderPane root = new BorderPane();
  
  private final ObservableList<Step> stepPanels = FXCollections.observableArrayList();
  
  private FXD dialog;
  private boolean result;

  public MasterPane() {
    setCenter(root);
    
    next.setOnAction(e -> next());
    prev.setOnAction(e -> prev());
    
    ok.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return getCurrentStepIndex() < stepPanels.size()-1;
    }, root.centerProperty(), stepPanels));
    
    next.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return getCurrentStepIndex() == stepPanels.size()-1;
    }, root.centerProperty(), stepPanels));
    
    prev.disableProperty().bind(Bindings.createBooleanBinding(() -> {
      return getCurrentStepIndex() <= 0;
    }, root.centerProperty(), stepPanels));
    
    cancel.setOnAction(e -> {
      dialog.close();
      result = false;
    });
    
    ok.setOnAction(e ->  {
      if(getCurrentStep().end()) {
        result = true;
        dialog.close();
      }
    });
    
    stepPanels.addListener((ListChangeListener.Change<? extends Step> c) -> {
      storeControls().removeAll(stepPanels);
      storeControls().addAll(stepPanels);
      stepPanels.forEach(s -> s.setMaster(this));
    });
  }
  
  public boolean show(Node parent) {
    result = true;
    dialog = FXD.create("", parent, this);
    dialog.getButtonPanel().getChildren().addAll(cancel, prev, next, ok);
    next();
    dialog.setOnCloseRequest(e -> result = false);
    dialog.showDialog();
    return result;
  }

  public Step getCurrentStep() {
    return (Step)root.getCenter();
  }
  
  private void setCurrentStep(Step step) {
    step.init = true;
    root.setCenter(step);
    dialog.setTitle(step.getTitle());
    step.startCurrent();
  }
  
  public int getCurrentStepIndex() {
    if(getCurrentStep() == null)
      return -1;
    return stepPanels.indexOf(getCurrentStep());
  }
  
  public void startStep(int index) {
    setCurrentStep(stepPanels.get(index));
  }
  
  public void next() {
    if(getCurrentStepIndex() < stepPanels.size()-1 && (getCurrentStep() == null || getCurrentStep().end()) && stepPanels.get(getCurrentStepIndex()+1).start())
      setCurrentStep(stepPanels.get(getCurrentStepIndex()+1));
  }
  
  public void prev() {
    if(getCurrentStepIndex() > 0 && stepPanels.get(getCurrentStepIndex()-1).start())
      setCurrentStep(stepPanels.get(getCurrentStepIndex()-1));
  }

  @Override
  public void store(String fileName) {
    FXStorable.super.store(getClass().getName());
  }

  @Override
  public void load(String fileName) {
    FXStorable.super.load(getClass().getName());
  }
  
  public ObservableList<Step> stepPanels() {
    return stepPanels;
  }
  
  private final ObservableList<Node> stories = FXCollections.observableArrayList(root);

  @Override
  public ObservableList<Node> storeControls() {
    return stories;
  }
}