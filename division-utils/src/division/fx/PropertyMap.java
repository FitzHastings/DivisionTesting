package division.fx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import division.util.Utility;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

public interface PropertyMap extends Serializable {
  public ObjectProperty<LocalDateTime> lastupdate();
  
  public BooleanProperty printDifferenceProperty();
  
  public Map<String, Property> getMap();
  
  public default ObservableList<EventHandler<Event>> getAddListeners() {
    return FXCollections.observableArrayList();
  }
  
  public default ObservableList<EventHandler<Event>> getDellListeners() {
    return FXCollections.observableArrayList();
  }
  
  public ObservableList<String> equalKeys();
  public ObservableList<String> notEqualKeys();
  
  public default PropertyMap toStringProperty(String propertyName) {
    return this;
  };
  
  public static PropertyMap copy(PropertyMap map, String... keys) {
    return PropertyMap.copy(map.getSimpleMap(true, keys)).equalKeys(map.equalKeys()).notEqualKeys(map.notEqualKeys());
  }
  
  public static PropertyMap copy(PropertyMap map) {
    if(map != null)
      return PropertyMap.copy(map.getSimpleMap(true)).equalKeys(map.equalKeys()).notEqualKeys(map.notEqualKeys());
    return PropertyMap.create();
  }
  
  public default PropertyMap copy() {
    return PropertyMap.copy(this);
  }
  
  public static PropertyMap copy(Map map) {
    PropertyMap object = PropertyMap.create();
    if(map != null) {
      map.keySet().stream().forEach((key) -> {
        if(map.get(key) instanceof PropertyMap)
          object.setValue(String.valueOf(key), PropertyMap.copy((PropertyMap)map.get(key)));
        /*else if(map.get(key) instanceof Map)
          object.setValue(String.valueOf(key), PropertyMap.copy((Map)map.get(key)));
        else */if(map.get(key) instanceof Collection) {
          object.setValue(String.valueOf(key),  map.get(key).getClass().cast(PropertyMap.copyList((Collection)map.get(key))));
        }else object.setValue((String)key, map.get(key));
      });
    }
    return object;
  }
  
  public static <T extends Collection> T copyList(T l) {
    try {
      T list = (T)(l instanceof ObservableList ? FXCollections.observableArrayList() : l.getClass().newInstance());
      l.stream().forEach(i -> {
        if(i instanceof Collection)
          list.add(PropertyMap.copyList((Collection)i));
        else if(i instanceof PropertyMap)
          list.add(PropertyMap.copy((PropertyMap)i));
        else list.add(i);
      });
      return list;
    }catch(InstantiationException | IllegalAccessException ex) {
      ex.printStackTrace();
    }
    return null;
  }
  
  public static PropertyMap create() {
    PropertyMap p = new PropertyMap() {
      private final Map<String, Property> map = new TreeMap<>();
      private final ObservableList<EventHandler<Event>> addlisteners = FXCollections.observableArrayList();
      private final ObservableList<EventHandler<Event>> dellisteners = FXCollections.observableArrayList();
      private final ObservableList<String> equalKeys = FXCollections.observableArrayList();
      private final ObservableList<String> notEqualKeys = FXCollections.observableArrayList();
      private String toStringPropertyName = "name";
      private final ObjectProperty<LocalDateTime> lastupdate = new SimpleObjectProperty<>(LocalDateTime.now());

      @Override
      public ObservableList<String> equalKeys() {
        return equalKeys;
      }
      
      @Override
      public ObservableList<String> notEqualKeys() {
        return notEqualKeys;
      }

      @Override
      public int hashCode() {
        return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public boolean equals(Object obj) {
        if(!(obj instanceof PropertyMap))
          return false;

        Set<String> tkeys = new CopyOnWriteArraySet<>(keySet());
        tkeys.addAll(((PropertyMap)obj).keySet());
        
        for(String key:tkeys) {
          if(iskeyforequal(key)) {
            boolean equal = true;
            if(getValue(key) != null && getValue(key).getClass().isArray()) {
              Object a = getValue(key);
              Object a2 = ((PropertyMap)obj).getValue(key);
              if(a instanceof Object[])
                equal = a2 instanceof Object[] && Arrays.equals((Object[])a, (Object[])a2);
              if(a instanceof int[])
                equal = a2 instanceof int[] && Arrays.equals((int[])a, (int[])a2);
              if(a instanceof long[])
                equal = a2 instanceof long[] && Arrays.equals((long[])a, (long[])a2);
              if(a instanceof double[])
                equal = a2 instanceof double[] && Arrays.equals((double[])a, (double[])a2);
              if(a instanceof float[])
                equal = a2 instanceof float[] && Arrays.equals((float[])a, (float[])a2);
              if(a instanceof byte[])
                equal = a2 instanceof byte[] && Arrays.equals((byte[])a, (byte[])a2);
              if(a instanceof char[])
                equal = a2 instanceof char[] && Arrays.equals((char[])a, (char[])a2);
              if(a instanceof boolean[])
                equal = a2 instanceof boolean[] && Arrays.equals((boolean[])a, (boolean[])a2);
              if(a instanceof short[])
                equal = a2 instanceof short[] && Arrays.equals((short[])a, (short[])a2);
            }else if(getValue(key) != null && getValue(key) instanceof Collection) {
              Object[] olist = ((PropertyMap)obj).getList(key,Object.class).toArray();
              Object[] list  = getList(key,Object.class).toArray();
              equal = olist != null && list.length == olist.length;
              if(equal) {
                for(int i=0;i<list.length;i++) {
                  equal = list[i].equals(olist[i]);
                  if(!equal) {
                    if(printDifferenceProperty().getValue()) {
                      System.out.println();
                      System.out.println("NOT EQUAL:");
                      System.out.println(list[i]);
                      System.out.println(olist[i]);
                    }
                    break;
                  }
                }
              }
            }else equal = ((PropertyMap)obj).getValue(key) == null && getValue(key) == null || ((PropertyMap)obj).getValue(key) != null && getValue(key) != null && ((PropertyMap)obj).getValue(key).equals(getValue(key));//Objects.equals(((PropertyMap)obj).getValue(key), getValue(key));
            if(!equal) {
              if(printDifferenceProperty().getValue()) {
                Object thiso = getValue(key);
                Object othero = ((PropertyMap)obj).getValue(key);
                System.out.println();
                System.out.println("this : "+key+" = "+getValue(key)+(getValue(key) instanceof byte[] ? " l:"+((byte[])getValue(key)).length : "")+"|");
                System.out.println("other: "+key+" = "+((PropertyMap)obj).getValue(key)+(((PropertyMap)obj).getValue(key) instanceof byte[] ? " l:"+((byte[])((PropertyMap)obj).getValue(key)).length : "")+"|");
                System.out.println(getValue(key)+" equals "+((PropertyMap)obj).getValue(key)+" = "+Objects.equals(getValue(key), ((PropertyMap)obj).getValue(key)));
              }
              return false;
            }
          }
        }
        return true;
      }
      
      private BooleanProperty printDifference = new SimpleBooleanProperty(false);
      
      public BooleanProperty printDifferenceProperty() {
        return printDifference;
      }
      
      private boolean iskeyforequal(String key) {
        if(notEqualKeys().contains(key))
          return false;
        if(!equalKeys().isEmpty()) {
          for(String k:equalKeys()) {
            if(k.startsWith("re:")) {
              Matcher m = Pattern.compile(k.substring(3)).matcher(key);
              if(m.find())
                return true;
            }else if(k.equals(key))
              return true;
          }
          return false;
        }
        return true;
      }
      
      @Override
      public Map<String, Property> getMap() {
        return map;
      }

      @Override
      public ObservableList<EventHandler<Event>> getAddListeners() {
        return addlisteners;
      }
      
      @Override
      public ObservableList<EventHandler<Event>> getDellListeners() {
        return dellisteners;
      }
      
      @Override
      public String toString() {
        if(getToStringPropertyName() != null && containsKey(getToStringPropertyName()))
          return String.valueOf(getValue(getToStringPropertyName()));
        else return "";
      }

      private String getToStringPropertyName() {
        return toStringPropertyName;
      }

      @Override
      public PropertyMap toStringProperty(String propertyName) {
        toStringPropertyName = propertyName;
        return this;
      }

      @Override
      public ObjectProperty<LocalDateTime> lastupdate() {
        return lastupdate;
      }
    };
    return p;
  }
  
  public default PropertyMap equalKeys(String... keys) {
    equalKeys().addAll(keys);
    return this;
  }

  public default PropertyMap equalKeys(Collection keys) {
    equalKeys().addAll(keys);
    return this;
  }
  
  public default PropertyMap notEqualKeys(String... keys) {
    notEqualKeys().addAll(keys);
    return this;
  }

  public default PropertyMap notEqualKeys(Collection keys) {
    notEqualKeys().addAll(keys);
    return this;
  }
  
  public default PropertyMap copyFrom(PropertyMap map) {
    return copyFrom(map, new String[0]);
  }
  
  public default PropertyMap copyFrom(PropertyMap map, String... keys) {
    if(map != null) {
      if(keys == null || keys.length == 0)
        keys = map.keySet().toArray(new String[0]);
      Arrays.stream(keys).forEach((key) -> {
        Object val = map.getValue(key);
        if(val instanceof PropertyMap)
          setValue(key, ((PropertyMap)val).copy());
        if(val instanceof Collection)
          setValue(key, PropertyMap.copyList((Collection)val));
        setValue(key, map.getValue(key));
      });
      equalKeys(map.equalKeys());
    }
    return this;
  }
  
  public default PropertyMap copyFrom(Map<String, Object> map) {
    if(map != null) {
      map.keySet().stream().forEach((key) -> {
        setValue(key, map.get(key));
      });
    }
    return this;
  }
  
  public default Map<String,Object> getSimpleMapWithoutKeys(String... keys) {
    return getSimpleMap(false, ArrayUtils.removeElements(getMap().keySet().toArray(new String[0]), keys));
  }
  
  public default Map<String,Object> getSimpleMap(Collection<String> keys) {
    return getSimpleMap(false, keys.toArray(new String[0]));
  }
  
  public default Map<String,Object> getSimpleMap(String... keys) {
    return getSimpleMap(false, keys);
  }
  
  public default Map<String,Object> getSimpleMap() {
    return getSimpleMap(false);
  }
  
  public default Map<String,Object> getSimpleMap(boolean copy, String... keys) {
    Map map = new TreeMap();
    keys = keys.length == 0 ? getMap().keySet().toArray(new String[0]) : keys;
    for(String key:keys) {
      Object value = getValue(key);
      Object mapValue = value;

      if(value instanceof PropertyMap)
        mapValue = copy ? PropertyMap.copy((PropertyMap)value) : ((PropertyMap)value).getSimpleMap();

      if(value instanceof Collection) {
        //((List) value).
        if(copy) {
          Collection list = null;
          try {
            list = (Collection) value.getClass().newInstance();
          }catch(Exception ex) {
            list = FXCollections.observableArrayList();
          }
          if(list != null)
            for(Object o:(Collection)value)
              list.add(o instanceof PropertyMap ? PropertyMap.copy((PropertyMap)o) : o);
          mapValue = list;
        }else {
          Object[] ms = new Object[0];
          for(Object o:(Collection)value)
            ms = ArrayUtils.add(ms, o instanceof PropertyMap ? ((PropertyMap)o).getSimpleMap() : o);
          mapValue = ms;
        }
      }

      map.put(key, mapValue);
    }
    return map;
  }
  
  public default PropertyMap putAll(PropertyMap map) {
    map.keySet().stream().forEach((key) -> setValue(key, map.getValue(key)));
    return this;
  }
  
  public default PropertyMap setValueIfNotExist(String propertyName, Object value) {
    if(isNullOrEmpty(propertyName))
      getProperty(propertyName).setValue(value);
    return this;
  }
  
  public default PropertyMap setValue(String propertyName, Object value) {
    getProperty(propertyName).setValue(value);
    return this;
  }
  
  public default Property getProperty(String propertyName) {
    Property property = getMap().get(propertyName);
    if(property == null) {
      getMap().put(propertyName, property = new SimpleObjectProperty());
      property.addListener((ObservableValue ob, Object ol, Object nw) -> lastupdate().setValue(LocalDateTime.now()));
    }
    return property;
  }
  
  
  
  public static <T> List<T> getListFromList(Collection<? extends PropertyMap> sourcelist, String propertyName, Class<? extends T> type) {
    return sourcelist.stream().map(s -> s.getValue(propertyName, type)).collect(Collectors.toList());
  }
  
  public static <T> T[] getArrayFromList(Collection<? extends PropertyMap> sourcelist, String propertyName, Class<? extends T> type) {
    return sourcelist.stream().map(s -> s.getValue(propertyName, type)).collect(Collectors.toList()).toArray((T[])Array.newInstance(type, 0));
  }
  
  public static <T> Set<T> getSetFromList(Collection<? extends PropertyMap> sourcelist, String propertyName, Class<? extends T> type) {
    return sourcelist.stream().map(s -> s.getValue(propertyName, type)).collect(Collectors.toSet());
  }
  
  public static Collection<Map> toSimple(Collection<? extends PropertyMap> list) {
    ObservableList<Map> mlist = FXCollections.observableArrayList();
    if(list != null)
      list.stream().forEach(l -> mlist.add(l.getSimpleMap()));
    return mlist;
  }
  
  public static ObservableList<PropertyMap> fromSimple(Collection<Map> list) {
    ObservableList<PropertyMap> mlist = FXCollections.observableArrayList();
    if(list != null)
      list.stream().forEach(l -> mlist.add(PropertyMap.copy(l)));
    return mlist;
  }
  
  public static <T> ObservableList<T> getListFromListMap(Collection<? extends Map> sourcelist, String propertyName, Class<? extends T> type) {
    ObservableList<T> list = FXCollections.observableArrayList();
    if(sourcelist != null)
      sourcelist.stream().forEach(l -> list.add(type.cast(l.get(propertyName))));
    return list;
  }
  
  public static ObservableList<PropertyMap> createList(Collection<Map> simpleList) {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    simpleList.stream().forEach(l -> list.add(PropertyMap.copy(l)));
    return list;
  }
  
  public default ObservableList<PropertyMap> getList(String propertyName) {
    return getValue(propertyName, FXCollections.<PropertyMap>observableArrayList());
  }
  
  public default <T> ObservableList<T> getList(String listPropertyName, String propertyName, Class<? extends T> type) {
    ObservableList<T> list = FXCollections.observableArrayList();
    getList(listPropertyName).stream().forEach(l -> list.add(l.getValue(propertyName, type)));
    return list;
  }
  
  public default <T> T[] getArray(String propertyName, Class<T> type) {
    return (T[])getValue(propertyName, Array.newInstance(type, 0));
  }
  
  public static void addListCangeListener(Collection<PropertyMap> list, String propertyName, ChangeListener listener) {
    list.stream().forEach(l -> l.get(propertyName).addListener(listener));
  }
  
  public static ObservableList<PropertyMap> get(ObservableList<PropertyMap> list, String propertyName, Object propertyValue) {
    ObservableList<PropertyMap> vals = FXCollections.observableArrayList();
    for(PropertyMap p:list)
      if(p.getValue(propertyName).equals(propertyValue))
        vals.add(p);
    return vals;
  }
  
  public static boolean contains(ObservableList<PropertyMap> list, String propertyName, Object propertyValue) {
    return !get(list, propertyName, propertyValue).isEmpty();
  }
  
  public default <T> T getValue(String propertyName, Class<? extends T> type) {
    if(type.isEnum()) {
      Object[] values = type.getEnumConstants();
      for(Object o:values) {
        if(o.toString().equals(String.valueOf(getValue(propertyName)))) {
          return type.cast(o);
        }
      }
    }//else if(type == PropertyMap.class && getValue(propertyName) instanceof Map)
      //setValue(propertyName, PropertyMap.copy((Map)getValue(propertyName)));
    if(type == Period.class && getValue(propertyName) instanceof String)
      return (T)Period.parse(getString(propertyName));
    return (T)getValue(propertyName);
  }
  
  public default Object getValue(String propertyName) {
    return getProperty(propertyName).getValue();
  }
  
  public default <T> T getValue(String propertyName, T defailtValue) {
    if(getProperty(propertyName).getValue() == null)
      setValue(propertyName, defailtValue);
    return (T)getValue(propertyName, defailtValue.getClass());
  }
  
  public default <T> ObservableList<T> getList(String propertyName, Class<? extends T> type) {
    return getValue(propertyName, FXCollections.<T>observableArrayList());
  }
  
  public default boolean isPropertyMap(String propertyName) {
    return !isNull(propertyName) && getValue(propertyName) instanceof PropertyMap;
  }
  
  public default boolean isNotNull(String... propertyNames) {
    return !isNull(propertyNames);
  }
  
  public default boolean isNull(String... propertyNames) {
    for(String propertyName:propertyNames)
      if(getValue(propertyName) != null)
        return false;
    return true;
  }
  
  public default boolean isNullOrEmpty(String propertyName) {
    return !containsKey(propertyName) || getValue(propertyName) == null;
  }
  
  public default int size() {
    return getMap().size();
  }

  public default boolean isEmpty() {
    return getMap().isEmpty();
  }

  public default boolean containsKey(Object key) {
    return getMap().containsKey(key);
  }

  public default boolean containsValue(Object value) {
    return getMap().containsValue(value);
  }
  
  public default PropertyMap bind(String propertyName, ObservableValue observableValue) {
    get(propertyName).bind(observableValue);
    return this;
  }
  
  public default PropertyMap getMap(String propertyName) {
    return getValue(propertyName, PropertyMap.class);
  }
  
  public default Property get(String propertyName) {
    return get(propertyName, Object.class);
  }
  
  public default Property[] get(String... propertysName) {
    List<Property> list = new ArrayList<>();
    Arrays.stream(propertysName).filter(name -> containsKey(name)).forEach(name -> list.add(get(name)));
    return list.toArray(new Property[0]);
  }
  
  public default <T> Property<T> get(String propertyName, Class<? extends T> type) {
    Property<T> property = getMap().get(propertyName);
    if(property == null) {
      getMap().put(propertyName, property = new SimpleObjectProperty<>());
      firePropertyAdd(propertyName);
    }
    return property;
  }

  public default PropertyMap put(String key, Property value) {
    getMap().put(key, value);
    return this;
  }
  
  public default PropertyMap removePattern(String keyPattern) {
    getMap().keySet().stream().filter(k -> k.matches(keyPattern)).forEach(k -> remove(k));
    return this;
  }
  
  public default PropertyMap stayOnly(Object... keys) {
    for(String k:getMap().keySet().toArray(new String[0]))
      if(!ArrayUtils.contains(keys, k))
        remove(k);
    return this;
  }

  public default PropertyMap remove(Object... keys) {
    for(Object key:keys) {
      getMap().remove(key);
      firePropertyDell((String)key);
    }
    return this;
  }

  public default PropertyMap putAll(Map<? extends String, ? extends Property> m) {
    getMap().putAll(m);
    return this;
  }

  public default PropertyMap clear() {
    getMap().clear();
    return this;
  }
  
  public default PropertyMap clearValues() {
    getMap().values().stream().forEach(v -> v.setValue(null));
    return this;
  }

  public default Set<String> keySet() {
    return getMap().keySet();
  }

  public default Collection<Property> values() {
    return getMap().values();
  }

  public default Set<Map.Entry<String, Property>> entrySet() {
    return getMap().entrySet();
  }
  
  public default void setOnPropertyAdd(EventHandler<Event> listener) {
    if(!getAddListeners().contains(listener))
      getAddListeners().add(listener);
  }
  
  public default void setOnPropertyDell(EventHandler<Event> listener) {
    if(!getDellListeners().contains(listener))
      getDellListeners().add(listener);
  }
  
  public default void firePropertyAdd(String propertyName) {
    getAddListeners().stream().forEach(l -> l.handle(new Event(propertyName, null, EventType.ROOT)));
  }
  
  public default void firePropertyDell(String propertyName) {
    getDellListeners().stream().forEach(l -> l.handle(new Event(propertyName, null, EventType.ROOT)));
  }
  
  public default PropertyMap unbindAll() {
    keySet().stream().forEach(key -> get(key).unbind());
    return this;
  }
  
  public default PropertyMap unbind(String... propertyNames) {
    for(String n:propertyNames)
      get(n).unbind();
    return this;
  }
  
  public default boolean saveAsJsonFile(String jsonFileName) {
    FileWriter writer = null;
    try {
      writer = new FileWriter(jsonFileName);
      writer.write(toJson());
      return true;
    }catch(Exception ex) {
      ex.printStackTrace();
      return false;
    }finally {
      if(writer != null) {
        try{writer.flush();}catch(Exception x){x.printStackTrace();}
        try{writer.close();}catch(Exception x){x.printStackTrace();}
      }
    }
  }
  
  public static PropertyMap fromJsonFile(String jsonFileName) {
    return fromJson(Utility.getStringFromFile(jsonFileName));
  }
  
  public default String toJsonFile(String jsonFileName) throws Exception {
    return Utility.writeBytesToFile(toJson().getBytes(), jsonFileName);
  }
  
  public default String toJson() {
    return toJson(false);
  }
  
  public default String toJson(boolean simple) {
    if(simple)
      return new GsonBuilder().setPrettyPrinting().registerTypeAdapter(PropertyMap.class, new PropertyMapDeserializer()).create().toJson(copy().getSimpleMap());
    else return new GsonBuilder().setPrettyPrinting().registerTypeAdapter(PropertyMap.class, new PropertyMapDeserializer()).create().toJson(copy(), PropertyMap.class);
  }
  
  public static PropertyMap fromJson(String json) {
    Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(PropertyMap.class, new PropertyMapDeserializer()).create();
    return gson.fromJson(json, PropertyMap.class);
  }

  class PropertyMapDeserializer implements JsonDeserializer<PropertyMap>, JsonSerializer<PropertyMap> {
    @Override
    public JsonElement serialize(PropertyMap p, Type type, JsonSerializationContext jsc) {
      JsonObject o = new JsonObject();
      p.keySet().stream().filter(key -> !p.isNull(key)).forEach(key -> {
        Object val = p.getValue(key);
        if(val instanceof PropertyMap)
          o.add(key, serialize(((PropertyMap)val), type, jsc));
        else if(val instanceof Collection || val.getClass().isArray()) {
          o.add(key, getArray(val, type, jsc));
        }else o.add(key, objectToJsonElement(val));
      });
      return o;
    }
    
    @Override
    public PropertyMap deserialize(JsonElement json, Type type, JsonDeserializationContext jdc) throws JsonParseException {
      PropertyMap p = PropertyMap.create();
      
      json.getAsJsonObject().entrySet().stream().forEach(entry -> {
        String      key = entry.getKey();
        JsonElement val = entry.getValue();
        
        if(val.isJsonObject())
          p.setValue(key, deserialize(val, type, jdc));
        else if(val.isJsonArray()) {
          ObservableList list = FXCollections.observableArrayList();
          for(JsonElement je:val.getAsJsonArray()) {
            if(je.isJsonObject() || je.isJsonArray())
              list.add(deserialize(je, type, jdc));
            else list.add(jsonElementToObject(je));
          }
          p.setValue(key, list);
        }else p.setValue(key, jsonElementToObject(val));
      });
      return p;
    }
    
    private JsonElement objectToJsonElement(Object o) {
      if(o instanceof Boolean)
        return new JsonPrimitive((Boolean)o);
      
      if(o instanceof Integer)
        return new JsonPrimitive((Integer)o);
      
      if(o instanceof String)
        return new JsonPrimitive((String)o);
      
      if(o instanceof Timestamp)
        return new JsonPrimitive(o.getClass().getSimpleName()+":"+Utility.format((Timestamp)o));
      
      if(o instanceof Date)
        return new JsonPrimitive(o.getClass().getSimpleName()+":"+Utility.format((Date)o));
      
      if(o instanceof java.sql.Date)
        return new JsonPrimitive(o.getClass().getName()+":"+Utility.format((java.sql.Date)o));
      
      if(o instanceof LocalDate)
        return new JsonPrimitive(o.getClass().getSimpleName()+":"+Utility.format((LocalDate)o));
      
      if(o instanceof LocalDateTime)
        return new JsonPrimitive(o.getClass().getSimpleName()+":"+Utility.format((LocalDateTime)o));
      
      if(o instanceof LocalTime)
        return new JsonPrimitive(o.getClass().getSimpleName()+":"+Utility.format((LocalTime)o));
      
      return new JsonPrimitive(o.getClass().getSimpleName()+":"+String.valueOf(o));
    }
    
    private Object jsonElementToObject(JsonElement element) {
      if(element instanceof JsonNull)
        return null;
      
      JsonPrimitive prim = element.getAsJsonPrimitive();
      Object o = null;
      
      if(prim.isBoolean())
        o = prim.getAsBoolean();
      
      if(prim.isNumber())
        o = prim.getAsInt();
      
      if(prim.isString()) {
        o = prim.getAsString();
        if(prim.getAsString().indexOf(":") > 0) {
          String classname = prim.getAsString().substring(0,prim.getAsString().indexOf(":"));
          try {
            String val = prim.getAsString().substring(classname.length()+1);
            switch(classname) {
              
              case "date"          : o = Utility.parse(val);break;
              case "java.sql.Date" : o = Utility.parseToSqlDate(val);break;
              case "Timestamp"     : o = Utility.parseToTimeStamp(val);break;
              
              case "LocalDate"     : o = Utility.parseLocalDate(val);break;
              case "LocalDateTime" : o = Utility.parseLocalDateTime(val);break;
              case "LocalTime"     : o = Utility.parseLocalTime(val);break;
              
              case "BigDecimal"    : o = BigDecimal.valueOf(Double.parseDouble(val));break;
              case "BigInteger"    : o = BigInteger.valueOf(Long.parseLong(val));break;
              case "Byte"          : o = Byte.valueOf(val);break;
              case "Double"        : o = Double.parseDouble(val);break;
              case "Float"         : o = Float.parseFloat(val);break;
              case "Short"         : o = Short.parseShort(val);break;
              case "Long"          : o = Long.parseLong(val);break;
            }
          }catch(Exception ex) {
            Logger.getLogger(PropertyMap.class).error("",ex);
          }
        }
      }
      return o;
    }
    
    private JsonArray getArray(Object src, Type type, JsonSerializationContext jsc) {
      if(src instanceof Collection)
        return getArray(((Collection)src).toArray(), type, jsc);
      JsonArray array = new JsonArray();
      for(int i=0;i<Array.getLength(src);i++) {
        Object o = Array.get(src, i);
        if(o instanceof Collection || o.getClass().isArray())
          array.add(getArray(src, type, jsc));
        else if(o instanceof PropertyMap)
          array.add(serialize(((PropertyMap)o), type, jsc));
        else array.add(objectToJsonElement(o));
      }
      return array;
    }
  }
  
  public default byte[] getBytes(String property) {
    return getValue(property, byte[].class);
  }
  
  public default Integer getInteger(String property) {
    return getValue(property, Integer.TYPE);
  }
  
  public default Long getLong(String property) {
    return getValue(property, Long.TYPE);
  }
  
  public default Double getDouble(String property) {
    return getValue(property, Double.TYPE);
  }
  
  public default Short getShort(String property) {
    return getValue(property, Short.TYPE);
  }
  
  public default Float getFloat(String property) {
    return getValue(property, Float.TYPE);
  }
  
  public default BigDecimal getBigDecimal(String property) {
    return getValue(property, BigDecimal.class);
  }
  
  public default boolean is(String property) {
    return getValue(property, boolean.class);
  }
  
  public default String getString(String property) {
    return getValue(property, String.class);
  }
  
  public default Period getPeriod(String property) {
    return getValue(property, Period.class);
  }
  
  public default Date getDate(String property) {
    return getValue(property, Date.class);
  }
  
  public default java.sql.Date getSqlDate(String property) {
    return getValue(property, java.sql.Date.class);
  }
  
  public default LocalDate getLocalDate(String property) {
    return getValue(property, LocalDate.class);
  }
  
  public default LocalTime getLocalTime(String property) {
    return getValue(property, LocalTime.class);
  }
  
  public default LocalDateTime getLocalDateTime(String property) {
    return getValue(property, LocalDateTime.class);
  }
  
  public default Timestamp getTimestamp(String property) {
    return getValue(property, Timestamp.class);
  }
  
  public static boolean equals(Object a, Object b, String... keys) {
    if(a.getClass() != b.getClass())
      return false;
    
    if(a instanceof Collection) {
      if(((Collection)a).size() != ((Collection)b).size())
        return false;
      Object[] al = ((Collection)a).toArray();
      Object[] bl = ((Collection)b).toArray();
      for(int i=0;i<al.length;i++) {
        if(!equals(al[i], bl[i], keys)) {
          return false;
        }
      }
      return true;
    }
    
    keys = Arrays.stream(keys).flatMap(k -> {
      if(k.startsWith("r=")) {
        String m = k.substring(2);
        Set<String> ks = new HashSet<>();
        ks.addAll(((PropertyMap)a).keySet());
        ks.addAll(((PropertyMap)b).keySet());
        return ks.stream().filter(kk -> kk.matches(m));
      }else return Arrays.stream(new String[]{k});
    }).collect(Collectors.toList()).toArray(new String[0]);
    
    if(a instanceof PropertyMap) {
      keys = keys.length == 0 ? ((PropertyMap)a).keySet().toArray(new String[0]) : keys;
      for(String key:keys) {
        if(((PropertyMap)a).containsKey(key) && !((PropertyMap)b).containsKey(key) || !((PropertyMap)a).containsKey(key) && ((PropertyMap)b).containsKey(key))
          return false;
        Object ao = ((PropertyMap)a).getValue(key);
        Object bo = ((PropertyMap)b).getValue(key);
        if(ao != null && ao.getClass().isArray() && !Objects.deepEquals(ao,bo))
          return false;
        if(!Objects.equals(ao,bo))
          return false;
      }
      return true;
    }
    
    return Objects.equals(a, b);
  }
}