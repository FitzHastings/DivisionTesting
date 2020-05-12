package conf;

import division.util.Utility;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

public class P {
  public static String DEF_CONF_FILE;
  
  static {
    DEF_CONF_FILE = "conf"+File.separator+"conf.json";
    P.load();
  }
  
  public static boolean isNull(String name) {
    return System.getProperties().get(name) == null;
  }
  
  public static boolean contains(String name) {
    return System.getProperties().containsKey(name);
  }
  
  public static <T> T get(String name, Class<? extends T> type) {
    return (T)System.getProperties().get(name);
  }
  
  public static <T> T get(String name, T defaultValue) {
    return System.getProperties().get(name) == null ? defaultValue : (T)System.getProperties().get(name);
  }
  
  public static <T> List<T> list(String name, Class<? extends T> type) {
    ObservableList<T> list = FXCollections.observableArrayList();
    for(Object o:get(name, JSONArray.class).toArray())
      list.add(type.cast(o));
    return list;
  }
  
  public static String String(String name) {
    return get(name, String.class);
  }
  
  public static Integer Integer(String name) {
    return get(name, Integer.TYPE);
  }
  
  public static Double Double(String name) {
    return get(name, Double.TYPE);
  }
  
  public static Float Float(String name) {
    return get(name, Float.TYPE);
  }
  
  public static BigDecimal BigDecimal(String name) {
    return get(name, BigDecimal.class);
  }
  
  public static void load() {
    load(DEF_CONF_FILE);
  }
  
  public static void load(String confFile) {
    try {
      setparams("", JSONObject.fromObject(Utility.getStringFromFile(confFile)));
    }catch(Exception ex) {
      ex.printStackTrace();
      Logger.getLogger(P.class).error(ex);
    }
  }
  
  private static void setparams(String pkey, JSONObject config) {
    for(Object key:config.keySet()) {
      String prop = pkey+(pkey.equals("") ? "" : ".")+key;
      Object val = config.get(key);
      if(val instanceof JSONObject)
        setparams(prop, (JSONObject)val);
      else {
        System.getProperties().put(prop, val);
        System.out.println(prop+" = "+val+" "+val.getClass().getSimpleName());
      }
    }
  }
}