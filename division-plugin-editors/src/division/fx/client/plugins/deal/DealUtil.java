package division.fx.client.plugins.deal;

import bum.editors.util.ObjectLoader;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Contract;
import bum.interfaces.ContractProcess;
import bum.interfaces.Service;
import division.fx.DivisionTextField;
import division.fx.FXButton;
import division.fx.PropertyMap;
import division.fx.controller.deal.FXDeal;
import division.fx.controller.store.EquipmentFactorTable;
import division.fx.controller.store.FXCompanyStore;
import division.fx.controller.store.FXCompanyStore.ItemType;
import division.fx.dialog.FXD;
import division.fx.editor.GLoader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.BigDecimalStringConverter;

public class DealUtil {
  public static void createDeal(Node parent, Integer contractProcessId, LocalDate start) {
    PropertyMap contractProcess = ObjectLoader.getMap(ContractProcess.class, contractProcessId);
    PropertyMap process         = ObjectLoader.getMap(Service.class, contractProcess.getInteger("process"));
    PropertyMap contract        = ObjectLoader.getMap(Contract.class, contractProcess.getInteger("contract"));
    PropertyMap ownerPartition  = ObjectLoader.getMap(CompanyPartition.class, contract.getInteger(process.getValue("owner", Service.Owner.class) == Service.Owner.SELLER ? "sellerCompanyPartition" : "customerCompanyPartition"));
    
    if(process.isNull("owner")) {
      PropertyMap dealProp = FXDeal.get(FXDeal.class, parent, PropertyMap.create()
              .setValue("dealStartDate"  , start)
              .setValue("endContractDate", contract.getSqlDate("endDate").toLocalDate())
              .setValue("duration"       , Period.ofDays(10))
              .setValue("recurrence"     , Period.ZERO));
        if(dealProp != null && !dealProp.isEmpty()) {
          System.out.println(dealProp.toJson());
          /*int dealCount = dealProp.getInteger("periodCount");
          if(dealProp.is("checkContractPeriod")) {
            LocalDate startDate = dealProp.getLocalDate("dealStartDate");
            LocalDate endDate   = dealProp.getLocalDate("dealEndDate");
          }
          
          PropertyMap deal = PropertyMap.create()
                  .setValue("service", process.getInteger("id"))
                  .setValue("contract", contract.getInteger("id"))
                  .setValue("contractProcess", contractProcess.getInteger("id"))
                  .setValue("dealStartDate", dealProp.getLocalDate("dealStartDate"));*/
        }
    }else {
      ObservableList<PropertyMap> equipments = getEquipments(parent, ownerPartition);
      if(!equipments.isEmpty()) {
        equipments.stream().forEach(object -> {
          System.out.println();
          System.out.println("##########################################################");
          System.out.println(object.toJson());
        });
      }
    }
  }
  
  private static ObservableList<PropertyMap> getEquipments(Node parent, PropertyMap ownerPartition) {
    FXCompanyStore       storeTree    = new FXCompanyStore();
    EquipmentFactorTable equipFactors = new EquipmentFactorTable();
    DivisionTextField<BigDecimal> amount = new DivisionTextField(new BigDecimalStringConverter() {
      @Override
      public BigDecimal fromString(String value) {
        BigDecimal am = super.fromString(value);
        return am != null && am.compareTo(BigDecimal.ZERO) <= 0 ? null : am;
      }
    }, BigDecimal.ONE);
    
    Label      groupLabel = new Label();
    FXButton   addButton  = new FXButton(new String[]{"add-tool-button"});
    HBox       hb         = new HBox(10, groupLabel, amount, addButton);
    VBox       treeBox    = new VBox(storeTree, hb);
    SplitPane  split      = new SplitPane(treeBox, equipFactors);
    
    split.setOrientation(Orientation.VERTICAL);
    VBox.setVgrow(storeTree, Priority.ALWAYS);
    hb.setAlignment(Pos.CENTER_RIGHT);
    groupLabel.setText("Выберите объект");
    
    storeTree.companyPartitionProperty().setValue(ownerPartition);
    
    FXD dialog = FXD.create("Выбор объектов", parent, split, FXD.ButtonType.OK, FXD.ButtonType.CANCEL);
    dialog.setOnCloseRequest(e -> GLoader.store(DealUtil.class.getName(), split, storeTree));
    dialog.setOnShowing(e -> GLoader.load(DealUtil.class.getName(), split, storeTree));
    
    addButton.setOnAction(e -> {
      // Получить введённое количество
      BigDecimal am = amount.getValue();
      // Получить признак контроля остатков на складе
      boolean controll = storeTree.getSelectedStore().is("controllOut");
      // Цикл по выбранным объектам
      storeTree.getTreetable().getSelectionModel().getSelectedItems().stream().forEach(treeItem -> {
        PropertyMap item = treeItem.getValue();
        PropertyMap itemToDeal  = item.copy(); // объект в сделку
        PropertyMap zakazToDeal = null; // объект в сделку под заказ
        
        if(item.getValue("itemtype", ItemType.class) == ItemType.EQUIPMENT) { // Если выделен объект
          if(controll) { // Если есть контроль остатков
            if(am.compareTo(item.getBigDecimal("amount")) > 0) { // Введённая сумма превышает лимит
              
              RadioButton toZakaz    = new RadioButton("Недостающее количество объектов оформить как заказ");
              RadioButton onlyAmount = new RadioButton("Выбрать всё что есть");
              ToggleGroup group = new ToggleGroup();
              group.getToggles().addAll(onlyAmount, toZakaz);
              onlyAmount.setSelected(true);
              VBox pane = new VBox(5, new Label("Введённая сумма превышает лимит, возможные варианты действия:"),onlyAmount,toZakaz);
              
              if(FXD.showWait("Внимание!!!", parent, pane, FXD.ButtonType.OK).get() == FXD.ButtonType.OK) { // если вариант выбран
                if(toZakaz.isSelected()) // создаём заказ
                  zakazToDeal = item.copy().setValue("amount", am.subtract(item.getBigDecimal("amount"))).setValue("zakaz", true);
                item.setValue("amount", BigDecimal.ZERO);
              }else itemToDeal = null; // отм еняем выбор объекта в случае отмены
            }else {
              itemToDeal.setValue("amount", am);
              item.setValue("amount", item.getBigDecimal("amount").subtract(am));
            }
          }else itemToDeal.setValue("amount", am); // Контрполя остатков нет
        }else if(item.getValue("itemtype", ItemType.class) == ItemType.GROUP) {
          if(treeItem.getChildren().filtered(it -> it.getValue().getValue("itemtype", ItemType.class) != ItemType.EQUIPMENT).isEmpty()) {
            itemToDeal.setValue("id", null);
            if(controll) {
              itemToDeal = null;
              if(FXD.showWait("Внимание!", parent, "На складе осуществляется контроль остатков,\nдля продолжения необходимо сформировать заказ.", FXD.ButtonType.OK, FXD.ButtonType.CANCEL).get() == FXD.ButtonType.OK) {
                zakazToDeal = item.copy().setValue("amount", am).setValue("zakaz", true);
                zakazToDeal.setValue("group", zakazToDeal.getInteger("id")).setValue("id", null);
              }
            }else itemToDeal.setValue("amount", am); // Контрполя остатков нет
          }else itemToDeal = null;
        }else itemToDeal = null;
        
        if(itemToDeal != null)
          equipFactors.getItems().add(itemToDeal);
        
        if(zakazToDeal != null)
          equipFactors.getItems().add(zakazToDeal);
      });
      equipFactors.initHeader();
    });
    
    if(dialog.showDialog() != FXD.ButtonType.OK)
      equipFactors.clear();
    
    return equipFactors.getItems();
  }
  
  private static int getSelectedVariant(Node parent) {
    RadioButton toZakaz    = new RadioButton("Недостающее количество объектов оформить как заказ");
    RadioButton onlyAmount = new RadioButton("Выбрать всё что есть");
    ToggleGroup group = new ToggleGroup();
    group.getToggles().addAll(onlyAmount, toZakaz);
    onlyAmount.setSelected(true);
    VBox pane = new VBox(5, new Label("Введённая сумма превышает лимит, возможные варианты действия:"),onlyAmount,toZakaz);
    if(FXD.showWait("Внимание!!!", parent, pane, FXD.ButtonType.OK).get() == FXD.ButtonType.OK)
      return onlyAmount.isSelected() ? 0 : 1;
    return -1;
  }
}