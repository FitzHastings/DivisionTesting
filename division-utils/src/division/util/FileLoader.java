package division.util;

import division.xml.Document;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;

public class FileLoader {
  public static String getFlagName(String fileName) {
    int index = fileName.lastIndexOf(".");
    return fileName.substring(0, index==-1?0:index)+".flg";
  }

  public static File createFileFlag(String fileName) throws IOException {
    File file = new File(getFlagName(fileName));
    if(file.exists())
      return null;
    else
      return createFileIfNotExists(file.getAbsolutePath());
  }

  public static File createFileIfNotExists(String fileName) throws IOException {
    return createFileIfNotExists(fileName, true);
  }
  
  public static void deleteFile(String fileName) {
    deleteFile(new File(fileName));
  }

  public static void deleteFile(File file) {
    if(file.exists()) {
      System.gc();
      if(!file.delete())
        file.deleteOnExit();
    }
  }
  
  public static void delete(Path path) throws IOException {
    if(Files.isDirectory(path)) {
      for(Object o:Files.list(path).toArray())
        delete((Path)o);
    }
    Files.delete(path);
  }

  public static File createFileIfNotExists(String fileName, boolean createFile) {
    File file = new File(fileName);
    try {
      if(file.getParentFile() != null)
        Files.createDirectories(file.getParentFile().toPath());
      if(createFile && !Files.exists(Paths.get(fileName)))
        Files.createFile(Paths.get(fileName));
    }catch(Exception ex) {
      ex.printStackTrace();
      return null;
    }
    
    return file;
    /*File file;
    if(fileName.lastIndexOf(File.separator) > 0) {
      file = new File(fileName.substring(0, fileName.lastIndexOf(File.separator)));
      if(!file.exists())
        file.mkdirs();
    }
    file = new File(fileName);
    if(createFile && !file.exists()) {
      try {
        if(file.createNewFile())
          return file;
        else return null;
      }catch(IOException ex) {
        ex.printStackTrace();
        return null;
      }
    }else return file;*/
  }

  public static ImageIcon getIcon(String fileName) {
    ImageIcon icon = null;
    if(icon == null)
      try {
        icon = new ImageIcon(FileLoader.class.getClassLoader().getResource(fileName));
        System.out.println(FileLoader.class.getClassLoader().getResource(fileName).getFile());
      }catch(Exception ex){}
    if(icon == null)
      try {
        icon = new ImageIcon(FileLoader.class.getResource(fileName));
      }catch(Exception ex){}
    if(icon == null) {
      try {
        FileLoader.createFileIfNotExists(fileName);
        icon = new ImageIcon(fileName);
      }catch(Exception ex){}
    }
    return icon;
  }

  public static Properties loadXMLProperties(String fileName) throws IOException {
    FileInputStream in = null;
    try {
      in = new FileInputStream(createFileIfNotExists(fileName));
      Properties prop = new Properties();
      prop.loadFromXML(in);
      return prop;
    }catch(Exception ex) {
      throw new IOException(ex);
    }finally {
      if(in != null)
        in.close();
      in = null;
    }
  }

  public static Properties loadProperties(String fileName) throws IOException {
    FileInputStream in = null;
    try {
      in = new FileInputStream(createFileIfNotExists(fileName));
      Properties prop = new Properties();
      prop.load(in);
      return prop;
    }catch(Exception ex) {
      throw new IOException(ex);
    }finally {
      if(in != null)
        in.close();
      in = null;
    }
  }
  
  public static void storeProperties(String fileName, Properties properties) throws IOException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(createFileIfNotExists(fileName));
      properties.store(out, "");
    }catch(IOException ex) {
      throw new IOException(ex);
    }finally {
      if(out != null) {
        out.flush();
        out.close();
      }
      out = null;
    }
  }

  public static void storeXMLProperties(String fileName, Properties properties) throws IOException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(createFileIfNotExists(fileName));
      properties.storeToXML(out, "", "UTF8");
    }catch(IOException ex) {
      throw new IOException(ex);
    }finally {
      if(out != null) {
        out.flush();
        out.close();
      }
      out = null;
    }
  }

  public static Object loadObject(String fileName) throws Exception {
    ObjectInputStream in = null;
    Object object = null;
    try {
      in = new ObjectInputStream(new FileInputStream(createFileIfNotExists(fileName)));
      object = in.readObject();
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(in != null) {
        in.reset();
        in.close();
      }
      in = null;
    }
    return object;
  }

  public static void storeObject(Object object, String fileName) throws Exception {
    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream(new FileOutputStream(createFileIfNotExists(fileName)));
      out.writeObject(object);
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(out != null) {
        out.reset();
        out.flush();
        out.close();
      }
      out = null;
    }
  }

  public static String getPid() throws IOException, InterruptedException {
    // Запускаем bash с параметром -c, что означает "выполнить команду"
    // Командой будет "echo $PPID" - вывести PID родительского процесса
    ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "echo $PPID");
    Process pr = pb.start();
    pr.waitFor();
    // Если процесс завершился успешно, вернул код 0, считаем, что он нам вывел
    if(pr.exitValue() == 0) {
      BufferedReader outReader=new BufferedReader(new InputStreamReader(pr.getInputStream()));
      String pid = outReader.readLine().trim();
      outReader.close();
      return pid;
    }else {
      return null;
    }
  }

  /*public static File createPidFile(String name) throws Exception {
    String fileName = System.getProperty("java.io.tmpdir")+File.separator+name+".txt";
    Logger.getRootLogger().info("CREATE PIDFILE "+fileName);
    File file = new File(fileName);
    if(file.exists()) {
      return null;
    }else {
      file = FileLoader.createFileIfNotExists(fileName);
      file.deleteOnExit();
      FileOutputStream out = new FileOutputStream(file);
      out.write(FileLoader.getPid().getBytes());
      out.flush();
      out.close();
      return file;
    }
  }

  public static String getPid(String name) throws Exception {
    String fileName = System.getProperty("java.io.tmpdir")+File.separator+name+".txt";
    File file = new File(fileName);
    if(!file.exists()) {
      Logger.getRootLogger().error("NOT FOUND PIDFILE "+fileName);
      return null;
    }else {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String pid = reader.readLine();
      reader.close();
      return pid;
    }
  }*/

  public static void loadLibs(String confFile) {
    Document.load(confFile).getNode("libs").getNodes().stream().forEach(node -> {
      try {
        File file = new File(node.getValue());
        if(file.exists()) {
          ClassPathLoader.addFile(file);
          if(file.isDirectory())
            Arrays.stream(file.listFiles()).forEach(f -> loadJar(f));
          else loadJar(file);
        }
      }catch(MalformedURLException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException ex) {
        Logger.getRootLogger().error(ex);
      }
    });
  }
  
  public static void loadJar(File file) {
    JarFile jarFile = null;
    try {
      jarFile = new JarFile(file);
      Enumeration<JarEntry> en = jarFile.entries();
      while(en.hasMoreElements()) {
        JarEntry entry = en.nextElement();
        if(!entry.isDirectory() && entry.getName().endsWith(".class")) {
          String name = entry.getName().substring(0, entry.getName().lastIndexOf(".class")).replaceAll("/", ".");
          if(name.contains("$"))
            name = name.substring(0, name.indexOf("$"));
          Logger.getRootLogger().info("LOAD CLASS: "+name);
          Class.forName(name);
        }
      }
    }catch (IOException | ClassNotFoundException ex) {
      Logger.getRootLogger().error(ex);
    }finally {
      try{jarFile.close();}catch(Exception ex){}
    }
  }
}