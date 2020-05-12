package division.scale;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public interface ObjectPeriodScaleListener {
  public void objectPeriodDoubleClicked(ObjectPeriod period);
  public void objectPeriodsSelected(List<ObjectPeriod> periods);
  
  public void dayDoubleClicked(int rowIndex, Date day);
  public void daysSelected(TreeMap<Integer, List<Date>> days);

  public void dayWidthChanged(int dayWidth);
}