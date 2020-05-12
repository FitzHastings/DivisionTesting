package division.swing.table.filter;

import division.swing.DistanceList;
import java.util.Date;
import javax.swing.RowFilter;

public class DatesFilter extends RowFilter {
  private DistanceList dates;
  public enum Type {EQUAL,NOT_EQUAL}
  private DatesFilter.Type type;
  private int[] columns = new int[0];
  
  public DatesFilter(DistanceList dates, int... columns) {
    this(DatesFilter.Type.EQUAL, dates, columns);
  }

  public DatesFilter(DatesFilter.Type type, DistanceList dates, int... columns) {
      this.type = type;
      this.dates = dates;
      this.columns = columns;
  }

  @Override
  public boolean include(Entry entry) {
    for(int column:columns) {
      Object object = entry.getValue(column);
      if(object instanceof Date) {
        Long d = ((Date)object).getTime();
        switch(type) {
          case EQUAL:
            return dates.contains(d);
          case NOT_EQUAL:
            return !dates.contains(d);
        }
      }else if(object instanceof java.sql.Date) {
        Long d = ((Date)object).getTime();
        switch(type) {
          case EQUAL:
            return dates.contains(d);
          case NOT_EQUAL:
            return !dates.contains(d);
        }
      }
    }
    return false;
  }
}