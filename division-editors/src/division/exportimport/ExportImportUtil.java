package division.exportimport;

import bum.editors.util.ObjectLoader;
import division.swing.LocalProcessing;
import division.swing.guimessanger.Messanger;
import division.util.FileLoader;
import division.xml.Document;
import division.xml.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;

public class ExportImportUtil {
  public static void export(String fileName, Class<? extends MappingObject> objectClass, Integer id) {
    export(new String[]{fileName}, objectClass, id);
  }
  
  public static void export(String fileName, Class<? extends MappingObject> objectClass, Integer[] ids) {
    export(new String[]{fileName}, objectClass, ids);
  }
  
  public static void export(String[] paths, Class<? extends MappingObject> objectClass, Integer id) {
    export(paths, objectClass, new Integer[]{id});
  }
  
  public static void export(final Class<? extends MappingObject> objectClass, Integer id) {
    export(objectClass, new Integer[]{id});
  }
  
  public static void export(final Class<? extends MappingObject> objectClass, Integer[] ids) {
    export(getExportPaths(), objectClass, ids);
  }
  
  private static final ExecutorService pool = Executors.newSingleThreadExecutor();
  
  public static void export(final String[] paths, final Class<? extends MappingObject> objectClass, final Integer[] ids) {
    if(paths != null && paths.length > 0 && ids.length > 0) {
      final LocalProcessing processing = new LocalProcessing();
      processing.setMinMax(0, ids.length);
      processing.setTitle("Экспорт данных");
      processing.submit(() -> {
        try {
          String topic = "exportData-"+System.currentTimeMillis();
          
          ObjectLoader.addSubscriber(topic, new MessageListener() {
            TreeMap<String,Document> documents = new TreeMap<>();
            
            @Override
            public void onMessage(Message msg) {
              try {
                processing.setValue(processing.getValue()+100);
                Document data = (Document)((ObjectMessage)msg).getObject();
                for(String fileName:paths) {
                  Document document = documents.get(fileName);
                  if(document == null) {
                    document = Document.load(fileName);
                    if(document == null)
                      document = Document.load(fileName, true);
                    documents.put(fileName, document);
                  }
                  for(Node node:((Document)data).getRootNode().getNodes())
                    document.getRootNode().addNode(node);
                  System.out.println("document.save() = "+document.save());
                  long length = new File(fileName).length();
                  System.out.println("Записал порцию (файл: "+((double)length/1024/1024)+" Mb)");
                }
              }catch(Exception ex) {
                Messanger.showErrorMessage(ex);
              }
            }
          });
          processing.setValue(1);
          ObjectLoader.getServer().getExportData(objectClass, ids, 100, topic);
        }catch(Exception ex) {
          Messanger.showErrorMessage(ex);
        }
      });
    }
  }
  
  public static void setExportPath(Integer partitionId, String exportPath) {
    try {
      Properties exportpath = new Properties();
      exportpath.load(new FileInputStream(FileLoader.createFileIfNotExists("conf"+File.separator+"export-path-list.properties")));
      exportpath.setProperty(String.valueOf(partitionId), exportPath);
      exportpath.store(new FileOutputStream("conf"+File.separator+"export-path-list.properties"), "");
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
  
  public static String getExportPath(Integer partitionId) {
    try {
      Properties exportpath = new Properties();
      exportpath.load(new FileInputStream(FileLoader.createFileIfNotExists("conf"+File.separator+"export-path-list.properties")));
      return exportpath.getProperty(String.valueOf(partitionId));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return null;
  }
  
  public static String[] getExportPaths() {
    String[] paths = new String[0];
    try {
      Properties exportpath = new Properties();
      exportpath.load(new FileInputStream(FileLoader.createFileIfNotExists("conf"+File.separator+"export-path-list.properties")));
      Enumeration en = exportpath.propertyNames();
      while(en.hasMoreElements())
        paths = ArrayUtils.add(paths, exportpath.getProperty(en.nextElement().toString()));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    return paths;
  }
}