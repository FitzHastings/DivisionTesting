package division.fx.controller.deal;

import bum.interfaces.Deal;
import division.fx.DivisionTextField;
import division.fx.border.titleborder.TitleBorderPane;
import division.fx.dialog.FXDialog;
import division.fx.editor.FXObjectEditor;
import division.fx.FXUtility;
import division.util.Utility;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

public class FXDeal extends FXObjectEditor {
  @FXML private DatePicker endDate;
  
        private RadioButton periodRadio = new RadioButton();
  @FXML private TitleBorderPane periodBlock;
  
        private CheckBox reccuranceCheckBox = new CheckBox();
  @FXML private TitleBorderPane checkReccurance;
  
  @FXML private ComboBox<String> durationType;
  @FXML private DivisionTextField durationCount;
  
  
        private RadioButton endDateRadiov= new RadioButton();
  @FXML private TitleBorderPane checkEndDate;
  @FXML private ComboBox<String> reccuranceType;
  @FXML private DivisionTextField reccuranceCount;
  
  @FXML private DatePicker endDatePeriod;
  
  @FXML private RadioButton checkContractPeriod;
  @FXML private RadioButton checkCountPeriod;
  @FXML private RadioButton checkEndDatePeriod;
  
  @FXML private TextField countPeriod;
  @FXML private DatePicker startDate;
  private final StringConverter converter = new IntegerStringConverter();
  
  @Override
  public void initData() {
    startDate.setValue(getObjectProperty().getValue("dealStartDate", LocalDate.class));
    endDate.setValue(getObjectProperty().getValue("dealStartDate", LocalDate.class));
    endDatePeriod.setValue(getObjectProperty().getValue("dealStartDate", LocalDate.class));
    
    periodRadio.setSelected(getObjectProperty().getValue("duration") != null);
    endDateRadiov.setSelected(getObjectProperty().getValue("duration") == null);
    reccuranceCheckBox.setSelected(getObjectProperty().isNotNull("recurrence") && getObjectProperty().getPeriod("recurrence") != Period.ZERO);
    
    if(getObjectProperty().getValue("duration") != null) {
      Period p = getObjectProperty().getValue("duration", Period.class);
      durationCount.setValue(p.getDays() > 0 ? p.getDays() : p.getMonths() > 0 ? p.getMonths() : p.getYears());
      durationType.getSelectionModel().select(p.getDays() > 0 ? 0 : p.getMonths() > 0 ? 1 : 2);
    }
    
    if(getObjectProperty().getValue("recurrence") != null) {
      Period p = getObjectProperty().getPeriod("recurrence");
      reccuranceCount.setValue(p.getDays() > 0 ? p.getDays() : p.getMonths() > 0 ? p.getMonths() : p.getYears());
      reccuranceType.getSelectionModel().select(p.getDays() > 0 ? 0 : p.getMonths() > 0 ? 1 : 2);
    }
    
    getObjectProperty().get("dealStartDate").bind(startDate.valueProperty());
    getObjectProperty().get("dealEndDate").bind(Bindings.createObjectBinding(() -> endDateRadiov.isSelected() ? endDate.getValue() : null, endDate.valueProperty(), endDateRadiov.selectedProperty()));
    
    getObjectProperty().get("duration").bind(Bindings.createObjectBinding(() -> periodRadio.isSelected() ? 
            Utility.convert(durationCount.getValue()+" "+durationType.getSelectionModel().getSelectedItem()) : null, 
            periodRadio.selectedProperty(), 
            durationCount.valueProperty(), 
            durationType.getSelectionModel().selectedItemProperty()));
    
    getObjectProperty().get("recurrence").bind(Bindings.createObjectBinding(() -> 
            periodRadio.isSelected() && reccuranceCheckBox.isSelected() ? 
                    Utility.convert(reccuranceCount.getValue()+" "+reccuranceType.getSelectionModel().getSelectedItem()) : 
                    null, 
            reccuranceCheckBox.selectedProperty(), reccuranceCount.valueProperty(), reccuranceType.getSelectionModel().selectedItemProperty()));
    
    getObjectProperty().get("checkContractPeriod").bind(Bindings.createObjectBinding(() -> 
            checkContractPeriod.isDisable() ? null : checkContractPeriod.isSelected(), 
            checkContractPeriod.selectedProperty(), checkContractPeriod.disabledProperty()));
    
    getObjectProperty().get("periodCount").bind(Bindings.createObjectBinding(() -> 
            countPeriod.isDisable() ? null : getPeriodCount(), 
            countPeriod.disabledProperty(),
            periodRadio.selectedProperty(), 
            reccuranceCheckBox.selectedProperty(), 
            checkContractPeriod.selectedProperty(), 
            checkEndDatePeriod.selectedProperty()));
    
    if(getObjectProperty().getValue("endContractDate") == null)
      countPeriod.setText("1");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    FXUtility.initCss(this);
    ToggleGroup group = new ToggleGroup();
    group.getToggles().addAll(periodRadio, endDateRadiov);
    
    ToggleGroup group2 = new ToggleGroup();
    group2.getToggles().addAll(checkContractPeriod, checkCountPeriod, checkEndDatePeriod);
    
    countPeriod.textProperty().addListener((ob, ol, nw) -> {
      checkCountPeriod.setSelected(true);
      try {
        converter.fromString(nw);
      }catch(Exception ex) {
        durationCount.setText(ol);
      }
    });
    endDatePeriod.valueProperty().addListener((ob, ol, nw) -> checkEndDatePeriod.setSelected(true));
    
    periodBlock.setTitle(periodRadio);
    checkReccurance.setTitle(reccuranceCheckBox);
    checkEndDate.setTitle(endDateRadiov);
    
    periodBlock.getCenter().disableProperty().bind(periodRadio.selectedProperty().not());
    checkEndDate.getCenter().disableProperty().bind(endDateRadiov.selectedProperty().not());
    checkReccurance.getCenter().disableProperty().bind(reccuranceCheckBox.selectedProperty().not());
    
    countPeriod.disableProperty().bind(checkCountPeriod.selectedProperty().not());
    endDatePeriod.disableProperty().bind(checkEndDatePeriod.selectedProperty().not());
    
    endDate.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
      if(newValue != null && newValue.isBefore(startDate.getValue())) {
        FXDialog.show(getRoot(), new Label("Дата окончания не должна быть раньше даты начала"), "Внимание!", FXDialog.ButtonGroup.OK);
        endDate.setValue(oldValue);
      }
    });
    
    endDatePeriod.valueProperty().addListener((ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) -> {
      if(newValue.isBefore(startDate.getValue())) {
        FXDialog.show(getRoot(), new Label("Дата окончания циклов не должна быть раньше даты начала"), "Внимание!", FXDialog.ButtonGroup.OK);
        endDatePeriod.setValue(oldValue);
      }
    });
    
    durationCount.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      try {
        converter.fromString(newValue);
        int index = durationType.getSelectionModel().getSelectedIndex();
        Integer count = newValue.equals("") || Integer.valueOf(newValue) == 0 ? null : Integer.valueOf(newValue);
        durationType.getItems().setAll(getType(count));
        durationType.getSelectionModel().select(index < 0 ? 0 : index);
      }catch(Exception ex) {
        durationCount.setText(oldValue);
      }
    });
    
    reccuranceCount.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
      try {
        converter.fromString(newValue);
        int index = reccuranceType.getSelectionModel().getSelectedIndex();
        Integer count = newValue.equals("") || Integer.valueOf(newValue) == 0 ? null : Integer.valueOf(newValue);
        reccuranceType.getItems().setAll(getType(count));
        reccuranceType.getSelectionModel().select(index < 0 ? 0 : index);
      }catch(Exception ex) {
        reccuranceCount.setText(oldValue);
      }
    });
    
    initData();
  }
  
  private ObservableList<String> getType(Integer count) {
    if(count == null) return FXCollections.observableArrayList();
    if(count%10 == 0 || count%10 >= 5 && count%10 <= 9 || count%100 >= 11 && count%100 <= 14)
      return FXCollections.observableArrayList("дней","месяцев","лет");
    else if(count%10 == 1) return FXCollections.observableArrayList("день","месяц","год");
    else return FXCollections.observableArrayList("дня","месяца","года");
  }

  @Override
  public String validate() {
    String msg = "";
    if(periodRadio.isSelected()) {
      Period dur = Utility.convert(durationCount.getText()+" "+durationType.getSelectionModel().getSelectedItem());
      if(dur.isZero())
        msg += "\n   -необходимо задать длительность сделки";
      else if(reccuranceCheckBox.isSelected()) {
        Period rec = Utility.convert(reccuranceCount.getText()+" "+reccuranceType.getSelectionModel().getSelectedItem());
        if(rec.isZero())
          msg += "\n   -необходимо задать цикличность сделки";
        else if(getDays(rec) < getDays(dur))
          msg += "\n   -цикличность не может быть меньше длительности";
        else {
          if(checkCountPeriod.isSelected() && countPeriod.getText().equals(""))
            msg += "\n   -не задано колличество циклов";
          if(checkEndDatePeriod.isSelected()) {
            if(endDatePeriod.getValue() == null)
              msg += "\n   -необходимо задать дату окончания циклов";
            else if(endDatePeriod.getValue().isBefore(startDate.getValue()))
              msg += "\n   -дата окончания циклов не должна быть раньше даты начала";
          }
        }
      }
    }else {
      if(endDate.getValue().isBefore(startDate.getValue()))
        msg += "\n   -дата окончания не должна быть раньше даты начала";
    }
    return msg;
  }
  
  @Override
  public boolean save() {
    return "".equals(validate());
  }
  
  private int getPeriodCount() {
    int count = 0;
    if(periodRadio.isSelected() && reccuranceCheckBox.isSelected()) {
      LocalDate start = startDate.getValue();
      LocalDate end = null;
      Period dur = Utility.convert(durationCount.getText()+" "+durationType.getSelectionModel().getSelectedItem());
      Period rec = Utility.convert(reccuranceCount.getText()+" "+reccuranceType.getSelectionModel().getSelectedItem());
      if(checkContractPeriod.isSelected() || checkEndDatePeriod.isSelected()) {
        LocalDate endOfDeals = checkEndDatePeriod.isSelected() ? endDatePeriod.getValue() : getObjectProperty().getValue("endContractDate", LocalDate.class);
        if(endOfDeals != null) {
          while(end == null || end.isBefore(endOfDeals) || end.equals(endOfDeals)) {
            count++;
            start = Deal.getStartDeal(start, rec);
            end = Deal.getEndDeal(start, dur);
          }
        }
      }else if(periodRadio.isSelected() && checkCountPeriod.isSelected())
        count = Integer.valueOf(countPeriod.getText());
    }
    return count;
  }

  @Override
  public void close(WindowEvent e) {
    getObjectProperty().clear();
    getMementoProperty().clear();
    super.close(e);
  }
  
  private int getDays(Period p) {
    return p.getDays()+p.getMonths()*31+p.getYears()*12*31;
  }
}