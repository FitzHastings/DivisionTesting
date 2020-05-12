package division.fx.table;

import division.fx.PropertyMap;
import division.fx.tree.TreeColumn;
import javafx.collections.ObservableList;

public interface FXColumn<RowType extends PropertyMap, S> {
  //public BooleanProperty createKeyIfNotExistProperty();
  //public BooleanProperty columnNameKeyProperty();
  
  public default void setValue(RowType row, S value) {
    /*if(columnNameKeyProperty().getValue() && getColumnName() != null && (row.containsKey(getColumnName()) || createKeyIfNotExistProperty().getValue()))
      row.setValue(getColumnName(), value);
    
    if(!columnNameKeyProperty().getValue() && getDatabaseColumnName() != null && (row.containsKey(getDatabaseColumnName()) || createKeyIfNotExistProperty().getValue()))
      row.setValue(getDatabaseColumnName(), value);*/
    
    if(getDatabaseColumnName() != null/* && row.containsKey(getDatabaseColumnName())*/)
      row.setValue(getDatabaseColumnName(), value);
    else if(getColumnName() != null/* && row.containsKey(getColumnName())*/)
      row.setValue(getColumnName(), value);
  }
  
  /*public default void setValue(RowType row, S value) {
    setValue(row, value, true, false);
  }*/
  
  public default S getValue(RowType row) {
    if(getColumnName() != null && row.containsKey(getColumnName()))
      return (S)row.getValue(getColumnName());
    if(getDatabaseColumnName() != null && row.containsKey(getDatabaseColumnName()))
      return (S)row.getValue(getDatabaseColumnName());
    return (S)null;
  }
  
  public default void unbind(RowType row) {
    String key = null;
    if(row!= null && getColumnName() != null && row.containsKey(getColumnName()))
      key = getColumnName();
    if(row!= null && getDatabaseColumnName() != null && row.containsKey(getDatabaseColumnName()))
      key = getDatabaseColumnName();
    if(key != null)
      row.get(key).unbind();
  }
  
  public default FXColumn<RowType, S> getColumn(String columnName) {
    return getColumn(getColumns(), columnName);
  }
  
  public default FXColumn<RowType, S> getColumn(ObservableList columns, String columnName) {
    FXColumn<RowType, S> returnColumn = null;
    for(Object c:columns) {
      if(columnName.equals(((TreeColumn)c).getColumnName()))
        returnColumn = (TreeColumn)c;
      else returnColumn = getColumn(((FXColumn)c).getColumns(), columnName);
      if(returnColumn != null)
        break;
    }
    return returnColumn;
  }
  
  public default void setDefault(RowType row) {
    setValue(row, getDefaultValue());
  }

  public String getColumnName();

  public String getDatabaseColumnName();
  public void setDatabaseColumnName(String databaseColumnName);

  public ObservableList getColumns();

  public S getDefaultValue();
}