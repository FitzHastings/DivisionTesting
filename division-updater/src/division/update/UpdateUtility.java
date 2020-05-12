package division.update;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class UpdateUtility {
  private static final String updatepath = "applications";
  private static final String fileList   = "file-list.txt";
  
  public static Map<String, String> getMD5Files(String dirName, String fileName) throws Exception {
    File file = new File(fileName);
    Map<String, String> files = new TreeMap<>();
    if(file.exists()) {
      if(!file.isDirectory()) {
        String key = dirName==null?file.getPath():file.getAbsolutePath().substring(new File(dirName).getAbsolutePath().length()+1);
        files.put(key, getMD5(file));
      }else {
        for(File f:file.listFiles((File dir, String name) -> !name.endsWith("~")))
          if(!f.isHidden())
            files.putAll(getMD5Files(dirName, f.getPath()));
      }
    }
    return files;
  }
  
  public static String getMD5(File f) throws Exception {
    String output = "";
    InputStream is = null;
    MessageDigest digest = MessageDigest.getInstance("MD5");
    try {
      is = new FileInputStream(f);
      byte[] buffer = new byte[819200];
      int read = 0;
      while ((read = is.read(buffer)) > 0) {
        digest.update(buffer, 0, read);
      }
      byte[] md5sum = digest.digest();
      BigInteger bigInt = new BigInteger(1, md5sum);
      output = bigInt.toString(16);
    }catch(Exception ex) {
      throw new Exception(ex);
    }finally {
      if(is != null)
        is.close();
      is = null;
    }
    return output;
  }
  
  public static String validatePath(String path) throws Exception {
    return path==null?null:path.replaceAll("[\\\\/]", "\\"+File.separator);
  }
  
  public static byte[] gzip(byte[] source) throws IOException {
    if(source == null)
      return new byte[0];
    
    ByteArrayOutputStream bos = null;
    GZIPOutputStream zop = null;
    
    try {
      bos = new ByteArrayOutputStream();
      zop = new GZIPOutputStream(bos);
      zop.write(source);
      zop.flush();
      zop.finish();
      zop.close();
      source = null;
      source = bos.toByteArray();
    }catch(IOException ex) {
      throw new IOException(ex);
    }finally {
      if(bos != null) {
        bos.reset();
        bos.flush();
        bos.close();
      }
      if(zop != null) {
        zop.flush();
        zop.finish();
        zop.close();
      }
      zop = null;
      bos = null;
    }
    return source;
  }
  
  public static byte[] ungzip(byte[] source) throws IOException {
    if(source != null && source.length > 0) {
      ByteArrayInputStream bis = null;
      GZIPInputStream zip = null;
      ByteArrayOutputStream bos = null;

      try {
        bis = new ByteArrayInputStream(source);
        zip = new GZIPInputStream(bis);
        bos = new ByteArrayOutputStream();
        int read;
        byte[] buf=new byte[1024];
        while((read=zip.read(buf))!=-1)
          bos.write(buf,0,read);
        source = bos.toByteArray();
      }catch(IOException ex) {
        throw new IOException(ex);
      }finally {
        if(bis != null) {
          bis.reset();
          bis.close();
        }
        if(zip != null)
          zip.close();
        if(bos != null) {
          bos.reset();
          bos.flush();
          bos.close();
        }
        zip = null;
        bis = null;
        bos = null;
      }
    }
    return source;
  }
  
  public static void delete(Path path) throws IOException {
    if(Files.isDirectory(path)) {
      for(Object o:Files.list(path).toArray())
        delete((Path)o);
    }
    Files.delete(path);
  }
  
  public static File createFileIfNotExists(String fileName, boolean createFile) {
    File file;
    if(fileName.lastIndexOf(File.separator)>0) {
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
    }else return file;
  }
  
  //SERVER
  public static boolean validateCommand(Command command) {
    if(!Files.exists(Paths.get(updatepath+File.separator+command.getApplication()))) {
      command.setCommand("warning");
      command.setData("Приложение отсутствует");
      return false;
    }
    if(!Files.exists(Paths.get(updatepath+File.separator+command.getApplication()+File.separator+fileList))) {
      command.setCommand("warning");
      command.setData("Отсутствует файл списка обновлений");
      return false;
    }
    return true;
  }
  
  public static void getUpdateList(Command command) {
    try {
      command.setData(Files.readAllLines(Paths.get(updatepath+File.separator+command.getApplication()+File.separator+fileList)));
      command.setCommand("OK");
    }catch(Exception ex) {error(command, ex);}
  }
  
  public static void getUpdateInstruction(Command command) {
    try {
      Map<String, String> clientFiles = (Map<String,String>) command.getData();
      for(String fileName:clientFiles.keySet().toArray(new String[0]))
        clientFiles.put(validatePath(fileName), clientFiles.remove(fileName));
      
      Map<String, String> serverFles  = new TreeMap<>();
      for(String fileName:Files.readAllLines(Paths.get(updatepath+File.separator+command.getApplication()+File.separator+fileList)))
        serverFles.putAll(getMD5Files(updatepath+File.separator+command.getApplication(), validatePath(updatepath+File.separator+command.getApplication()+File.separator+fileName)));
      
      Map<String, String> updates = new TreeMap<>();
      for(String fileName:serverFles.keySet()) {
        if(!clientFiles.containsKey(fileName))
          updates.put(fileName, "create");
        else if(!clientFiles.get(fileName).equals(serverFles.get(fileName)))
          updates.put(fileName, "update");
      }
      
      clientFiles.keySet().removeAll(serverFles.keySet());
      for(String fileName:clientFiles.keySet())
        updates.put(fileName, "remove");
      
      command.setData(updates);
      command.setCommand("OK");
    }catch(Exception ex) {error(command, ex);}
  }
  
  public static void getFile(Command command) {
    try {
      command.setData(gzip(Files.readAllBytes(Paths.get(validatePath(updatepath+File.separator+command.getApplication()+File.separator+command.getData())))));
      command.setCommand("OK");
    }catch(Exception ex) {error(command, ex);}
  }
  
  public static void error(Command command, Exception ex) {
    command.setCommand("ERROR");
    String errorMessage = ex.toString()+"\n";
    for(StackTraceElement element:ex.getStackTrace())
      errorMessage += "     "+element.toString()+"\n";
    command.setData(errorMessage);
  }
  
  public static void startServer() {
    try {
      Properties prop = getProperties();
      String rminame = prop.getProperty("name");
      String rmihost = prop.getProperty("host");
      int    rmiPort = Integer.valueOf(prop.getProperty("rmi-port"));
      int    objPort = Integer.valueOf(prop.getProperty("object-port"));

      System.setProperty("java.rmi.server.hostname", rmihost);
      Registry registry = LocateRegistry.createRegistry(rmiPort);

      UpdaterImpl server = new UpdaterImpl(objPort, new DivisionClientSocketFactory(rmihost), new DivisionServerSocketFactory());

      registry.rebind(rminame, server);
      System.out.println("Запущен "+rminame+": host: "+rmihost+" port:"+rmiPort+" object-port:"+objPort);
    }catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  
  //CLIENT
  
  private static void startClient() {
    Properties prop = null;
    try {
      prop = getProperties();
      if(prop != null && prop.getProperty("update").equals("on"))
        UpdateUtility.updateClient("division");
    }catch(Exception ex) {
      error(ex);
    }
    try {
      if(prop.getProperty("command") != null && !prop.getProperty("command").equals("")) {
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.addAll(Arrays.asList(prop.getProperty("options").split(" ")));
        commands.add(prop.getProperty("command"));
        new ProcessBuilder().command(commands).start();
      }
    }catch(Exception ex) {
      error(ex);
    }
    System.exit(0);
  }
  
  private static void error(Exception ex) {
    String errorMessage = ex.toString()+"\n";
    for(StackTraceElement element:ex.getStackTrace())
      errorMessage += "     "+element.toString()+"\n";
    
    JTextArea area = new JTextArea(errorMessage);
    JScrollPane scroll = new JScrollPane(area);
    scroll.setPreferredSize(new Dimension(500, 300));
    
    if(JOptionPane.showOptionDialog(null, scroll, "Ошибка", JOptionPane.YES_NO_OPTION, 
            JOptionPane.ERROR_MESSAGE, null, new String[]{"Продолжить","Закрыть"},"Продолжить") == 1)
      System.exit(0);
  }
  
  private static Properties getProperties() throws IOException {
    File conf = createFileIfNotExists("conf"+File.separator+"updater.properties", true);
    
    Properties prop = new Properties();
    prop.load(new FileInputStream(conf));
    
    boolean is = !prop.containsKey("name") || !prop.containsKey("host") || 
            !prop.containsKey("rmi-port") || !prop.containsKey("object-port") || 
            !prop.containsKey("update") || !prop.containsKey("options") || !prop.containsKey("command");
    
    if(!prop.containsKey("name"))
      prop.setProperty("name", "update-division");
    if(!prop.containsKey("host"))
      prop.setProperty("host", "192.168.1.1");
    if(!prop.containsKey("rmi-port"))
      prop.setProperty("rmi-port", "8002");
    if(!prop.containsKey("object-port"))
      prop.setProperty("object-port", "8003");
    
    if(!prop.containsKey("update"))
      prop.setProperty("update", "on");
    if(!prop.containsKey("options"))
      prop.setProperty("options", "-Xmx500m -splash:images/splash.gif");
    if(!prop.containsKey("command"))
      prop.setProperty("command", "division-plugin-editors.jar");
    if(is)
      prop.store(new FileOutputStream(conf), "");
    return prop;
  }
  
  public static void updateClient(String application) throws Exception {
    Properties prop = getProperties();
    String  name = prop.getProperty("name");
    String  host = prop.getProperty("host");
    Integer port = Integer.valueOf(prop.getProperty("rmi-port"));
    
    Updater updater = null;
    
    try {
      updater = (Updater) LocateRegistry.getRegistry(host, port).lookup(name);
    }catch(Exception ex) {throw new Exception("Не удалось запросить обновление пакетов\n"+host+"//"+name+":"+port, ex);}
    
    Command command = updater.command(new Command(application, "get-update-list", null));
    if(command.getCommand().equals("OK")) {
      List<String> list = (List<String>) command.getData();
      
      Map<String,String> clientFiles = new TreeMap<>();
      for(String fileName:list)
        clientFiles.putAll(getMD5Files(null, validatePath(fileName)));
      
      command = updater.command(new Command(application, "get-update-instruction", clientFiles));
      if(command.getCommand().equals("OK")) {
        Map<String,String> serverFiles = (Map<String,String>) command.getData();
        if(!serverFiles.isEmpty()) {
          
          String text = "Обновляемые пакеты:\n";
          for(String fileName:serverFiles.keySet())
            text += "  "+fileName+" - "+serverFiles.get(fileName)+"\n";
          
          JTextArea area = new JTextArea(text);
          JScrollPane scroll = new JScrollPane(area);
          scroll.setPreferredSize(new Dimension(500, 300));
          
          if(JOptionPane.showOptionDialog(null, scroll, "Доступно обновление...", JOptionPane.YES_NO_OPTION, 
                  JOptionPane.QUESTION_MESSAGE, null, new String[]{"Обновить","Не обновлять"}, "Обновить") == 0) {
            for(String fileName:serverFiles.keySet()) {
              if(serverFiles.get(fileName).equals("remove"))
                delete(Paths.get(validatePath(fileName)));
              else {
                command = updater.command(new Command(application, "get-file", fileName));
                if(command.getCommand().equals("OK")) {
                  if(serverFiles.get(fileName).equals("update"))
                    delete(Paths.get(validatePath(fileName)));
                  Files.write(
                          createFileIfNotExists(validatePath(fileName), false).toPath(), 
                          ungzip((byte[])command.getData()), 
                          StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                }
              }
              System.out.println(serverFiles.get(fileName)+" "+validatePath(fileName));
            }
          }
        }
      }else throw new Exception("Ошибка при получении инструкций обновления...");
    }else throw new Exception("Ошибка при получении списка обновляемых пакетов...");
  }
  
  
  
  public static void main(String[] args) {
    if(args.length == 0)
      startClient();
    else if(args[0].equals("server"))
      startServer();
    else if(args[0].equals("stop")) {
      try {
        Properties prop = getProperties();
        String  name = prop.getProperty("name");
        String  host = prop.getProperty("host");
        Integer port = Integer.valueOf(prop.getProperty("rmi-port"));

        Updater updater = null;

      try {
        updater = (Updater) LocateRegistry.getRegistry(host, port).lookup(name);
        updater.command(new Command("all", "stop", ""));
      }catch(Exception ex) {System.out.println("Не удалось подключиться к серверу обновлений...");}
      }catch(Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}