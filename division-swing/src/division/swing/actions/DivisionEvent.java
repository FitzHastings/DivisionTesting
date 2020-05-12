package division.swing.actions;

public class DivisionEvent {
  private Object source;
  private String command;
  private Object data;

  public DivisionEvent(Object source, String command, Object data) {
    this.source = source;
    this.command = command;
    this.data = data;
  }

  public DivisionEvent(Object source, String command) {
    this(source, command, null);
  }

  public String getCommand() {
    return command;
  }

  public Object getData() {
    return data;
  }

  public Object getSource() {
    return source;
  }
}