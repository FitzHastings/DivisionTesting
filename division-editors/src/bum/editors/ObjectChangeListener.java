package bum.editors;

public interface ObjectChangeListener {
  enum Type{CREATE,UPDATE,REMOVE}

  public void objectChanged(Type type);
}
