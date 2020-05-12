package bum.editors;

import bum.editors.util.ObjectLoader;
import bum.interfaces.JSModul;
import bum.interfaces.JScript;
import division.swing.DivisionComboBox;
import division.swing.DivisionToolButton;
import division.swing.ScriptPanel;
import division.swing.guimessanger.Messanger;
import division.util.FileLoader;
import groovy.lang.GroovyShell;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import javax.script.ScriptEngineManager;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class JSModulEditor extends MainObjectEditor {
  private final ScriptPanel        script     = new ScriptPanel(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
  private final JTextField         nameText   = new JTextField();
  private final DivisionToolButton save       = new DivisionToolButton(FileLoader.getIcon("Save16.gif"));
  private final JButton            play       = new DivisionToolButton(FileLoader.getIcon("Play16.gif"));

  //если JScript
  private final JLabel                  classesName = new JLabel("Объект");
  private final DivisionComboBox        classes     = new DivisionComboBox();
  private final TreeMap<String, String> cl          = new TreeMap<>();
  
  private static NashornScriptEngine jsEngine     = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("JavaScript");
  private static GroovyShell         groovyShell  = new GroovyShell();

  public JSModulEditor() {
    getRootPanel().add(new JLabel("Наименование"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(nameText,                   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(classesName,                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(classes,                    new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    getRootPanel().add(script,                     new GridBagConstraints(0, 1, 4, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

    script.addToolComponent(save,0);
    script.addToolComponent(play, 1);

    play.addActionListener((ActionEvent e) -> {
      try {
        runScript(null, script.getText(), script.getContentType(), null);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
      /*try {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.put("engine", engine);
        for(MappingObject js:ObjectLoader.getObjects(JSModul.class))
          if(getEditorObject() instanceof JScript || js.getId().intValue() != ((JSModul)getEditorObject()).getId().intValue())
            engine.eval(((JSModul)js).getScript());
        engine.eval(script.getText());
      }catch(RemoteException | ScriptException ex) {
        Messanger.showErrorMessage(ex);
      }*/
    });

    save.addActionListener((ActionEvent e) -> {
      saveAction();
    });
  }
  
  public static Object runScript(String scriptName, String scriptText, String scriptLanguage, Map<String, Object> scriptParam) throws Exception {
    Object returnObject = scriptName;
    if(scriptText != null && !scriptText.equals("")) {
      try {
        switch(scriptLanguage) {
          case SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT:
            if(scriptParam != null)
              scriptParam.keySet().stream().forEach(key -> jsEngine.put(key, scriptParam.get(key)));
            returnObject = jsEngine.eval(scriptText);
            break;
          case SyntaxConstants.SYNTAX_STYLE_GROOVY:
            if(scriptParam != null)
              scriptParam.keySet().stream().forEach(key -> groovyShell.setVariable(key, scriptParam.get(key)));
            returnObject = groovyShell.evaluate(scriptText);
            break;
          default:
            throw new UnsupportedOperationException("Пока не поддерживается ("+scriptLanguage+")");
        }
      }catch(Exception ex) {
        returnObject = null;
        throw new Exception("Ошибка в скрипте: ", ex);
      }
    }
    return returnObject;
  }

  @Override
  public String commit() throws RemoteException {
    String msg = "";
    if(nameText.getText().equals(""))
      msg += "  -Необходимо назвать модуль\n";
    if(msg.equals("")) {
       JSModul modul = (JSModul)getEditorObject();
       modul.setName(nameText.getText());
       modul.setScript(script.getText());
       modul.setScriptLanguage(script.getContentType());

       if(modul instanceof JScript) {
         JScript scriptModul = (JScript)modul;
         if(classes.getSelectedIndex() < 0)
           scriptModul.setSourceClass(null);
         else scriptModul.setSourceClass(cl.get(classes.getSelectedItem().toString()));
         scriptModul.setClassDescription(classes.getSelectedItem().toString());
      }
    }
    return msg;
  }

  @Override
  public void clearData() {
    nameText.setText("");
    script.setText("");
  }

  @Override
  public void initData() {
    try {
      Rectangle vr = script.getScroll().getVisibleRect();
      int cp = script.getaScript().getCaretPosition();

      JSModul modul = (JSModul)getEditorObject();
      nameText.setText(modul.getName());
      script.setText(modul.getScript());
      script.setContentType(modul.getScriptLanguage());
      
      classes.clear();
      classes.setVisible(getEditorObject() instanceof JScript);
      classesName.setVisible(getEditorObject() instanceof JScript);
      if(getEditorObject() instanceof JScript) {
        try {
          cl.clear();
          String key;
          for(Class clazz:ObjectLoader.getServer().getClasses()) {
            key = ObjectLoader.getClientName(clazz);
            key = key==null?clazz.getSimpleName():key;
            int i=1;
            while(cl.containsKey(key)) {
              key += "("+i+")";
              i++;
            }
            cl.put(key, clazz.getName());
          }
          cl.keySet().stream().forEach((name) -> {
            classes.addItem(name);
          });
          classes.insertItemAt("Не важно",0);
        }catch(ClassNotFoundException | RemoteException ex) {
          Messanger.showErrorMessage(ex);
        }
        classes.setSelectedItem(((JScript)modul).getClassDescription());
      }
      script.getScroll().scrollRectToVisible(vr);
      script.getaScript().setCaretPosition(cp);
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
  }
}