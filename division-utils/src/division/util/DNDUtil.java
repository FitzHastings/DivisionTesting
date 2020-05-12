package division.util;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.input.DataFormat;

public class DNDUtil {
  private static final Map<Class, DataFormat> dragFormats = new HashMap<>();
  
  public static DataFormat getDragFormat(Class dragclass) {
    if(!dragFormats.containsKey(dragclass))
      dragFormats.put(dragclass, new DataFormat(dragclass.getName()));
    return dragFormats.get(dragclass); 
  }
}