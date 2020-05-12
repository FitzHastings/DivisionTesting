package bum.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

public abstract class Editor extends EditorGui {
  private final List<ObjectChangeListener> listeners = new ArrayList<>();
  
  public Editor() {	
    this("");
  }

  public Editor(String name, String title) {
    this(name, title, null);
  }

  public Editor(String name) {
    this(name, "", null);
  }

  public Editor(String title,ImageIcon icon) {
    this(null, title, icon);
  }

  public Editor(ImageIcon icon, String name) {
    this(name, "", icon);
  }
	
  public Editor(String name, String title, ImageIcon icon) {
    super(title,icon,name);
  }

  public void addObjectChangeListener(ObjectChangeListener objectChangeListener) {
    if(!listeners.contains(objectChangeListener))
      listeners.add(objectChangeListener);
  }

  public void removeObjectChangeListener(ObjectChangeListener objectChangeListener) {
    listeners.remove(objectChangeListener);
  }

  public void fireObjectChangeListener(ObjectChangeListener.Type type) {
    for(int i=listeners.size()-1;i>=0;i--) {
      try {
        listeners.get(i).objectChanged(type);
      }catch(Exception ex) {
        listeners.remove(i);
      }
    }
  }
  
  public static Editor loadEditor(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
    if(className == null)
      return null;
    Class cl = Class.forName(className);
    return (Editor)cl.newInstance();
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof Editor))
      return false;
    return (this.getId()==null?((Editor)obj).getId()==null:this.getId().equals(((Editor)obj).getId()));
  }
}