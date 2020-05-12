package util;

import bum.annotations.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import mapping.MappingObjectImpl;
import org.apache.commons.lang3.ArrayUtils;

public class AnnotationReader {
  
  public static String getTableName(Class objectClass, Class interfaceClass) {
    Annotation table = objectClass.getAnnotation(Table.class);
    if(table != null && ((Table)table).name().intern() != "null")
      return ((Table)table).name().toLowerCase();
    return interfaceClass.getSimpleName().toLowerCase();
  }

  public static String getViewFields(Class objectClass) {
    Table table = (Table)objectClass.getAnnotation(Table.class);
    if(table == null)
      return "";
    String query = "";
    for(String sql:table.viewFields())
      query += ", "+sql;
    return query;
  }
  
  public static String getClientName(Class objectClass, Class interfaceClass) {
    String name = null;
    Annotation table = objectClass.getAnnotation(Table.class);
    if(table != null && !((Table)table).clientName().equals("null"))
      name = ((Table)table).clientName();
    return name;
  }
  
  public static boolean isHistoryTable(Class objectClass) {
    Annotation table = objectClass.getAnnotation(Table.class);
    if(table != null)
      return ((Table)table).history();
    return false;
  }
  
  public static DBColumn getIdColumn(Class objectClass) {
    for(Field field:getFields(objectClass)) {
      Annotation id = field.getAnnotation(Id.class);
      if(id != null) {
        String name = ((Id)id).name();
        if(name.equals("null"))
          name = field.getName().toLowerCase();
        DBColumn idColumn = new DBColumn(objectClass, field, name, 0, null, null, true);
        idColumn.setDescription("Идентификатор");
        idColumn.setView(true);
        idColumn.setIndex(true);
        return idColumn;
      }
    }
    return null;
  }
  
  public static List<Field> getFields(Class clazz) {
    List<Field> fields = new ArrayList<>();
    fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
    while(clazz != MappingObjectImpl.class) {
      clazz = clazz.getSuperclass();
      fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
    }
    return fields;
  }
  
  public static Hashtable<String, DBColumn> getColumns(Class objectClass) {
    Hashtable<String, DBColumn> columns = new Hashtable<>();
    for(Field field:getFields(objectClass)) {
      Annotation column = field.getAnnotation(Column.class);
      if(column != null) {
        String name = ((Column)column).name();
        if(name.equals("null"))
          name = field.getName().toLowerCase();
        DBColumn dBColumn = new DBColumn(objectClass, field, name,
                ((Column)column).length(), 
                ((Column)column).sqlType(),
                ((Column)column).defaultValue().equals("null")?null:
                  ((Column)column).defaultValue(),
                false);
        dBColumn.setZip(((Column)column).zip());
        dBColumn.setGzip(((Column)column).gzip());
        dBColumn.setView(((Column)column).view());
        dBColumn.setNullAble(((Column)column).nullable());
        dBColumn.setDescription(((Column)column).description());
        dBColumn.setUnique(((Column)column).unique());
        dBColumn.setIndex(((Column)column).index());
        columns.put(name,dBColumn);
      }
    }
    DBColumn id = getIdColumn(objectClass);
    columns.put(id.getName(), id);
    return columns;
  }

  public static TreeMap<String, Map<String,Object>> getQyeryColumns(Class annotatedClass) {
    TreeMap<String, Map<String,Object>> columns = new TreeMap<>();
    Annotation table = annotatedClass.getAnnotation(Table.class);
    if(table != null) {
      for(QueryColumn column:((Table)table).queryColumns()) {
        TreeMap<String,Object> col = new TreeMap<>();
        col.put("DESCRIPTION", column.desctiption());
        col.put("NAME",        column.name());
        col.put("QUERY",       column.query());
        columns.put(column.name(), col);
      }
    }
    return columns;
  }
  
  public static String[][] getUnicumFields(Class annotatedClass) {
    String[][] unicumFields = new String[][]{};
    Annotation table = annotatedClass.getAnnotation(Table.class);
    if(table != null) {
      for(UnicumFields fields:((Table)table).unicumFields()) {
        String[] ff = fields.fields();
        if(!fields.where().equals(""))
          ff = ArrayUtils.add(ff, ":"+fields.where());
        unicumFields = (String[][]) ArrayUtils.add(unicumFields, ff);
      }
    }
    return unicumFields;
  }
  
  public static ArrayList<DBRelation> getRelations(Class objectClass){
    ArrayList<DBRelation> relationships = new ArrayList<>();
    for(Field field:getFields(objectClass)) {
      Annotation relation = field.getAnnotation(bum.annotations.ManyToOne.class);
      if(relation == null)
        relation = field.getAnnotation(bum.annotations.ManyToMany.class);
      if(relation == null)
        relation = field.getAnnotation(bum.annotations.OneToMany.class);
      
      if(relation != null) {
        DBRelation relationship = getRelation(objectClass, field, relation);
        if(relationship != null)
          relationships.add(relationship);
      }
    }
    return relationships;
  }
  
  private static DBRelation getRelation(Class objectClass, Field field, Annotation relation){
    DBRelation rel = null;
    String mappedBy = "";
    if(relation instanceof bum.annotations.ManyToOne) {
      rel = new util.ManyToOne(objectClass, getFieldGenericClass(field), field);
      ((util.ManyToOne)rel).setOn_delete(((bum.annotations.ManyToOne)relation).on_delete());
      ((util.ManyToOne)rel).setOn_update(((bum.annotations.ManyToOne)relation).on_update());
      ((util.ManyToOne)rel).setViewFields(((bum.annotations.ManyToOne)relation).viewFields());
      ((util.ManyToOne)rel).setViewNames(((bum.annotations.ManyToOne)relation).viewNames());
      ((util.ManyToOne)rel).setNullAble(((bum.annotations.ManyToOne)relation).nullable());
      ((util.ManyToOne)rel).setSaveNow(((bum.annotations.ManyToOne)relation).saveNow());
      ((util.ManyToOne)rel).setDescription(((bum.annotations.ManyToOne)relation).description());
      mappedBy = ((bum.annotations.ManyToOne)relation).mappedBy();
    }
    if(relation instanceof bum.annotations.OneToMany) {
      rel = new util.OneToMany(objectClass, getFieldGenericClass(field), field);
      ((util.OneToMany)rel).setOn_delete(((bum.annotations.OneToMany)relation).on_delete());
      ((util.OneToMany)rel).setOn_update(((bum.annotations.OneToMany)relation).on_update());
      ((util.OneToMany)rel).setOrderBy(((bum.annotations.OneToMany)relation).orderBy());
      ((util.OneToMany)rel).setDescription(((bum.annotations.OneToMany)relation).description());
      mappedBy = ((bum.annotations.OneToMany)relation).mappedBy();
    }
    if(relation instanceof bum.annotations.ManyToMany) {
      rel = new util.ManyToMany(objectClass, getFieldGenericClass(field), field);
      ((util.ManyToMany)rel).setOn_delete(((bum.annotations.ManyToMany)relation).on_delete());
      ((util.ManyToMany)rel).setOn_update(((bum.annotations.ManyToMany)relation).on_update());
      ((util.ManyToMany)rel).setOrderBy(((bum.annotations.ManyToMany)relation).orderBy());
      ((util.ManyToMany)rel).setDescription(((bum.annotations.ManyToMany)relation).description());
      mappedBy = ((bum.annotations.ManyToMany)relation).mappedBy();
    }
    rel.setName(mappedBy.equals("") ? field.getName().toLowerCase() : mappedBy.toLowerCase());
    rel.setMappedBy(mappedBy.equals("") ? null : mappedBy);
    return rel;
  }
  
  public static Class<?> getFieldGenericClass(Field field) {
    Class<?> clazz = null;
    if(field.getGenericType() instanceof ParameterizedType) {
      ParameterizedType t = (ParameterizedType)field.getGenericType();
      Type[] tt = t.getActualTypeArguments();
      if(tt.length > 0 && tt[0] instanceof Class<?>)
        clazz = (Class<?>)tt[0];
    }else clazz = field.getType();
    return clazz;
  }
}