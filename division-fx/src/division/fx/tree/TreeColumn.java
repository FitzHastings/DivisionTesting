package division.fx.tree;

import division.fx.PropertyMap;
import division.fx.table.FXColumn;
import division.fx.table.filter.ColumnFilter;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import javafx.util.StringConverter;

public class TreeColumn<RowType extends PropertyMap, S> extends TreeTableColumn<RowType, S> implements FXColumn<RowType, S> {
  private String columnName;
  private String databaseColumnName;
  private ColumnFilter columnFilter;
  private S defaultValue = null;
    
  public TreeColumn(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, null, null, new String[0]);
  }
  
  public TreeColumn(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, defaultValue, null, new String[0]);
  }
  
  public TreeColumn(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, String... val) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, null, null, val);
  }
  
  public TreeColumn(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue, String... val) {
    this(columnName, databaseColumnName, columnFilter, visible, editable, defaultValue, null, val);
  }
  
  public TreeColumn(String columnName, String databaseColumnName, ColumnFilter columnFilter, boolean visible, boolean editable, S defaultValue, StringConverter customConverter, String... val) {
    super(columnName);
    this.columnName = columnName;
    this.databaseColumnName = databaseColumnName;
    this.columnFilter = columnFilter;
    if(columnFilter != null && columnFilter.getProperty() == null)
      columnFilter.setProperty(getDatabaseColumnName() != null ? getDatabaseColumnName() : getColumnName());
    this.defaultValue = defaultValue;
    
    setCellValueFactory((CellDataFeatures<RowType, S> param) -> {
      return getColumnName() != null && param.getValue().getValue() != null && param.getValue().getValue().containsKey(getColumnName()) ? 
              param.getValue().getValue().get(getColumnName()) :
              getDatabaseColumnName() != null && param.getValue().getValue() != null && param.getValue().getValue().containsKey(getDatabaseColumnName()) ?
              param.getValue().getValue().get(getDatabaseColumnName()) :
              new SimpleObjectProperty(null);
    });
    ((TreeTableColumn)this).setCellFactory((Object param) -> customConverter == null ? new FXDivisionTreeTableCell(val) : new FXDivisionTreeTableCell(customConverter));
    
    setVisible(visible);
    getStyleClass().add(this.databaseColumnName);
    setEditable(editable);
  }

  

  public ColumnFilter getColumnFilter() {
    return columnFilter;
  }

  @Override
  public String getColumnName() {
    return columnName;
  }

  @Override
  public String getDatabaseColumnName() {
    return databaseColumnName;
  }

  @Override
  public S getDefaultValue() {
    return defaultValue;
  }
  
  
  
  public TreeColumn addColumn(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter) {
    return addColumn(new TreeColumn(columnName, databaseColumnName, columnFilter, true, false));
  }
  
  public TreeColumn addColumn(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible) {
    return addColumn(new TreeColumn(columnName, databaseColumnName, columnFilter, visible, false));
  }
  
  public TreeColumn addColumn(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible, boolean editable) {
    return addColumn(new TreeColumn(columnName, databaseColumnName, columnFilter, visible, editable));
  }
  
  public TreeColumn addColumn(String columnName, String databaseColumnName) {
    return addColumn(new TreeColumn(columnName, databaseColumnName, null, true, false));
  }
  
  public TreeColumn addColumn(String columnName, String databaseColumnName, boolean visible) {
    return addColumn(new TreeColumn(columnName, databaseColumnName, null, visible, false));
  }
  
  public TreeColumn addColumn(String columnName, String databaseColumnName, boolean visible, boolean editable) {
    return addColumn(new TreeColumn(columnName, databaseColumnName, null, visible, editable));
  }
  
  public TreeColumn addColumn(String columnName) {
    return addColumn(new TreeColumn(columnName, null, null, true, false));
  }
  
  public TreeColumn addColumn(String columnName, boolean visible) {
    return addColumn(new TreeColumn(columnName, null, null, visible, false));
  }
  
  public TreeColumn addColumn(String columnName, boolean visible, boolean editable) {
    return addColumn(new TreeColumn(columnName, null, null, visible, editable));
  }
  
  public TreeColumn addColumn(TreeColumn column) {
    getColumns().add(column);
    return (TreeColumn<PropertyMap, S>)this;
  }
  
  public TreeColumn addColumns(TreeColumn... columns) {
    this.getColumns().addAll(columns);
    return (TreeColumn<PropertyMap, S>)this;
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName) {
    return new TreeColumn(columnName, databaseColumnName, null, true, false);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, false);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object defaultColumn) {
    if(defaultColumn instanceof StringConverter)
      return create(columnName, databaseColumnName, visible, editable, (StringConverter)defaultColumn);
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable, defaultColumn);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object defaultColumn, String... val) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable, defaultColumn, val);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable, String... val) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable, val);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable, StringConverter customConverter) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable, null, customConverter);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable, StringConverter customConverter, String... val) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable, null, customConverter, val);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, boolean visible, boolean editable, Object defaultColumn, StringConverter customConverter, String... val) {
    return new TreeColumn(columnName, databaseColumnName, null, visible, editable, defaultColumn, customConverter, val);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter) {
    return new TreeColumn(columnName, databaseColumnName, columnFilter, true, false);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible) {
    return new TreeColumn(columnName, databaseColumnName, columnFilter, visible, false);
  }
  
  public static TreeColumn create(String columnName, String databaseColumnName, division.fx.table.filter.ColumnFilter columnFilter, boolean visible, boolean editable) {
    return new TreeColumn(columnName,  databaseColumnName, columnFilter, visible, editable);
  }

  public static TreeColumn create(String columnName) {
    return new TreeColumn(columnName, null, null, true, false);
  }
  
  public static TreeColumn create(String columnName, division.fx.table.filter.ColumnFilter columnFilter) {
    return new TreeColumn(columnName, null, columnFilter, true, false);
  }
  
  public static TreeColumn create(String columnName, boolean visible) {
    return new TreeColumn(columnName, null, null, visible, false);
  }
  
  public static TreeColumn create(String columnName, boolean visible, boolean editable) {
    return new TreeColumn(columnName, null, null, visible, editable);
  }

  /*@Override
  public BooleanProperty createKeyIfNotExistProperty() {
    return createKeyIfNotExist;
  }

  @Override
  public BooleanProperty columnNameKeyProperty() {
    return columnNameKey;
  }*/

  @Override
  public void setDatabaseColumnName(String databaseColumnName) {
    this.databaseColumnName = databaseColumnName;
  }
}
