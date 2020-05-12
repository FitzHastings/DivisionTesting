package division.update;

import java.io.Serializable;

public class Command implements Serializable {
  private String command;
  private String application;
  private Object data;

  public Command(String application, String command, Object data) {
    this.application = application;
    this.command = command;
    this.data = data;
  }

  public String getApplication() { 
    return application;
  }

  public void setApplication(String application) {
    this.application = application;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }
}