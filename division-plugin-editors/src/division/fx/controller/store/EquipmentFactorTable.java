package division.fx.controller.store;

import bum.editors.util.ObjectLoader;
import bum.interfaces.Factor;
import division.fx.table.FXDivisionTable;
import division.fx.PropertyMap;
import division.fx.table.Column;
import division.fx.table.FXDivisionTableCell;
import division.fx.table.filter.DateFilter;
import division.fx.table.filter.ListFilter;
import division.fx.table.filter.TextFilter;
import division.fx.FXUtility;
import java.math.BigDecimal;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.ArrayUtils;

public class EquipmentFactorTable extends BorderPane {
  private final FXDivisionTable<PropertyMap> factorTable = new FXDivisionTable<>();
  
  private final TableColumn<PropertyMap, Object> rootColumn   = new TableColumn<>("Реквизиты");
  private final Column<PropertyMap, Object>      nameColumn   = Column.create("Наименование","group_name");
  private final Column<PropertyMap, BigDecimal>  amountColumn = new Column("Кол.","amount", BigDecimal.ZERO);
  private final Column<PropertyMap, BigDecimal>  costColumn = new Column("Кол.","amount", BigDecimal.ZERO);
  
  private final ObservableList<String> hiddingColumns = FXCollections.observableArrayList();
  public BooleanProperty indentivicatorsVisible = new SimpleBooleanProperty(true);
  
  public ObservableList<EventHandler> factorChangeListeners = FXCollections.observableArrayList();
  
  private final ObservableList<Column<PropertyMap, ?>> customColumns = FXCollections.observableArrayList();
  
  public EquipmentFactorTable() {
    FXUtility.initCss(this);
    setCenter(factorTable);
    factorTable.setEditable(true);
    rootColumn.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> initHeader());
    
    hiddingColumns.addListener((ListChangeListener.Change<? extends String> c) -> {
      getColumns().stream().forEach(column -> {
        column.setVisible(true);
        column.setVisible(!hiddingColumns.contains(column.getText()));
      });
    });
  }

  public ObservableList<Column<PropertyMap, ?>> getCustomColumns() {
    return customColumns;
  }
  
  public BooleanProperty amountEditableProperty() {
    return amountColumn.editableProperty();
  }
  
  public void setColumnVisible(boolean vivsible, String... columns) {
    if(vivsible)
      hiddingColumns.removeAll(columns);
    else hiddingColumns.addAll(columns);
  }
  
  public void setIndentivicatorsVisible(boolean indentivicatorsVisible) {
    this.indentivicatorsVisible.setValue(indentivicatorsVisible);
  }
  
  public ObservableList<PropertyMap> getItems() {
    return factorTable.getSourceItems();
  }
  
  private Integer[] getFactors() {
    Integer[] factors = new Integer[0];
    for(PropertyMap item:factorTable.getSourceItems())
      if(item.getValue("factors") != null)
        for(Integer factor:(Integer[])item.getValue("factors"))
          if(!ArrayUtils.contains(factors, factor))
            factors = ArrayUtils.add(factors, factor);
    return factors;
  }
  
  private Integer[] getIdentificators() {
    Integer[] identificators = new Integer[0];
    for(PropertyMap item:factorTable.getSourceItems())
      if(item.getValue("iden_id") != null && !ArrayUtils.contains(identificators, item.getValue("iden_id")))
        identificators = ArrayUtils.add(identificators, (Integer)item.getValue("iden_id"));
    return identificators;
  }
  
  private ObservableList<TableColumn<PropertyMap, ?>> getColumns() {
    return rootColumn.isVisible() ? rootColumn.getColumns() : factorTable.getColumns();
  }
  
  public BooleanProperty nameColumnVisibleProperty() {
    return nameColumn.visibleProperty();
  }
  
  public BooleanProperty amountColumnVisibleProperty() {
    return amountColumn.visibleProperty();
  }
  
  public void initHeader() {
    clearHeader();
    Integer[] fids  = getFactors();
    Integer[] idens = getIdentificators();
    
    if(rootColumn.isVisible())
      factorTable.getColumns().add(rootColumn);
    
    if(nameColumn.isVisible())
      getColumns().add(nameColumn);
    
    if(amountColumn.isVisible())
      getColumns().add(amountColumn);
    
    getColumns().addAll(customColumns);
    
    for(int i=getColumns().size()-1;i>=0;i--) {
      TableColumn c = getColumns().get(i);
      if(c instanceof FactorColumn) {
        if(!ArrayUtils.contains(fids, ((FactorColumn)c).getFactorId()))
          getColumns().remove(i);
        else fids = ArrayUtils.removeElement(fids, ((FactorColumn)c).getFactorId());
      }
    }
    
    if(fids.length > 0) {
      ObjectLoader.getData("select id,name,factorType,listValues,[Factor(unique)] from [Factor] where id=ANY(?)", new Object[]{fids}).stream().forEach(d -> {
        FactorColumn column = null;
        
        for(TableColumn col:factorTable.getTableFilter().getFilters().keySet()) {
          if(col instanceof FactorColumn && ((FactorColumn) col).getFactorId() == (Integer)d.get(0)) {
            column = (FactorColumn) col;
          }
        }
        
        if(column == null) {
          column = new FactorColumn((String)d.get(1), (Integer)d.get(0), Factor.FactorType.valueOf((String) d.get(2)), 
                (String) d.get(3), ArrayUtils.contains(idens, (Integer)d.get(0)), factorChangeListeners);
          //factorTable.addFilter(column, new TextFilter("factor-"+d.get(0)));
        }
        
        column.setEditable(true);
        if(ArrayUtils.contains(idens, d.get(0)))
          getColumns().add(2, column);
        else getColumns().add(column);
      });
    }
    
    getColumns().stream().forEach(column -> {
      if(!column.visibleProperty().isBound()) {
        column.setVisible(true);
        column.setVisible(!hiddingColumns.contains(column.getText()));
      }
    });
  }
  
  public void addItems(PropertyMap... items) {
    factorTable.getSourceItems().addAll(items);
  }
  
  public void clear() {
    factorTable.getSourceItems().clear();
  }
  
  public void clearHeader() {
    rootColumn.getColumns().clear();
    factorTable.getColumns().clear();
  }
  
  public void setRootColumnVisible(boolean vivsible) {
    rootColumn.setVisible(vivsible);
  }

  public FXDivisionTable<PropertyMap> getTable() {
    return factorTable;
  }
  
  public class FactorColumn extends Column<PropertyMap, Object> {
    private final int factorId;
    private final Factor.FactorType factorType;
    private final String listValues;
    private final boolean identificator;
    private FactorCellEditableHandler factorCellEditableHandler = cell -> true;
    private ObservableList<EventHandler> factorChangeListeners;

    public FactorColumn(String text, int factorId, Factor.FactorType factorType, String listValues, boolean identificator, ObservableList<EventHandler> factorChangeListeners) {
      super(text, "factor-"+factorId, null, true, true, listValues == null ? null : (Object[])listValues.split(";"));
      this.factorId = factorId;
      this.factorType = factorType;
      this.listValues = listValues;
      this.identificator = identificator;
      this.factorChangeListeners = factorChangeListeners;
      
      if(isIdentificator()) {
        System.out.println("IDENTIFICATOR: "+text);
        visibleProperty().bind(indentivicatorsVisible);
      }
      
      if(listValues != null)
        getTable().addFilter(this, new ListFilter("factor-"+factorId));
      else if(factorType == Factor.FactorType.дата)
        getTable().addFilter(this, new DateFilter("factor-"+factorId));
      else getTable().addFilter(this, new TextFilter("factor-"+factorId));
      
      setCellFactory((TableColumn<PropertyMap, Object> param) -> new FactorCell(this));
    }

    public ObservableList<EventHandler> getFactorChangeListeners() {
      return factorChangeListeners;
    }

    public FactorCellEditableHandler getFactorCellEditableHandler() {
      return factorCellEditableHandler;
    }

    public void setFactorCellEditableHandler(FactorCellEditableHandler factorCellEditableHandler) {
      this.factorCellEditableHandler = factorCellEditableHandler;
    }

    public int getFactorId() {
      return factorId;
    }

    public Factor.FactorType getFactorType() {
      return factorType;
    }

    public String getListValues() {
      return listValues;
    }

    public boolean isIdentificator() {
      return identificator;
    }
  }
  
  public static class FactorCell extends FXDivisionTableCell {
    private FactorColumn column;
    
    public FactorCell(FactorColumn column) {
      super(column.listValues != null ? column.listValues.split(";") : null);
      this.column = column;
    }

    @Override
    public void commitEdit(Object newValue) {
      super.commitEdit(newValue);
      column.getFactorChangeListeners().stream().forEach(e -> e.handle(new Event(
              PropertyMap.create().setValue("object", (PropertyMap) getTableRow().getItem())
                      .setValue("column", ((Column)getTableColumn()))
                      .setValue("value", newValue), 
              this, EventType.ROOT)));
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
      super.updateItem(item, empty);
      
      setTooltip(getText() != null && !"".equals(getText()) ? new Tooltip(getText()) : null);
      
      getStyleClass().remove("disable-factor-cell");
      getStyleClass().remove("unique-column");
      
      if(getTableRow() != null && getTableRow().getItem() != null && column != null) {
        if(
                column.isIdentificator() && (((PropertyMap) getTableRow().getItem()).isNullOrEmpty("amount") || ((PropertyMap) getTableRow().getItem()).getValue("amount", BigDecimal.class).compareTo(BigDecimal.ONE) != 0) ||
                !ArrayUtils.contains((Integer[])((PropertyMap) getTableRow().getItem()).getValue("factors"), column.getFactorId()) || 
                !column.getFactorCellEditableHandler().isCellEditable(this)) {
          
          getStyleClass().add("disable-factor-cell");
          setEditable(false);
        }else setEditable(true);
        
        if(column.isIdentificator()) {
          getStyleClass().add("unique-column");
        }
      }
      //setContentDisplay(ContentDisplay.TEXT_ONLY);
      //setText((String)item);
    }
  }
  
  public interface FactorCellEditableHandler {
    public boolean isCellEditable(FactorCell cell);
  }
}