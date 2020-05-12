package bum.editors.util.api;

import bum.editors.util.ObjectLoader;
import division.fx.PropertyMap;
import division.util.GzipUtil;
import java.util.ArrayList;
import java.util.Map;
import javafx.collections.ObservableList;
import util.RemoteSession;
import util.filter.local.DBFilter;

public class Transaction implements DivisionServerCommunications {
  private RemoteSession session;
  
  private Transaction() {}
  
  public static Transaction create() {
    return new Transaction();
  }
  
  @Override
  public Transaction start() {
    session = ObjectLoader.createSession();
    return this;
  }
  
  @Override
  public Transaction stop() {
    ObjectLoader.commitSession(session);
    session = null;
    return this;
  }
  
  @Override
  public Transaction cancel() {
    ObjectLoader.rollBackSession(session);
    session = null;
    return this;
  }
  
  private RemoteSession session() throws Exception {
    if(session == null)
      return ObjectLoader.createSession(true, true);
    else return session;
  }

  @Override
  public Transaction fillList(DBFilter filter, ObservableList<PropertyMap> list, String... fields) throws Exception {
    list.clear();
    GzipUtil.getObjectFromGzip(session().getGZipList(filter, fields), new ArrayList<Map>()).stream().forEach(map -> list.add(PropertyMap.copy(map)));
    return this;
  }
}