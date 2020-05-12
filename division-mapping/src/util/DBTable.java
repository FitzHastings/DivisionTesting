package util;

import bum.annotations.Procedure;
import bum.annotations.Procedures;
import bum.annotations.Trigger;
import bum.annotations.Triggers;
import bum.pool.DBController;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBTable extends Element {
  private String clientName;
  private Class annotatedClass;
  private Class interfacesClass;
  
  private DBColumn idColumn;
  private Hashtable<String, DBColumn>        columns      = new Hashtable<>();
  private TreeMap<String,Map<String,Object>> queryColumns = new TreeMap<>();
  private ArrayList<DBRelation>              relations    = new ArrayList<>();
  private String[][]                         unicumFields = new String[][]{};

  public DBTable(Class realClass, Class interfacesClass) {
    this.setName(AnnotationReader.getTableName(realClass,interfacesClass));
    this.clientName = AnnotationReader.getClientName(realClass, interfacesClass);
    
    this.annotatedClass  = realClass;
    this.interfacesClass = interfacesClass;
    
    if(interfacesClass.getSimpleName().equals("Comment")) {
      System.out.println("sgdf");
    }
    
    this.columns      = AnnotationReader.getColumns(realClass);
    this.queryColumns = AnnotationReader.getQyeryColumns(realClass);
    this.relations    = AnnotationReader.getRelations(realClass);
    this.unicumFields = AnnotationReader.getUnicumFields(realClass);

    for(DBColumn column:columns.values()) {
      if(column.isIdentify()) {
        idColumn = column;
        break;
      }
    }
  }

  public TreeMap<String, Map<String,Object>> getQueryColumns() {
    return queryColumns;
  }

  public String getColumnName(String fieldName) {
    DBColumn column = getColumn(fieldName);
    if(column != null)
      return column.getName();
    DBRelation relation = getRelation(fieldName);
    if(relation != null && relation instanceof util.ManyToOne)
      return ((util.ManyToOne)relation).getColumn().getName();
    if(queryColumns.containsKey(fieldName))
      return replaceString(queryColumns.get(fieldName).get("QUERY").toString());
    return null;
  }

  public Class getInterfacesClass() {
    return interfacesClass;
  }

  public Class getAnnotatedClass() {
    return annotatedClass;
  }

  public String getClientName() {
    return clientName;
  }
  
  public DBRelation getRelation(String fieldName) {
    for(DBRelation rel:relations) {
      if(rel.getField().getName().intern() == fieldName.intern())
        return rel;
    }
    return null;
  }
  
  public Collection<DBRelation> getRelations() {
    return relations;
  }
  
  public DBTable(Class objectClass) {
    this(objectClass,objectClass);
  }
  
  public DBColumn getIdColumn() {
    return this.idColumn;
  }
  
  public DBColumn getColumn(String fieldName) {
    for(DBColumn column:columns.values())
      if(fieldName.intern() == column.getField().getName().intern())
        return column;
    return null;
  }
  
  public DBColumn[] getColumns() {
    return columns.values().toArray(new DBColumn[columns.size()]);
  }
  
  @Override
  public void configure() {
    if(!isExist(getName()))
      create();
    else
      for(DBColumn column:columns.values())
        column.configure();
    if(!isConstraintExist(getName().concat("_id_tmp_unique")))
      createConstraint();
  }
  
  public void removeConstraints() {
    ResultSet rs = null;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      DatabaseMetaData meta = conn.getMetaData();
      rs = meta.getIndexInfo(conn.getCatalog(), null, getName(), true, true);
      System.out.println("GET INDEX INFO FOR TABLE \""+getName()+"\"");
      while(rs.next()) {
        //execute("ALTER TABLE "+getName()+" DROP CONSTRAINT IF EXISTS "+rs.getString("index_name")+" CASCADE", true);
        System.out.println("     "+rs.getString("index_name"));
        /*System.out.println("     ---------------------------------------");
        for(int i=1; i <= rs.getMetaData().getColumnCount(); i++) {
          System.out.println("     "+rs.getMetaData().getColumnName(i)+": "+rs.getObject(i));
        }*/
      }
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }finally {
      if(rs != null)
        try {rs.close();}catch(SQLException ex){logger.error("", ex);}
      if(conn != null)
        try {conn.close();}catch(SQLException ex){logger.error("", ex);}
    }
  }
  
  public void removeOldColumns() {
    ResultSet rs = null;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      DatabaseMetaData meta = conn.getMetaData();
      rs = meta.getColumns(System.getProperty("DB-NAME"), "%", getName(), "%");
      while(rs.next()) {
        String cn = rs.getString("COLUMN_NAME").toLowerCase();
        boolean remove = true;
        for(DBColumn c:getColumns()) {
          if(c.getName().toLowerCase().equals(cn)) {
            remove = false;
            break;
          }
        }
        if(remove) {
          for(DBRelation r:getRelations()) {
            DBColumn c = null;
            if(r instanceof ManyToOne)
              c = ((ManyToOne)r).getColumn();
            if(r instanceof OneToMany)
              c = ((OneToMany)r).getColumn();
            if(c!= null && c.getName().toLowerCase().equals(cn)) {
              remove = false;
              break;
            }
          }
        }
        if(remove) {
          logger.info("REMOVE COLUMN "+cn+" FROM "+getName());
          execute("ALTER TABLE "+getName()+" DROP COLUMN "+cn, true);
        }
      }
      conn.commit();
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }finally {
      if(rs != null)
        try {rs.close();}catch(SQLException ex){logger.error("", ex);}
      if(conn != null)
        try {conn.close();}catch(SQLException ex){logger.error("", ex);}
    }
  }

  private void createConstraint() {
    execute("ALTER TABLE ".concat(getName()).concat(" DROP CONSTRAINT IF EXISTS ").concat(getName()).concat("_id_tmp_unique"));
    execute("ALTER TABLE ".concat(getName()).concat(" ADD CONSTRAINT ").concat(getName()).concat("_id_tmp_unique UNIQUE (id,tmp)"));
  }

  public void configureIndexes() {
    String indexName;
    for(DBColumn column: getColumns()) {
      if(column.isIndex()) {
        indexName = getName().concat("_").concat(column.getName());
        //execute("DROP INDEX "+indexName);
        if(!isExistIndex(indexName))
          execute("CREATE ".concat((column.isUnique()?"UNIQUE ":"")).concat("INDEX ").concat(indexName).concat(" ON ").concat(getName()).concat("(").concat(column.getName()).concat(")"));
        else execute("REINDEX INDEX ".concat(indexName));
      }
    }
    String columnName;
    for(DBRelation relation:relations) {
      if(relation instanceof ManyToOne) {
        columnName = ((ManyToOne)relation).getColumn().getName();
        indexName = "index_"+relation.getObjectTable().getName()+"_"+columnName;
        //execute("DROP INDEX "+indexName);
        if(!isExistIndex(indexName))
          execute("CREATE INDEX "+indexName+" ON "+getName()+"("+columnName+")");
        else execute("REINDEX INDEX "+indexName);
      }else if(relation instanceof ManyToMany) {
        String objectColumnName = ((ManyToMany)relation).getObjectColumnName();
        String targetColumnName = ((ManyToMany)relation).getTargetColumnName();
        String objectIndexName = "index_"+relation.getRelationTableName()+"_"+objectColumnName;
        String targetIndexName = "index_"+relation.getRelationTableName()+"_"+targetColumnName;
        
        if(!isExistIndex(objectIndexName))
          execute("CREATE INDEX "+objectIndexName+" ON "+relation.getRelationTableName()+"("+objectColumnName+")");
        else execute("REINDEX INDEX "+objectIndexName);
        
        if(!isExistIndex(targetIndexName))
          execute("CREATE INDEX "+targetIndexName+" ON "+relation.getRelationTableName()+"("+targetColumnName+")");
        else execute("REINDEX INDEX "+targetIndexName);
      }
    }
    
    //execute("REINDEX TABLE "+getName());
  }
  
  public void configureUnicumFields() {
    if(getName().equals("product")) 
      System.out.println("product");
    String unicumName = "unicum";
    String fs = "";
    for(String[] fields:unicumFields) {
      String where = null;
      for(String field:fields) {
        if(!field.startsWith(":")) {
          String f = getColumnName(field);
          unicumName += "_"+f;
          fs += ","+f;
        }else where = field.substring(1);
      }
      execute("ALTER TABLE "+getName()+" DROP CONSTRAINT "+unicumName);
      if(where != null)
        execute("CREATE UNIQUE INDEX "+unicumName+" ON "+getName()+" ("+fs.substring(1)+") WHERE "+where);
      else execute("ALTER TABLE "+getName()+" ADD CONSTRAINT "+unicumName+" UNIQUE ("+fs.substring(1)+")");
    }
  }

  public boolean isExistIndex(String indexName) {
    boolean exist = false;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      DatabaseMetaData meta = conn.getMetaData();
      ResultSet rs = meta.getIndexInfo(System.getProperty("DB-NAME"),
        "%", getName(), false, true);
      while(rs.next()) {
        if(rs.getString(6).equals(indexName)) {
          exist = true;
          break;
        }
      }
      rs.close();
      conn.commit();
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }
    finally {
      try {
        if(conn != null)
          conn.close();
      }catch(SQLException ex) {
        logger.error("", ex);
      }
    }
    return exist;
  }
  
  public String getRelationTable(String fieldName) {
    DBRelation relation = getRelation(fieldName);
    if(relation != null) {
      return relation.getRelationTableName();
    }
    return null;
  }

  public String getTargetRelationColumnName(String fieldName) {
    DBRelation relation = getRelation(fieldName);
    return relation.getTargetColumnName();
  }

  public String getObjectRelationColumnName(String fieldName) {
    DBRelation relation = getRelation(fieldName);
    return relation.getObjectColumnName();
  }
  
  public static String replaceString(String string) {
    String query = string;
    Pattern p = Pattern.compile("(\\[[^\\[\\]]+\\])");
    Matcher m = p.matcher(string);
    String match,className,fieldName;
    int index;
    while(m.find()) {
      fieldName = null;
      match = m.group(1);
      int nowPos = query.toUpperCase().indexOf(match.toUpperCase());
      boolean notShema = query.toUpperCase().startsWith("INSERT") &&
              (nowPos < query.toUpperCase().indexOf("VALUES") || nowPos < query.toUpperCase().indexOf("SELECT")) ||
              
              query.toUpperCase().startsWith("UPDATE") && 
              nowPos > query.toUpperCase().indexOf(" SET ") && 
              (query.toUpperCase().indexOf(" WHERE") == -1 || nowPos < query.toUpperCase().lastIndexOf(" WHERE"));
      
      if(!notShema)
        notShema = match.indexOf("!") == 1;

      index = match.indexOf("(");
      className = match.substring(1, index==-1?match.length()-1:index).replaceAll("\\*", "").replaceAll("!", "").intern();
      if(index > -1)
        fieldName = match.substring(index+1, match.lastIndexOf(")")).intern();
      Class clazz = null;
      try {
        clazz = Class.forName(className);
      }catch(ClassNotFoundException e) {
        //e.printStackTrace();
      }
      if(clazz == null) {
        try {
          clazz = Class.forName("bum.interfaces."+className);
        }catch(Exception e){
          //e.printStackTrace();
        }
      }
      if(clazz != null) {
        DBTable table = DataBase.get(clazz);
        String name = table.getName();
        if(fieldName != null) {
          DBRelation relation = table.getRelation(fieldName);
          if(relation instanceof ManyToMany) {
            ManyToMany mtm = (ManyToMany)relation;
            if(match.indexOf(":") > 0) {
              switch(match.substring(1, match.length()-1).split(":")[1].intern()) {
                case "table":
                  name = mtm.getRelationTableName();
                  break;
                case "object":
                  name = notShema?mtm.getObjectColumnName():mtm.getRelationTableName()+"."+mtm.getObjectColumnName();
                  break;
                case "target":
                  name = notShema?mtm.getTargetColumnName():mtm.getRelationTableName()+"."+mtm.getTargetColumnName();
                  break;
              }
            }else {
              String alias  = "a"+System.currentTimeMillis();
              String aliasb = "b"+System.currentTimeMillis();
              name = "(SELECT array_agg("+mtm.getRelationTableName()+"."+mtm.getTargetColumnName()+")"
                      + " FROM "+mtm.getRelationTableName()
                      +" WHERE "+mtm.getRelationTableName()+"."+mtm.getObjectColumnName()+"="+name+".id AND "
                      +mtm.getRelationTableName()+"."+mtm.getTargetColumnName()+" in (SELECT "+alias+".id FROM "+mtm.getTargetTable().getName()+" "+alias
                      +" WHERE "+alias+".id in (SELECT "+aliasb+"."+mtm.getTargetColumnName()+" FROM "+mtm.getRelationTableName()+" "+aliasb+" WHERE "+aliasb+"."+mtm.getObjectColumnName()+"="+name+".id)"
                      + " AND tmp=false AND type='CURRENT'))";
            }
          }else if(relation instanceof OneToMany) {
            OneToMany otm = (OneToMany) relation;
            if(match.indexOf(":") > 0) {
              switch(match.substring(1, match.length()-1).split(":")[1].intern()) {
                case "table":
                  name = otm.getRelationTableName();
                  break;
                case "object":
                  name = notShema?otm.getTargetColumnName():otm.getRelationTableName()+"."+otm.getTargetColumnName();
                  break;
                case "target":
                  name = notShema?"id":otm.getRelationTableName()+".id";
                  break;
              }
            }else {
              String alias = "a"+System.currentTimeMillis();
              name = "(SELECT array_agg("+alias+"."+otm.getTargetTable().getIdColumn().getName()+
                      ") FROM "+otm.getRelationTableName()+" "+alias+
                      " WHERE tmp=false AND type='CURRENT' AND "+alias+"."+otm.getTargetColumnName()+
                      "="+name+".id)";
            }
          }else if(relation instanceof util.ManyToOne) {
            name = !notShema?name+"."+((util.ManyToOne)relation).getColumn().getName():((util.ManyToOne)relation).getColumn().getName();
          }else {
            if(match.indexOf("*") >= 0)
              name = !notShema?name+"."+fieldName:fieldName;
            else {
              DBColumn column = table.getColumn(fieldName);
              if(column != null)
                name = !notShema?name+"."+column.getName():column.getName();
              else if(table.getQueryColumns().containsKey(fieldName))
                name = replaceString(table.getQueryColumns().get(fieldName).get("QUERY").toString());//.substring(0, table.getQueryColumns().get(fieldName).get("QUERY").toString().lastIndexOf("AS")-1);
              else name = null;
            }
          }
        }
        if(name != null)
          query = query.replace(match, name);
      }
    }
    return query;
  }
  
  public void configureView() {
    for(DBRelation relation:getRelations()) {
      if(relation instanceof ManyToOne && ((ManyToOne)relation).getViewFields().length > 0) {
        String[] viewFields = ((ManyToOne)relation).getViewFields();
        for(int i=0;i<viewFields.length;i++) {
          TreeMap<String, Object> col = new TreeMap<>();
          col.put("DESCRIPTION", ((ManyToOne)relation).getViewNames()[i]);
          col.put("NAME", ((ManyToOne)relation).getViewNames()[i]);
          col.put("QUERY", "("+((ManyToOne)relation).getViewFieldQuery(viewFields[i])+")");// AS "+((ManyToOne)relation).getViewNames()[i]);
          queryColumns.put(((ManyToOne)relation).getViewNames()[i], col);
        }
      }
    }
    
    for(String name:queryColumns.keySet()) {
      Map<String,Object> col = queryColumns.get(name);
      col.put("QUERY", "("+replaceString(col.get("QUERY").toString())+")");// AS "+name);
    }
  }

  public void configureTriggers() throws ClassNotFoundException {
    
    try {
      execute("DROP TRIGGER IF EXISTS "+getName()+"_updateLastModification ON "+getName());
    }catch(Exception e) {
      logger.info(e.getMessage());
    }
    execute("CREATE TRIGGER "+getName()+"_updateLastModification BEFORE UPDATE"
                + "  ON "+getName()
                + "  FOR EACH ROW"
                + "  EXECUTE PROCEDURE updateLastModification()");
    
    Triggers ts = (Triggers)annotatedClass.getAnnotation(Triggers.class);
    if(ts != null) {
      for(Trigger trigger:ts.triggers()) {
        String actionTypes = "";
        for(Trigger.ACTIONTYPE actiontype:trigger.actionTypes())
          actionTypes += " OR "+actiontype;
        actionTypes = actionTypes.substring(4);

        String name = trigger.name();
        if(name.equals(""))
          name = getName()+"_"+trigger.timeType()+"_"+actionTypes.replaceAll(" OR ", "_");
        String linkProcedure = trigger.linkProcedure();
        String query;
        if(linkProcedure.equals("") && !trigger.procedureText().equals(""))
          linkProcedure = name;
        String n = trigger.classname().equals("")?getName():DataBase.get(trigger.classname()).getName();
        try {
          execute("DROP TRIGGER IF EXISTS "+name+" ON "+n);
        }catch(Exception e) {
          logger.info(e.getMessage());
        }
        query = "CREATE TRIGGER "+name
                + " "+trigger.timeType()+" "+actionTypes
                + "  ON "+n
                + "  FOR EACH ROW"
                + "  EXECUTE PROCEDURE "+linkProcedure+"()";
        execute(query);
      }
    }
  }
  
  public void configureTriggerFunctions() {
    Triggers ts = (Triggers)annotatedClass.getAnnotation(Triggers.class);
    if(ts != null) {
      for(Trigger trigger:ts.triggers()) {
        String actionTypes = "";
        for(Trigger.ACTIONTYPE actiontype:trigger.actionTypes())
          actionTypes += " OR "+actiontype;
        actionTypes = actionTypes.substring(4);

        String name = trigger.name();
        if(name.equals(""))
          name = getName()+"_"+trigger.timeType()+"_"+actionTypes.replaceAll(" OR ", "_");
        String linkProcedure = trigger.linkProcedure();
        String query;
        if(linkProcedure.equals("") && !trigger.procedureText().equals("")) {
          linkProcedure = name;
          query = "CREATE OR REPLACE FUNCTION "+linkProcedure+"() returns trigger AS $$ "+replaceString(trigger.procedureText())+" $$ LANGUAGE "+trigger.language();
          execute(query);
        }
      }
    }
  }

  public void configureProcedures() {
    Procedures ts = (Procedures)annotatedClass.getAnnotation(Procedures.class);
    if(ts != null) {
      for(Procedure procedure:ts.procedures()) {
        String name = procedure.name();
        if(name.equals(""))
          name = getName()+"_procedure_"+new Date().getTime();
        String query;
        if(!procedure.procedureText().equals("")) {
          String arguments = "";
          for(String argument:procedure.arguments())
            arguments += argument+",";
          arguments = arguments.equals("")?"":arguments.substring(0, arguments.length()-1);
          query = "CREATE OR REPLACE FUNCTION "+name+"("+arguments+") returns "+procedure.returnType()+" AS $$ "+replaceString(procedure.procedureText())+" $$ LANGUAGE "+procedure.language();
          try {
            execute("DROP FUNCTION "+name+"("+arguments+")");
          }catch(Exception e) {
            logger.info(e.getMessage());
          }
          execute(query);
        }
      }
    }
  }
  
  @Override
  public void create() {
    String CREATE_QUERY = this.getIdColumn()+",";
    for(DBColumn column:columns.values())
      if(!column.isIdentify())
        CREATE_QUERY += column+",";
    CREATE_QUERY = "CREATE TABLE "+getName()+"("+CREATE_QUERY+" CONSTRAINT "+getName()+"_id_tmp_unique UNIQUE (id,tmp))";
    execute(CREATE_QUERY);
  }
  
  @Override
  public void drop() {
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      PreparedStatement st = conn.prepareStatement("DROP TABLE "+getName());
      logger.info(st);
      st.execute();
      conn.commit();
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }
    finally {
      try {
        if(conn != null)
          conn.close();
      }catch(SQLException ex) {
        logger.error("", ex);
      }
    }
  }

  public static boolean isConstraintExist(String constraintName) {
    boolean exist = false;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery("SELECT conname FROM pg_constraint WHERE conname='"+constraintName+"'");
      while(rs.next())
        exist = true;
      rs.close();
      st.close();
      conn.commit();
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }
    finally {
      try{
        if(conn != null)
          conn.close();
      }catch(SQLException ex) {
        logger.error("", ex);
      }
    }
    return exist;
  }
  
  public static boolean isExist(String tableName) {
    boolean exist = false;
    Connection conn = null;
    try {
      conn = DBController.getConnection();
      exist = isExist(conn, tableName);
      conn.commit();
    }catch(SQLException ex) {
      try {
        if(conn != null)
          conn.rollback();
      }catch(SQLException e) {
        logger.error("", e);
      }
      logger.error("", ex);
    }
    finally {
      try{
        if(conn != null)
          conn.close();
      }catch(SQLException ex) {
        logger.error("", ex);
      }
    }
    return exist;
  }
  
  public static boolean isExist(Connection conn, String tableName) throws SQLException {
    boolean exist = false;
    DatabaseMetaData meta = conn.getMetaData();
    ResultSet rs = meta.getTables(System.getProperty("DB-NAME"),
            "%", tableName, new String[]{"TABLE"});
    while(rs.next())
      exist = true;
    rs.close();
    return exist;
  }
  
  @Override
  public boolean isExist() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}