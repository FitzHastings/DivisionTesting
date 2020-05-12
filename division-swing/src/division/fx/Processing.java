package division.fx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Processing {
  private final static ExecutorService pool = Executors.newCachedThreadPool();
  
  public static void submit(Runnable process) {
    pool.submit(process);
  }
}