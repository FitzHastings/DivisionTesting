package division.slider;

import java.awt.Dimension;
import java.awt.Rectangle;

public interface MThumbSliderAdditional {
  public Rectangle getTrackRect();
  public Dimension getThumbSize();
  public int xPositionForValue(int value);
  public int yPositionForValue(int value);
}