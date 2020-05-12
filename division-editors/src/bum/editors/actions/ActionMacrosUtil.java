package bum.editors.actions;

import bum.editors.JSModulEditor;
import bum.editors.util.ObjectLoader;
import bum.interfaces.JScript;
import division.swing.guimessanger.Messanger;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import mapping.MappingObject;
import org.apache.commons.lang3.ArrayUtils;
import util.filter.local.DBFilter;

public class ActionMacrosUtil {
  
  public static JMenuItem[] createMacrosMenu(Class<? extends MappingObject> objectClass, final Map<String,Object> scriptMap) throws Exception {
    JMenuItem[] menuItems = new JMenuItem[0];
    List<List> data = ObjectLoader.getData(DBFilter.create(JScript.class).AND_EQUAL("sourceClass", objectClass==null?objectClass:objectClass.getName()).AND_EQUAL("type", MappingObject.Type.CURRENT).AND_EQUAL("tmp", false), new String[]{"name","script", "scriptLanguage"});
    if(!data.isEmpty()) {
      for(final List d:data) {
        JMenuItem item = new JMenuItem(d.get(0).toString());
        item.addActionListener((ActionEvent e) -> {
          try {
            JSModulEditor.runScript((String)d.get(0), (String)d.get(1), (String)d.get(2), scriptMap);
          } catch (Exception ex) {
            Messanger.showErrorMessage(ex);
          }
        });
        menuItems = (JMenuItem[])ArrayUtils.add(menuItems, item);
      }
    }
    return menuItems;
  }
  
  /*private static void startScript(Integer scriptId, Map<String,Object> scriptMap) {
    try {
      if(scriptMap == null)
        scriptMap = new TreeMap<>();
      JScript scriptObject = (JScript) ObjectLoader.getObject(JScript.class, scriptId);
      String script = scriptObject.getScript();

      if(script != null && !script.equals("")) {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        for(String key:scriptMap.keySet())
          engine.put(key, scriptMap.get(key));
      
        for(MappingObject js:ObjectLoader.getObjects(JSModul.class))
          engine.eval(((JSModul)js).getScript());
        engine.eval(script);
      }
    }catch(RemoteException | ScriptException e) {
      Messanger.showErrorMessage(e);
    }
  }*/
}