package bum.editors.util.api;

import division.fx.PropertyMap;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public interface DivisionServerCommunications {
  
  public DivisionServerCommunications start();
  public DivisionServerCommunications stop();
  public DivisionServerCommunications cancel();
  
  public default ObservableList<PropertyMap> getList(DBFilter filter, String... fields) throws Exception {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    fillList(filter, list, fields);
    return list;
  }
  
  public default ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, Integer[] ids, String... fields) throws Exception {
    return getList(DBFilter.create(objectClass).AND_IN("id", ids), fields);
  }
  
  public default ObservableList<PropertyMap> getList(Class<? extends MappingObject> objectClass, List<Integer> ids, String... fields) throws Exception {
    return getList(objectClass, ids.toArray(new Integer[0]), fields);
  }
  
  public default PropertyMap getMap(Class<? extends MappingObject> cl, Integer id, String... fields) throws Exception {
    ObservableList<PropertyMap> list = getList(DBFilter.create(cl).AND_EQUAL("id", id), fields);
    return list.isEmpty() ? null : list.get(0);
  }
  
  public DivisionServerCommunications fillList(DBFilter filter, ObservableList<PropertyMap> list, String... fields) throws Exception;
}