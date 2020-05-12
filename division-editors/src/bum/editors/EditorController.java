package bum.editors;

import bum.editors.util.DivisionTarget;
import java.awt.Cursor;
import java.awt.Window;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JComponent;
import javax.swing.JFrame;

public class EditorController {
  private static ConcurrentHashMap<Long,EditorGui> stack = new ConcurrentHashMap<>();
  private static Window frame;
  private static DivisionDesktop deskTop;
  private static ExecutorService pool = Executors.newCachedThreadPool();
  
  private static TreeMap<Long,ArrayList<Long>> targets = new TreeMap<>();

  private EditorController() {
  }
  
  public static void waitCursor() {
    waitCursor(null);
  }
  
  
  
  public static void waitCursor(JComponent com) {
    setCursor(com, new Cursor(Cursor.WAIT_CURSOR));
  }
  
  public static void defaultCursor() {
    setCursor(null, new Cursor(Cursor.DEFAULT_CURSOR));
  }
  
  public static void defaultCursor(JComponent com) {
    setCursor(com, new Cursor(Cursor.DEFAULT_CURSOR));
  }
  
  public static void setCursor(JComponent com, Cursor cursor) {
    if(com == null) {
      if(EditorController.getFrame() != null) {
        com = ((JFrame)EditorController.getFrame()).getRootPane();
        for(EditorGui gui:stack.values())
          gui.setCursor(cursor);
      }
    }
    if(com != null)
      EditorGui.setCursor(com, cursor);
  }
  
  public static void submit(Runnable runnable) {
    pool.submit(runnable);
  }

  public static DivisionDesktop getDeskTop() {
    return deskTop;
  }

  public static void setDeskTop(DivisionDesktop deskTop) {
    EditorController.deskTop = deskTop;
  }

  public static Window getFrame() {
    return frame;
  }

  public static void setFrame(Window frame) {
    EditorController.frame = frame;
  }
  
  public static EditorGui getEditor(Long id) {
    return stack.get(id);
  }
  
  public static void addToStack(Long id, EditorGui editor) {
    if(!stack.containsKey(id))
      stack.put(id, editor);
  }
  
  public static void addToStack(EditorGui editor) {
    if(!stack.containsKey(editor.getId()))
      stack.put(editor.getId(), editor);
  }
  
  public static void removeFromStack(Long id) {
    stack.remove(id);
  }
  
  public static void removeFromStack(EditorGui editor) {
    stack.remove(editor.getId());
  }

  public static ConcurrentHashMap<Long, EditorGui> getStack() {
    return stack;
  }

  public static void dispose(final EditorGui editor) {
    if(editor != null) {
      
      if(editor.isAutoStore())
        editor.store();

      if(editor.getDialog() != null)
        editor.getDialog().dispose();
      
      for(DivisionTarget target:editor.getTargets())
        target.dispose();
      
      if(editor.getInternalDialog() != null)
        editor.getInternalDialog().dispose();
      
      if(editor.getFrameDialog() != null)
        editor.getFrameDialog().dispose();
      
      removeFromStack(editor);
    }
  }
  
  public static void dispose() {
    EditorGui[] editors = stack.values().toArray(new EditorGui[0]);
    for(int i=0;i<editors.length;i++)
      dispose(editors[i]);
  }
}
