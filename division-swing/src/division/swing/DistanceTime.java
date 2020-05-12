package division.swing;

import java.util.Date;

public class DistanceTime implements Comparable<DistanceTime> {
  private long start;
  private long end;

  public DistanceTime(long start, long end) {
    this.start = start;
    this.end = end;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public Date getStartDate() {
    return new Date(getStart());
  }

  public Date getEndDate() {
    return new Date(getEnd());
  }

  boolean contains(DistanceTime distanceTime) {
    return contains(distanceTime.getStart()) && contains(distanceTime.getEnd());
  }

  public boolean contains(long date) {
    return date >= getStart() && date <= getEnd();
  }

  public boolean contains(Date date) {
    return contains(date.getTime());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof DistanceTime))
            return false;
    DistanceTime dt = (DistanceTime)obj;
    if(getStart() != dt.getStart())
            return false;
    if(getEnd() != dt.getEnd())
            return false;
    return true;
  }

  @Override
  public int compareTo(DistanceTime o) {
    return ((Long)getStart()).compareTo((Long)o.getStart());
  }

  @Override
  public String toString() {
    return "START: "+getStartDate()+"\nEND:   "+getEndDate();
  }
}