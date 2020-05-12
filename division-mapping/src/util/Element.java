package util;

import bum.pool.DBController;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import org.apache.log4j.Logger;
import util.filter.local.DBExpretion.EqualType;

public abstract class Element {
  private String name;
  protected static Logger logger = Logger.getLogger(Element.class);

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public static void execute(String query) {
    execute(query, false);
  }
  
  public static void execute(String query, boolean autoCommit) {
    Connection conn = null;
    PreparedStatement st = null;
    try {
      conn = DBController.getConnection();
      conn.setAutoCommit(autoCommit);
      st = conn.prepareStatement(query);
      logger.debug(st);
      st.execute();
      if(!autoCommit)
        conn.commit();
    }catch(Exception ex) {
      if(!autoCommit && conn != null)
        try {conn.rollback();}catch(Exception e) {logger.error("", e);}
      logger.error(ex.getMessage());
    }finally {
      try{st.close();}catch(Exception t) {logger.error("", t);}
      try{conn.close();}catch(Exception t) {logger.error("", t);}
    }
  }
  
  public abstract void create();
  public abstract void drop();
  public abstract void configure();
  //public abstract void configure(Connection conn) throws Exception;
  public abstract boolean isExist();

  private static  String getInIds(String idName,Integer[] ids, EqualType type) {
    String query = "";
    for(Integer id:ids)
      query += id+",";
    return idName+(type == EqualType.IN?" IN ":" NOT IN ")+"("+query.substring(0, query.length()-1)+")";
  }

  public static  String getQuery(String idName, Integer[] ids, EqualType type) {
    if(ids == null || ids.length == 0)
      return "";
    String query = "";
    String inQuery = "";
    if(ids.length <= 30)
      return Element.getInIds(idName, ids, type);
    TreeSet<Integer> idsSet = new TreeSet<>(Arrays.asList(ids));
    List<TreeSet<Integer>> arrays = new ArrayList<>();
    Integer id,previosId;
    TreeSet<Integer> arr = null;
    Iterator<Integer> it = idsSet.iterator();
    while(it.hasNext()) {
      id = it.next();
      previosId = idsSet.lower(id);
      if(previosId == null || (id - previosId) > 1) {
        arr = new TreeSet<>();
        arrays.add(arr);
      }else arr.add(previosId);
      arr.add(id);
    }
    int count = 0;
    for(TreeSet<Integer> a:arrays) {
      if(a.size() <= 10)
        for(Integer id_:a)
          inQuery += id_+",";
      else {
        query += (type==EqualType.IN?" OR ":" AND NOT ")+idName+" BETWEEN "+a.first()+" AND "+a.last();
        count++;
      }
    }
    if(count > idsSet.size()/2)
      return Element.getInIds(idName, ids, type);

    if(!inQuery.equals(""))
      inQuery = (query.equals("")?"":(type==EqualType.IN?" OR ":" AND "))+idName+(type==EqualType.IN?" IN ":" NOT IN ")+"("+inQuery.substring(0,inQuery.length()-1)+")";
    if(!query.equals(""))
      query = (count>=1&&!inQuery.equals("")?"(":"")+query.substring(type==EqualType.IN?4:9)+inQuery+(count>=1&&!inQuery.equals("")?")":"");
    else query = inQuery;
    return query;
  }
}
