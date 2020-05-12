package division.scale;

public interface PeriodDragger {
  public void moved(ObjectPeriod period, int oldRow, long oldStartDate, long oldEndDate);
  public boolean isMove(ObjectPeriod period, int newRow, long newStartDate, long newEndDate);
}
