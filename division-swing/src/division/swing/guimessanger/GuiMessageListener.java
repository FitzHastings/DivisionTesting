package division.swing.guimessanger;

public interface GuiMessageListener {
  public enum Type{ERROR, INFO, WARNING}
  public void message(Type messageType, String title, String message, Throwable ex);
}