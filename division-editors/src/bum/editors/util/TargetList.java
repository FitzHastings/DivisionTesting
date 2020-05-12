package bum.editors.util;

import bum.editors.util.api.Transaction;
import division.fx.PropertyMap;
import division.fx.util.MsgTrash;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class TargetList {
  private final DBFilter filter;
  private final DivisionTarget target;
  private final ObservableList<PropertyMap> items = FXCollections.observableArrayList();

  private TargetList(DBFilter filter) {
    this.filter = filter;
    
    target = DivisionTarget.create(filter.getTargetClass(), (DivisionTarget target, String type, Integer[] ids, PropertyMap objectEventProperty) -> {
      switch(type) {
        case "CREATE":
          create(ids);
          break;
        case "UPDATE":
          update(ids);
          break;
        case "REMOVE":
          remove(ids);
          break;
      }
    });
    
    
  }
  
  public static ObservableList<PropertyMap> create(DBFilter filter) {
    return new TargetList(filter).getItems();
  }
  
  public ObservableList<PropertyMap> getItems() {
    return items;
  }

  private void update(Integer[] ids) {
    try {
      Integer[] updateIds = ObjectLoader.isSatisfy(filter, ids);
      
      if(updateIds.length > 0)
        Transaction.create().getList(filter.getTargetClass(), updateIds).stream().forEach(item -> items.filtered(it -> it.getInteger("id").equals(((PropertyMap)item).getInteger("id"))).forEach(it -> it.copyFrom(((PropertyMap)item).stayOnly(it.keySet().toArray()))));
      
      for(Integer id:updateIds)
        ids = ArrayUtils.removeElement(ids, id);
      
      if(ids.length > 0)
        remove(ids);
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }

  private void create(Integer[] ids) {
    try {
      Integer[] createdIds = ObjectLoader.isSatisfy(filter, ids);
      if(createdIds.length > 0)
        Transaction.create().getList(filter.getTargetClass(), createdIds).stream().forEach(it -> items.add(items.isEmpty() ? (PropertyMap)it : ((PropertyMap)it).stayOnly(items.get(0).keySet().toArray())));
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
  }

  private void remove(Integer[] ids) {
    for(int i=items.size()-1;i>=0;i--)
      if(ArrayUtils.contains(ids, items.get(i).getInteger("id")))
        items.remove(i);
  }
}