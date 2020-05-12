package division.fx.scale.test;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.pool2.BaseObjectPool;

public class OPool<T> extends BaseObjectPool<T> {
  private final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue();
  private final Class<T> objectClass;
  private int min = 1000;

  public OPool(Class<T> cl, int min) {
    this.min = min;
    this.objectClass = cl;
    min();
  }

  private void min() {
    try {
      for(int i=0;i<min;i++)
        list.add(objectClass.newInstance());
    }catch(InstantiationException | IllegalAccessException ex) {
      ex.printStackTrace();
    }
  }
  
  public int size() {
    return list.size();
  }

  @Override
  public T borrowObject() {
    if(list.isEmpty())
      min();
    return list.remove();
  }
  
  @Override
  public void clear() {
    list.clear();
  }
  
  @Override
  public void returnObject(T t) throws Exception {
    //returnObject(Arrays.asList(t));
  }
  
  public void returnObject(T... objects) throws Exception {
    //returnObject(Arrays.asList(objects));
  }
  
  public void returnObject(Collection<T> objects) {
    //list.addAll(objects);
  }

  @Override
  public void invalidateObject(T t) throws Exception {
  }
}