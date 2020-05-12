package division.util;

import java.util.concurrent.ConcurrentHashMap;

public class Hronometr {
  private static ConcurrentHashMap<String, Long> times = new ConcurrentHashMap<>();

  private Hronometr(){}

  public static void start(String name) {
    times.put(name, System.currentTimeMillis());
  }

  public static long stop(String name) {
    long period   = System.currentTimeMillis()-times.get(name);
    long hours    = period/1000/60/60;
    long minutes  = (period - hours*60*60*1000)/1000/60;
    long seconds  = (period - hours*60*60*1000 - minutes*60*1000)/1000;
    long mseconds = period - hours*60*60*1000 - minutes*60*1000 - seconds*1000;
    //System.out.println(name+": "+(period/1000/60/60)+":"+(period/1000/60)+":"+(period/1000)+":"+period);
    System.out.println(name+": "+hours+"ч:"+minutes+"м:"+seconds+"с:"+mseconds+"мс");
    return period;
  }
}