package division.swing;

import division.swing.guimessanger.Messanger;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.lang.reflect.Field;
import java.util.TreeMap;
import javax.swing.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

public class ScriptPanel extends JPanel {
  private JToolBar     tool    = new JToolBar();
  private TreeMap<Integer,Component> toolButtons = new TreeMap<>();
  
  private DivisionComboBox types    = new DivisionComboBox();
  private RSyntaxTextArea  textArea = new RSyntaxTextArea();
  private RTextScrollPane  sp       = new RTextScrollPane(textArea);
  
  private JPanel            findPanel   = new JPanel(new GridBagLayout());
  private DivisionTextField searchField = new DivisionTextField("поиск...");
  private DivisionButton    nextButton  = new DivisionButton(" Вперёд ");
  private DivisionButton    prevButton  = new DivisionButton("  Назад  ");
  private JCheckBox         regexCB     = new JCheckBox("Рег. выраж.");
  private JCheckBox         matchCaseCB = new JCheckBox("Уч. рег.");

  public ScriptPanel() {
    this(SyntaxConstants.SYNTAX_STYLE_NONE, new Font("Monospace", Font.PLAIN, 10));
  }

  public ScriptPanel(String syntaxConstants) {
    this(syntaxConstants, new Font("Monospace", Font.PLAIN, 10));
  }

  public ScriptPanel(Font font) {
    this(SyntaxConstants.SYNTAX_STYLE_NONE, font);
  }

  public ScriptPanel(String syntaxConstants, Font scriptFont) {
    super(new GridBagLayout());
    initComponents();
    initEvents();
    
    try {
      for(Field field:SyntaxConstants.class.getDeclaredFields())
        types.addItem(field.get(null));
    }catch(Exception ex) {
      Messanger.showErrorMessage(ex);
    }
    setContentType(syntaxConstants);
    setFont(scriptFont);
  }

  public void addToolComponent(Component c) {
    addToolComponent(c,tool.getComponentCount());
  }

  public void addToolComponent(Component c, int index) {
    toolButtons.put(index, c);
    tool.add(c,index);
  }

  public JTextArea getaScript() {
    return textArea;
  }

  public JScrollPane getScroll() {
    return sp;
  }

  private void initComponents() {
    SyntaxScheme ss = textArea.getSyntaxScheme();
    ss = (SyntaxScheme) ss.clone();
    for (int i = 0; i < ss.getStyleCount(); i++) {
      if (ss.getStyle(i) != null) {
          ss.getStyle(i).font = new Font("Verdana", Font.PLAIN, 10);
      }
    }
    textArea.setSyntaxScheme(ss);
    textArea.setFont(new Font("Verdana", Font.PLAIN, 10));
    
    
    searchField.setMinimumSize(new Dimension(200, 20));
    searchField.setPreferredSize(new Dimension(200, 20));
    searchField.setMaximumSize(new Dimension(200, 20));
    
    findPanel.add(searchField, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 2, 2, 2), 0, 0));
    findPanel.add(nextButton,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 2, 1, 2), 0, 0));
    findPanel.add(prevButton,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 2, 1, 2), 0, 0));
    findPanel.add(regexCB,     new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 2, 0, 2), 0, 0));
    findPanel.add(matchCaseCB, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 2, 0, 2), 0, 0));
    
    nextButton.setActionCommand("FindNext");
    prevButton.setActionCommand("FindPrev");
    
    types.setMinimumSize(new Dimension(150, 20));
    types.setPreferredSize(new Dimension(150, 20));

    textArea.setCodeFoldingEnabled(true);
    textArea.setAntiAliasingEnabled(true);
    sp.setFoldIndicatorEnabled(true);
    
    tool.setBorderPainted(true);
    tool.setFloatable(false);
    
    tool.addSeparator();
    tool.add(findPanel);
    
    add(tool,      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(types,     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(sp,        new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
  }

  private void initEvents() {
    //nextButton.addActionListener(this::find);
    //prevButton.addActionListener(this::find);
    
    searchField.addActionListener((ActionEvent e) -> {
      nextButton.doClick(0);
    });
    
    types.addItemListener((ItemEvent e) -> {
      try {
        if(e.getStateChange() == ItemEvent.SELECTED) {
          textArea.setSyntaxEditingStyle((String) types.getSelectedItem());
        }
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    });
  }
  
  /*private void find(ActionEvent e) {
    String command = e.getActionCommand();
    boolean forward = "FindNext".equals(command);

    SearchContext context = new SearchContext();
    String text = searchField.getText();
    if (text.length() == 0) {
        return;
    }
    context.setSearchFor(text);
    context.setMatchCase(matchCaseCB.isSelected());
    context.setRegularExpression(regexCB.isSelected());
    context.setSearchForward(forward);
    context.setWholeWord(false);
    
    boolean found = SearchEngine.find(textArea, context).wasFound();
    if (!found) {
        JOptionPane.showMessageDialog(this, "Text not found");
    }
  }*/

  public void setContentType(String syntaxConstants) {
    types.setSelectedItem(syntaxConstants);
  }
  
  public String getContentType() {
    return String.valueOf(types.getSelectedItem());
  }

  public Font getScriptFont() {
    return textArea.getFont();
  }

  public void setScriptFont(Font scriptFont) {
    textArea.setFont(scriptFont);
  }

  public void setText(String text) {
    textArea.setText(text);
    textArea.setCaretPosition(0);
  }

  public String getText() {
    return textArea.getText();
  }
}