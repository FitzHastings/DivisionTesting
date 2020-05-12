package division.scale;

import division.util.Utility;
import java.awt.Color;
import java.awt.Rectangle;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TreeMap;

public class ObjectPeriod {
  private PeriodScale scale;
  private int rowIndex;
  private long startDate;
  private long endDate;
  private Color color;
  private Integer id;
  private String comment = "";
  private Rectangle bounds = null;
  
  //private TreeMap<Timestamp,UserComment> dateUserComments     = new TreeMap<>();
  //private TreeMap<Timestamp,UserComment> stopDateUserComments = new TreeMap<>();
  private TreeMap<Long,UserComment> dateUserComments     = new TreeMap<>();
  private TreeMap<Long,UserComment> stopDateUserComments = new TreeMap<>();
  
  private boolean visible  = true;
  
  public ObjectPeriod(PeriodScale scale, int rowIndex, long startDate, long endDate, Color color, Integer id, String comment) {
    this.scale = scale;
    this.rowIndex = rowIndex;
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(startDate);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    
    this.startDate = c.getTimeInMillis();
    
    c.setTimeInMillis(endDate);
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
    
    this.endDate = c.getTimeInMillis();
    
    this.color = color;
    this.id = id;
    this.comment = comment;
  }
  
  public ObjectPeriod(PeriodScale scale, int rowIndex, java.sql.Date startDate, java.sql.Date endDate, Color color, Integer id, String comment) {
    this(scale, rowIndex, startDate.getTime(), endDate.getTime(), color, id, comment);
  }
  
  public ObjectPeriod(PeriodScale scale, int rowIndex, Date startDate, Date endDate, Color color, Integer id, String comment) {
    this(scale, rowIndex, startDate.getTime(), endDate.getTime(), color, id, comment);
  }
  
  public ObjectPeriod(PeriodScale scale, int rowIndex, java.sql.Date startDate, java.sql.Date endDate, Color color, Integer id) {
    this(scale, rowIndex, startDate.getTime(), endDate.getTime(), color, id, "");
  }
  
  public ObjectPeriod(PeriodScale scale, int rowIndex, Date startDate, Date endDate, Color color, Integer id) {
    this(scale, rowIndex, startDate.getTime(), endDate.getTime(), color, id, "");
  }

  public TreeMap<Long, UserComment> getDateUserComments() {
    return dateUserComments;
  }
  
  public int getUserCommentsCount() {
    return dateUserComments.size();
  }

  public void setDateUserComments(TreeMap<Long, UserComment> dateUserComments) {
    this.dateUserComments = dateUserComments;
  }

  public TreeMap<Long, UserComment> getStopDateUserComments() {
    return stopDateUserComments;
  }

  public void setStopDateUserComments(TreeMap<Long, UserComment> stopDateUserComments) {
    this.stopDateUserComments = stopDateUserComments;
  }
  
  public UserComment getLastDateUserComment() {
    return dateUserComments.isEmpty()?null:dateUserComments.firstEntry().getValue();
  }
  
  public UserComment getLastStopUserComment() {
    return stopDateUserComments.isEmpty()?null:stopDateUserComments.firstEntry().getValue();
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public Rectangle getBounds() {
    if(bounds == null) {
      Rectangle startRect = scale.getCellRect(getRowIndex(), getStartColumn(), false);
      Rectangle endRect   = scale.getCellRect(getRowIndex(), getEndColumn(), false);
      bounds = new Rectangle(startRect.x, startRect.y, endRect.x+endRect.width-startRect.x, endRect.height);
    }
    return bounds;
  }
  
  public void recalculate() {
    this.bounds = null;
  }

  public int getEndColumn() {
    return scale.getColumn(endDate);
  }

  public int getStartColumn() {
    return scale.getColumn(startDate);
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public long getEndDate() {
    return endDate;
  }

  public void setEndDate(long endDate) {
    this.endDate = endDate;
    bounds = null;
  }

  public long getStartDate() {
    return startDate;
  }

  public void setStartDate(long startDate) {
    this.startDate = startDate;
    bounds = null;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getRowIndex() {
    return rowIndex;
  }

  public void setRowIndex(int rowIndex) {
    if(getRowIndex() != rowIndex) {
      scale.removePeriod(id);
      this.rowIndex = rowIndex;
      scale.addPeriods(new ObjectPeriod[]{this});
    }
  }

  public PeriodScale getScale() {
    return scale;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ObjectPeriod other = (ObjectPeriod) obj;
    if (this.rowIndex != other.rowIndex) {
      return false;
    }
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 97 * hash + this.rowIndex;
    hash = 97 * hash + Objects.hashCode(this.id);
    return hash;
  }

  @Override
  public String toString() {
    return Utility.format(new Timestamp(getStartDate()))+" => "+Utility.format(new Timestamp(getEndDate()));
  }
}