package division.swing.table.span;

public interface CellAlign {
  public int getAlign(int row, int column);
  public void setAlign(int align, int row, int column);
}