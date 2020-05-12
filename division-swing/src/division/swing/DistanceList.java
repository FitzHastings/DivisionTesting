package division.swing;

import java.util.Vector;

public class DistanceList extends Vector<DistanceTime> {
  @Override
  public boolean contains(Object o) {
    if(o instanceof Long)
      return getDistanceTime((Long)o) != null;
    return super.contains(o);
  }

  public DistanceTime getDistanceTime(long date) {
    for(DistanceTime distanceTime:this)
      if(distanceTime.contains(date))
        return distanceTime;
    return null;
  }

  public DistanceTime getDistanceTime(DistanceTime dt) {
    for(DistanceTime distanceTime:this)
      if(distanceTime.contains(dt))
        return distanceTime;
    return null;
  }

  public void validate() {
    DistanceTime distanceTime,dt;
    for(int i=size()-1;i>=0;i--) {
      distanceTime = get(i);
      for(int j=size()-1;j>=0;j--) {
        if(i != j) {
          dt = get(j);
          if(dt.contains(distanceTime))
            remove(i);
          else if(dt.contains(distanceTime.getStart())) {
            remove(i);
            remove(j);
            add(new DistanceTime(dt.getStart(), distanceTime.getEnd()));
          }else if(dt.contains(distanceTime.getEnd())) {
            remove(i);
            remove(j);
            add(new DistanceTime(distanceTime.getStart(), dt.getEnd()));
          }else if(dt.getEnd() == distanceTime.getStart()-24*60*60*1000) {
            remove(i);
            dt.setEnd(distanceTime.getEnd());
          }else if(distanceTime.getEnd() == dt.getStart()-24*60*60*1000) {
            remove(i);
            dt.setStart(distanceTime.getStart());
          }
        }
      }
    }
  }
}