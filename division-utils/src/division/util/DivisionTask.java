package division.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javafx.scene.Cursor;
import javafx.scene.layout.Region;

public abstract class DivisionTask implements Runnable {
  private Long id = IDStore.createID();
  private String name;
  private Region cursorPane = null;
  
  public static ExecutorService pool = Executors.newCachedThreadPool();

  private boolean run = false;

  public DivisionTask() {
    this(DivisionTask.class.getSimpleName());
  }

  public DivisionTask(String name) {
    this(name, null);
  }
  
  public DivisionTask(String name, Region cursorPane) {
    this.name = name;
    this.cursorPane = cursorPane;
  }
  
  
  
  private synchronized void start() {
    run = true;
    System.out.println("start thread "+name);
    if(cursorPane != null) {
      cursorPane.setCursor(Cursor.WAIT);
      cursorPane.getChildrenUnmodifiable().stream().forEach(n -> n.setCursor(Cursor.WAIT));
    }
    Hronometr.start(name);
  }
  
  private synchronized void stop() {
    run = false;
    if(cursorPane != null) {
      cursorPane.setCursor(Cursor.DEFAULT);
      cursorPane.getChildrenUnmodifiable().stream().forEach(n -> n.setCursor(Cursor.DEFAULT));
    }
    Hronometr.stop(name);
    System.out.println("stop thread "+name);
  }
  
  
  public static void start(DivisionTask task) {
    if(task != null && !task.isRun()) {
      task.start();
      pool.submit(() -> {
        Future f = pool.submit(task);
        while(!f.isDone() || (!f.isDone() && !f.isCancelled())) {}
        task.stop();
      });
    }
  }
  
  public static void start(Runnable task) {
    pool.submit(task);
  }
  
  public static void stop(DivisionTask task) {
    if(task != null && task.isRun())
      task.stop();
    task = null;
  }
  
  
  
  public boolean isRun() {
    return run;
  }
  
  protected void checkShutdoun() throws DivisionTaskException {
    if(!isRun())
      throw new DivisionTaskException();
  }

  @Override
  public void run() {
    try {
      task();
    }catch (DivisionTaskException ex) {
      System.out.println("forced exit thread "+name);
    }
  }

  public Long getId() {
    return id;
  }
  
  public abstract void task() throws DivisionTaskException;
  
  public class DivisionTaskException extends Exception {
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 11 * hash + Objects.hashCode(this.id);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DivisionTask other = (DivisionTask) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return true;
  }
}
