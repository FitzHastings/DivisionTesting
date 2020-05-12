package division.scale;

import java.awt.Color;
import java.util.Map;

public interface PeriodController {
  public boolean isVisible(int row, Map.Entry<Integer,Integer> startEnd,Color color);
  public boolean isVisible(int row, ObjectPeriod period);
}