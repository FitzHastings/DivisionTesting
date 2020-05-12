package division.fx.table;

import division.fx.PropertyMap;
import division.fx.table.filter.ColumnFilter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

public class Column<RowType extends PropertyMap, S> extends TableColumn<RowType, S> implements FXColumn<RowType, S> {
  private String columnName;
  private String databaseColumnName;
  private ColumnFilter columnFilter;
  private S defaultValue = null;
  private ObservableList<Object> choisevalues = FXCollections.observableArrayList();
  private ObjectProperty<StringConverter> converterProperty = new SimpleObjectProperty<>();
  
  //private BooleanProperty createKeyIfNotExist = new SimpleBooleanProperty(true);
  //private BooleanProperty columnNameKey = new SimpleBooleanProperty(false);
    
  public Column(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, null, null, new Object[0]);
  }
  
  public Column(String columnName, S defaultValue) {
    this(columnName, null, null, true, false, defaultValue, null, new Object[0]);
  }
  
  public Column(String columnName, String databaseColumnName, S defaultValue) {
    this(columnName, databaseColumnName, null, true, false, defaultValue, null, new Object[0]);
  }
  
  public Column(String columnName, boolean visible, boolean editable, S defaultValue) {
    this(columnName, null, null, visible, editable, defaultValue, null, new Object[0]);
  }
  
  public Column(String columnName, ColumnFilter columnFilter, S defaultValue) {
    this(columnName, null, columnFilter, true, false, defaultValue, null, new Object[0]);
  }
  
  public Column(String columnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue) {
    this(columnName, null, columnFilter, visible, editable, defaultValue, null, new Object[0]);
  }
  
  public Column(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, defaultValue, null, new Object[0]);
  }
  
  public Column(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, Object... val) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, null, null, val);
  }
  
  public Column(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue, Object... val) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, defaultValue, null, val);
  }
  
  public Column(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue, StringConverter customConverter, Object... val) {
    super(columnName);
    this.columnName = columnName;
    this.columnFilter = columnFilter;
    setDatabaseColumnName(databaseColumnName);
    
    if(columnFilter != null && columnFilter.getProperty() == null)
      columnFilter.setProperty(getDatabaseColumnName() != null ? getDatabaseColumnName() : getColumnName());
    this.defaultValue = defaultValue;
    
    choisevalues.addListener((ListChangeListener.Change<? extends Object> c) -> ((TableColumn)this).setCellFactory((Object param) -> 
            new FXDivisionTableCell(converterProperty.getValue(), choisevalues.toArray())));
    
    converterProperty.addListener((ObservableValue<? extends StringConverter> observable, StringConverter oldValue, StringConverter newValue) -> ((TableColumn)this).setCellFactory((Object param) -> 
            new FXDivisionTableCell(newValue, choisevalues)));
    
    ((TableColumn)this).setCellFactory((Object param) -> 
            new FXDivisionTableCell());
    
    choisevalues.setAll(val == null ? new Object[0] : val);
    converterProperty.setValue(customConverter);
    
    setVisible(visible);
    getStyleClass().add(this.databaseColumnName);
    setEditable(editable);
  }

  public ObservableList<Object> choisevalues() {
    return choisevalues;
  }

  public ObjectProperty<StringConverter> converterProperty() {
    return converterProperty;
  }

  public ColumnFilter getColumnFilter() {
    return columnFilter;
  }

  @Override
  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public final String getDatabaseColumnName() {
    return databaseColumnName;
  }
  
  @Override
  public final void setDatabaseColumnName(String databaseColumnName) {
    this.databaseColumnName = databaseColumnName;
    String dbc_time = databaseColumnName;
    if(dbc_time != null) {
      if(dbc_time.contains("=query:"))
        dbc_time = dbc_time.substring(0, dbc_time.indexOf("=query:"));
      if(dbc_time.contains(":=:"))
        dbc_time = dbc_time.substring(0, dbc_time.indexOf(":=:"));
    }
    
    final String dbc = dbc_time;
    
    setCellValueFactory((CellDataFeatures<RowType, S> param) -> {
      PropertyMap row = param.getValue();
      
      if(getDatabaseColumnName() != null) {
        if(!row.containsKey(getDatabaseColumnName()) && getDefaultValue() != null)
          param.getValue().setValue(getDatabaseColumnName(), getDefaultValue());
      }else if(getColumnName() != null && !row.containsKey(getColumnName()) && getDefaultValue() != null)
        param.getValue().setValue(getColumnName(), getDefaultValue());
      
      return 
              dbc != null && row.containsKey(dbc) ? row.get(dbc) :
              getColumnName() != null && row.containsKey(getColumnName()) ? row.get(getColumnName()) :
              getDatabaseColumnName() != null && row.containsKey(getDatabaseColumnName()) ? row.get(getDatabaseColumnName()) :
              new SimpleObjectProperty(null);
    });
  }

  @Override
  public S getDefaultValue() {
    return defaultValue;
  }
  
  public Column addColumn(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter) {
    return addColumn(new Column(columnName, databaseColumnName, columnFilter, true, false));
  }
  
  public Column addColumn(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible) {
    return addColumn(new Column(columnName, databaseColumnName, columnFilter, visible, false));
  }
  
  public Column addColumn(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible, boolean editable) {
    return addColumn(new Column(columnName, databaseColumnName, columnFilter, visible, editable));
  }
  
  public Column addColumn(String columnName, String databaseColumnName) {
    return addColumn(new Column(columnName, databaseColumnName, null, true, false));
  }
  
  public Column addColumn(String columnName, String databaseColumnName, boolean visible) {
    return addColumn(new Column(columnName, databaseColumnName, null, visible, false));
  }
  
  public Column addColumn(String columnName, String databaseColumnName, boolean visible, boolean editable) {
    return addColumn(new Column(columnName, databaseColumnName, null, visible, editable));
  }
  
  public Column addColumn(String columnName) {
    return addColumn(new Column(columnName, null, null, true, false));
  }
  
  public Column addColumn(String columnName, boolean visible) {
    return addColumn(new Column(columnName, null, null, visible, false));
  }
  
  public Column addColumn(String columnName, boolean visible, boolean editable) {
    return addColumn(new Column(columnName, null, null, visible, editable));
  }
  
  public Column addColumn(Column column) {
    getColumns().add(column);
    return (Column<PropertyMap, S>)this;
  }
  
  public Column addColumns(Column... columns) {
    this.getColumns().addAll(columns);
    return (Column<PropertyMap, S>)this;
  }
  
  public static Column create(String columnName, String databaseColumnName) {
    return new Column(columnName, databaseColumnName, null, true, false);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible) {
    return new Column(columnName, databaseColumnName, null, visible, false);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable) {
    return new Column(columnName, databaseColumnName, null, visible, editable);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object defaultColumn) {
    if(defaultColumn instanceof StringConverter)
      return create(columnName, databaseColumnName, visible, editable, (StringConverter)defaultColumn);
    return new Column(columnName, databaseColumnName, null, visible, editable, defaultColumn);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object defaultColumn, Object... val) {
    return new Column(columnName, databaseColumnName, null, visible, editable, defaultColumn, val);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object... val) {
    return new Column(columnName, databaseColumnName, null, visible, editable, val);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable, StringConverter customConverter) {
    return new Column(columnName, databaseColumnName, null, visible, editable, null, customConverter);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable, StringConverter customConverter, Object... val) {
    return new Column(columnName, databaseColumnName, null, visible, editable, null, customConverter, val);
  }
  
  public static Column create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object defaultColumn, StringConverter customConverter, Object... val) {
    return new Column(columnName, databaseColumnName, null, visible, editable, defaultColumn, customConverter, val);
  }
  
  public static Column create(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter) {
    return new Column(columnName, databaseColumnName, columnFilter, true, false);
  }
  
  public static Column create(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible) {
    return new Column(columnName, databaseColumnName, columnFilter, visible, false);
  }
  
  public static Column create(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible, boolean editable) {
    return new Column(columnName,  databaseColumnName, columnFilter, visible, editable);
  }

  public static Column create(String columnName) {
    return new Column(columnName, null, null, true, false);
  }
  
  public static Column create(String columnName, division.fx.table.filter.ColumnFilter columnFilter) {
    return new Column(columnName, null, columnFilter, true, false);
  }
  
  public static Column create(String columnName, boolean visible) {
    return new Column(columnName, null, null, visible, false);
  }
  
  public static Column create(String columnName, boolean visible, boolean editable) {
    return new Column(columnName, null, null, visible, editable);
  }
  
  public static Column create(String columnName, boolean visible, boolean editable, Object... val) {
    return new Column(columnName, null, null, visible, editable, val);
  }

  /*@Override
  public BooleanProperty createKeyIfNotExistProperty() {
    return createKeyIfNotExist;
  }

  @Override
  public BooleanProperty columnNameKeyProperty() {
    return columnNameKey;
  }*/
}
