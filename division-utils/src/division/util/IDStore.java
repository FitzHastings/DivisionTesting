package division.util;

import java.util.TreeMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;

public class IDStore {
  private static TreeMap<String, Long[]> IDStore = new TreeMap();
  private static TreeMap<String, Integer[]> intIDStore = new TreeMap();
  
  public static synchronized Long createID() {
    return createID(null);
  }
  
  public static synchronized Long createID(Object o) {
    return createID(o.getClass().getName());
  }

  public static synchronized Long createID(String storeName) {
    storeName = storeName == null ? "global" : storeName;
    Long id;
    do {
      id = RandomUtils.nextLong(0, Long.MAX_VALUE);
    }while(ArrayUtils.contains(IDStore.get(storeName), id));
    IDStore.put(storeName, ArrayUtils.add(IDStore.get(storeName), id));
    return id;
  }
  
  public static synchronized Integer createIntegerID(String storeName) {
    storeName = storeName == null ? "global" : storeName;
    Integer id;
    do {
      id = RandomUtils.nextInt(0, Integer.MAX_VALUE);
    }while(ArrayUtils.contains(intIDStore.get(storeName), id));
    intIDStore.put(storeName, ArrayUtils.add(intIDStore.get(storeName), id));
    return id;
  }
  
  public static int size(Object o) {
    return size(o.getClass().getName());
  }
  
  public static int size(String storeName) {
    return IDStore.get(storeName).length;
  }
  
  public static synchronized void resetIDStories() {
    IDStore.keySet().stream().forEach(storeName -> resetIDStore(storeName));
  }
  
  public static synchronized void resetIDStore() {
    resetIDStore(null);
  }
  
  public static synchronized void resetIDStore(Object o) {
    resetIDStore(o.getClass().getName());
  }

  public static synchronized void resetIDStore(String storeName) {
    IDStore.put(storeName == null ? "global" : storeName, new Long[0]);
  }
}