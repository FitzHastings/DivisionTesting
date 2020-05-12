package division.util;

import division.fx.PropertyMap;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.apache.commons.lang3.ArrayUtils;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class ScriptUtil {
  private static NashornScriptEngine jsEngine     = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("JavaScript");
  private static GroovyScriptEngine  engine;
  
  private static String[] getPathToGroovyEngine(String... paths) {
    String[] urls = new            String[0];
    for(String p:paths) {
      File f = new File(p);
      if(f.exists() && f.isDirectory()) {
        urls = ArrayUtils.add(urls, f.getAbsolutePath());
        for(String l:f.list())
          urls = ArrayUtils.addAll(urls, getPathToGroovyEngine(f.getAbsolutePath()+File.separator+l));
      }
    }
    return urls;
  }
  
  public static void loadGroovyScripts(String... paths) throws Exception {
    System.setProperty("groovy.source.encoding", "UTF-8");
    paths = getPathToGroovyEngine(paths);
    engine = new GroovyScriptEngine(paths);
    for(String path:paths) {
      engine.getGroovyClassLoader().addClasspath(path);
      File f = new File(path);
      if(f.exists()) {
        for(String clazz:f.list((File dir, String name) -> name.endsWith(".groovy"))) {
          Class cl = engine.getGroovyClassLoader().loadClass(clazz.substring(0, clazz.lastIndexOf(".")));
          System.out.println("LOADED CLASS "+cl.getName());
        }
      }
    }
  }
  
  public static void initFXML(String... paths) throws Exception {
    for(String path:getPathToGroovyEngine(paths)) {
      File f = new File(path);
      if(f.exists()) {
        for(String clazz:f.list((File dir, String name) -> name.endsWith(".fxml"))) {
          FXMLLoader loader = new FXMLLoader(f.toURI().toURL());
          loader.setClassLoader(ScriptUtil.class.getClassLoader());
          loader.load();
          System.out.println("LOADED FXML "+f.getName());
        }
      }
    }
  }
  
  public static void runGroovyClass(String scriptName, Map<String,Object> scriptParam) throws Exception {
    Binding binding = new Binding();
    binding.setVariable("engine", engine);
    scriptParam.keySet().stream().forEach(name -> binding.setVariable(name, scriptParam.get(name)));
    engine.run(scriptName+".groovy", binding);
  }
  
  public static Object runScript(String scriptName, String scriptText, String scriptLanguage, Map<String, Object> scriptParam) throws ScriptException {
    Object returnObject = scriptName;
    if(scriptText != null && !scriptText.equals("")) {
      switch(scriptLanguage) {
        case SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT:
          if(scriptParam != null)
            scriptParam.keySet().stream().forEach(key -> jsEngine.put(key, scriptParam.get(key)));
          returnObject = jsEngine.eval(scriptText);
          break;
        case SyntaxConstants.SYNTAX_STYLE_GROOVY:
          GroovyShell groovyShell  = new GroovyShell(ScriptUtil.class.getClassLoader());
          if(scriptParam != null)
            scriptParam.keySet().stream().forEach(key -> groovyShell.setVariable(key, scriptParam.get(key)));
          returnObject = groovyShell.evaluate(scriptText);
          break;
      }
    }
    return returnObject;
  }
  
  public static Object invokeMethodFromScript(String scriptText, String scriptLanguage, Map<String, Object> scriptParam, String methodName) throws javax.script.ScriptException, NoSuchMethodException {
    return invokeMethodFromScript(scriptText, scriptLanguage, scriptParam, methodName, new Object[0]);
  }
  
  public static Object invokeMethodFromScript(String scriptText, String scriptLanguage, String methodName) throws javax.script.ScriptException, NoSuchMethodException {
    return invokeMethodFromScript(scriptText, scriptLanguage, new HashMap<>(), methodName, new Object[0]);
  }
  
  public static Object invokeMethodFromScript(String scriptText, String scriptLanguage, String methodName, Object... methodParams) throws javax.script.ScriptException, NoSuchMethodException {
    return invokeMethodFromScript(scriptText, scriptLanguage, new HashMap<>(), methodName, methodParams);
  }
  
  public static Object invokeMethodFromScript(String scriptText, String scriptLanguage, Map<String, Object> scriptParam, String methodName, Object... methodParams) throws javax.script.ScriptException, NoSuchMethodException {
    Object returnObject = null;
    if(scriptText != null && !scriptText.equals("")) {
      switch(scriptLanguage) {
        case SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT:
          if(scriptParam != null)
            scriptParam.keySet().stream().forEach(key -> jsEngine.put(key, scriptParam.get(key)));
          jsEngine.eval(scriptText);
          returnObject = jsEngine.invokeFunction(methodName, methodParams);
          break;
        case SyntaxConstants.SYNTAX_STYLE_GROOVY:
          GroovyShell groovyShell  = new GroovyShell(ScriptUtil.class.getClassLoader());
          if(scriptParam != null)
            scriptParam.keySet().stream().forEach(key -> groovyShell.setVariable(key, scriptParam.get(key)));
          returnObject = groovyShell.parse(scriptText).invokeMethod(methodName, methodParams);
          break;
      }
    }
    return returnObject;
  }
}
