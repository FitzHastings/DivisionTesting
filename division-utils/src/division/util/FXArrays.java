package division.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;

public class FXArrays {
  public static <T> T[] retainAll(T[] a1, T... a2) {
    Set<T> s = new HashSet<T>(Arrays.asList(a1));
    s.retainAll(Arrays.asList(a2));
    for(int i=a1.length-1;i>=0;i--)
      a1 = ArrayUtils.remove(a1, i);
    return s.toArray(a1);
  }
}