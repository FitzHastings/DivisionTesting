package division.util;

import java.util.HashMap;
import java.util.Map;

public class DivisionMap<KeyType, ValueType> extends HashMap<KeyType, ValueType> {
  public DivisionMap<KeyType, ValueType> puts(KeyType key, ValueType value) {
    put(key, value);
    return this;
  }
  
  public DivisionMap<KeyType, ValueType> putsAll(Map<KeyType, ValueType> map) {
    putAll(map);
    return this;
  }

  public static DivisionMap create() {
    return new DivisionMap();
  }

  @Override
  public ValueType put(KeyType key, ValueType value) {
    super.put(key, value);
    return value;
  }
}