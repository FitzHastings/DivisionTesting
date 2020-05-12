package util;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;

public class Paths {
  public static String CONF_PATH    = "conf" + File.separator;
  public static String LIBS_PATH    = "libs"+File.separator;
  public static String PLUGINS_PATH = "plugins"+File.separator;
  public static String IMAGES_PATH  = "images" + File.separator;
  private static Logger logger = Logger.getRootLogger();
  
  static
  {
    createIfNotExist(CONF_PATH,false);
    //createIfNotExist(LIBS_PATH,false);
    //createIfNotExist(PLUGINS_PATH,false);
    //createIfNotExist(IMAGES_PATH,false);
  }
  
  public static synchronized void createIfNotExist(String fileName,boolean isFile)
  {
    try
    {
      File file = new File(fileName);
      if(!file.exists())
      {
        if(isFile)
        {
          new File(fileName.substring(0,fileName.lastIndexOf(File.separator))).mkdirs();
          file.createNewFile();
        }else new File(fileName).mkdirs();
      }
    }catch(IOException ex) {
      logger.error("", ex);
    }
  }
}