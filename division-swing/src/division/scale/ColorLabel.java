package division.scale;

import java.awt.Color;
import java.util.Date;
import java.util.Objects;

public class ColorLabel {
  private long date;
  private Color color;
  private String text;
  
  public ColorLabel(Date date, Color color, String text) {
    this(date==null?null:date.getTime(), color, text);
  }
  
  public ColorLabel(java.sql.Date date, Color color, String text) {
    this(date==null?null:date.getTime(), color, text);
  }
  
  public ColorLabel(long date, Color color, String text) {
    this.date = date;
    this.color = color;
    this.text = text;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public long getDate() {
    return date;
  }

  public void setDate(long date) {
    this.date = date;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ColorLabel other = (ColorLabel) obj;
    if (this.date != other.date) {
      return false;
    }
    if (!Objects.equals(this.text, other.text)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + (int) (this.date ^ (this.date >>> 32));
    hash = 79 * hash + Objects.hashCode(this.text);
    return hash;
  }
}