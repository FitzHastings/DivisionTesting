package bum.pool;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

public class DivisionObjectPool {
  private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
  public ArrayList<SoftReference> objects = new ArrayList<>();
  private final Class objectClass;

  public DivisionObjectPool(Class objectClass) {
    this.objectClass = objectClass;
  }
  
  public Object getObject() {
    if(objects.isEmpty()) {
      threadPool.submit(new ObjectTask());
      return createObject();
    }else return objects.remove(0).get();
  }
  
  private Object createObject() {
    try {
      return objectClass.newInstance();
    }catch(InstantiationException | IllegalAccessException ex) {
      Logger.getRootLogger().error("", ex);
    }
    return null;
  }
  
  private class ObjectTask implements Runnable {
    @Override
    public void run() {
      for(int i=0;i<20;i++)
        objects.add(new SoftReference(createObject()));
    }
  }
}