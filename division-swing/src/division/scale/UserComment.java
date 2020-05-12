package division.scale;

import java.sql.Timestamp;

public class UserComment {
  private Timestamp date;
  private Timestamp stopTime;
  private String comment;

  public UserComment(Timestamp date, Timestamp stopTime, String comment) {
    this.date = date;
    this.stopTime = stopTime;
    this.comment = comment;
  }

  public String getComment() {
    return comment;
  }

  public Timestamp getDate() {
    return date;
  }

  public Timestamp getStopTime() {
    return stopTime;
  }
}