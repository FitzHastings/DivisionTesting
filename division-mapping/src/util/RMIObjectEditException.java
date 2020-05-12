package util;

public class RMIObjectEditException extends Exception {
  public RMIObjectEditException() {
    super("Объект открыт на редактирование другим пользователем");
  }
}