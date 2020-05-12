package bum.editors.exportImport;

import bum.editors.util.ObjectLoader;
import bum.interfaces.ExportImport;
import bum.interfaces.JSModul;
import division.util.FileLoader;
import division.util.Utility;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import mapping.MappingObject;
import util.filter.local.DBFilter;

public class ExportImportClass {
  //private static Executor pool = Executors.newFixedThreadPool(5);

  public static ExportImport getExportImport(Class<? extends MappingObject> clazz) throws RemoteException {
    ExportImport exportImport = null;
    Object[] objects = ObjectLoader.getObjects(DBFilter.create(ExportImport.class).AND_EQUAL("objectClassName", clazz.getName()));
    if(objects.length > 0)
      exportImport = (ExportImport)objects[0];
    return exportImport;
  }
  
  public static void export(Integer[] ids, Class<? extends MappingObject> clazz) throws Exception {
    export(new String[]{}, ids, clazz, null);
  }
  
  public static void export(Integer[] ids, Class<? extends MappingObject> clazz, Map<String,Object> variables) throws Exception {
    export(new String[]{}, ids, clazz, variables);
  }

  public static void export(String path, Integer[] ids, Class<? extends MappingObject> clazz) throws Exception {
    export(new String[]{path}, ids, clazz, null);
  }

  public static void export(String[] paths, Integer[] ids, Class<? extends MappingObject> clazz) throws Exception {
    export(paths, ids, clazz, null);
  }

  public static void export(String[] paths, Integer[] ids, Class<? extends MappingObject> clazz, Map<String,Object> variables) throws Exception {
    ExportImport exportImport = getExportImport(clazz);
    if(exportImport != null) {
      if(exportImport.isScript()) {
        String script = exportImport.getExportScript();
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.put("engine", engine);
        engine.put("ids",    ids);
        engine.put("paths",  paths);
        if(variables != null)
          for(String key:variables.keySet())
            engine.put(key, variables.get(key));
        for(MappingObject js:ObjectLoader.getObjects(JSModul.class))
          engine.eval(((JSModul)js).getScript());
        engine.eval(script);
      }else {
        for(MappingObject obj:ObjectLoader.getObjects(clazz, ids)) {
          Object eSplit = exportImport.getParams().get("EXPORT_SPLIT");
          if(eSplit == null)
            eSplit = ";";
          Vector<Vector<Map<String,Object>>> fields = (Vector<Vector<Map<String, Object>>>)exportImport.getParams().get("EXPORT_FIELDS");
          if(fields != null && !fields.isEmpty()) {
            String str = "";
            for(Vector<Map<String,Object>> field:fields) {
              Object object = obj;
              for(Map<String,Object> f:field) {
                Method method = ((Class)f.get("CLASS")).getMethod(f.get("GETMETHOD").toString(), new Class[0]);
                if(object == null)
                  break;
                object = method.invoke(object, new Object[0]);
                if(object instanceof java.sql.Date)
                  object = Utility.format((java.sql.Date)object);
                else if(object instanceof java.util.Date)
                  object = Utility.format((java.util.Date)object);
                else if(object instanceof java.sql.Timestamp)
                  object = Utility.format(new java.util.Date(((java.sql.Timestamp)object).getTime()));
              }
              str += (object==null?"":object)+eSplit.toString();
            }

            for(String path:paths) {
              if(path != null && !path.equals("")) {
                System.out.println("RECORD TO "+path);
                File flg = FileLoader.createFileFlag(path);
                for(int i=0;i<4;i++) {
                  if(flg == null) {
                    Thread.sleep(1000);
                    flg = FileLoader.createFileFlag(path);
                  }else break;
                }
                if(flg == null) {
                  JOptionPane.showMessageDialog(null, "Файл импорта занят", "Файл импорта занят", JOptionPane.ERROR_MESSAGE);
                }else {
                  FileLoader.createFileIfNotExists(path);
                  FileWriter out = new FileWriter(path,true);
                  out.write(str+"\r\n");
                  out.flush();
                  out.close();
                  flg.delete();
                }
              }
            }
            System.out.println("EXPORT "+clazz.getSimpleName()+": <"+str+">\r\n");
          }
        }
      }
    }
  }
}
/**
 * Жарко на улице
 * Нет больше сил
 * К водичке холодной
 * Бежит крокодил
 * Люди потея едут в метро
 * Лёжа на улице воняет говно
 *
 * Мокрые лица, липкие руки
 * Как надоели эти тёплые муки
 * Юбки прилипли к мокрым ногам
 * Трусики делят зад по полам
 * Портится сало, тухнет шашлык
 * Пахнет девчушка, пахнет балык)
 */