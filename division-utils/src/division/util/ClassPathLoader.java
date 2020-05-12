package division.util;



import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

public class ClassPathLoader {
  private static final String FILE_PROTOCOL_PREFIX = "file:";

  private static FilenameFilter filter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      if(name.lastIndexOf(".") < 0)
        return false;
      return name.substring(name.lastIndexOf(".")).equals(".jar");
    }
  };
  
  public static String findFile(String fileName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    Field field = ClassLoader.class.getDeclaredField("sys_paths");
    field.setAccessible(true);
    String[] sys_paths = (String[]) field.get(null);
    File f = null;
    for(String s:sys_paths) {
      f = new File(s);
      if(f.isDirectory()) {
        f = new File(f.getAbsolutePath()+File.separator+fileName);
        if(f.exists())
          return f.getAbsolutePath();
      }else {
        if(f.getName().equals(fileName))
          return f.getAbsolutePath();
      }
    }
    return null;
  }
  
  public static void addOnliThisFile(File file) throws MalformedURLException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    addClassPathURL(FILE_PROTOCOL_PREFIX+file.getAbsolutePath());
    addToLibrary(file.getAbsolutePath());
  }

  public static void addFile(File file) throws MalformedURLException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    addClassPathURL(FILE_PROTOCOL_PREFIX+file.getAbsolutePath());
    addToLibrary(file.getAbsolutePath());
    if(file.isDirectory()) {
      File[] files = file.listFiles();
      for(File f:files)
        if(f.isDirectory() || filter.accept(file, f.getName()))
          addFile(f);
    }
  }
  
  private static void addToLibrary(String path) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    Field field = ClassLoader.class.getDeclaredField("sys_paths");
    field.setAccessible(true);
    String[] sys_paths = (String[]) field.get(null);
    
    if(!ArrayUtils.contains(sys_paths, path)) {
      sys_paths = (String[]) ArrayUtils.add(sys_paths, path);
      field.set(null, sys_paths);
    }
    sys_paths = null;
  }

  private static void addClassPathURL(String path) throws MalformedURLException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Logger.getRootLogger().debug("ADD TO CLASSPATH URL "+path);
    // URL файла для добавления к classpath
    URL u = new URL(path);
    // достаем системный загрузчик классов
    URLClassLoader urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
    // используя механизм отражения,
    // достаем метод для добавления URL к classpath
    Class urlClass = URLClassLoader.class;
    Method method = urlClass.getDeclaredMethod("addURL",new Class[]{URL.class});
    // делаем метод доступным для вызова
    method.setAccessible(true);
    // вызываем метод системного загрузчика,
    // передавая в качестве параметра
    // URL файла для добавления к classpath
    method.invoke(urlClassLoader, new Object[]{u});
  }
}