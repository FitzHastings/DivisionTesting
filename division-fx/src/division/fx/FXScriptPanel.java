package division.fx;

import division.fx.util.MsgTrash;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class FXScriptPanel extends VBox {
  private final TextField serachField = new TextField();
  private final FXToolButton next = new FXToolButton("Следующее совпадение",  "next-search");
  private final FXToolButton prev = new FXToolButton("Предыдущее совпадение", "prev-search");
  private final CheckBox regex = new CheckBox("Рег. выраж.");
  private final CheckBox ignoreCase = new CheckBox("Игнорировать регистр");
  
  //private final Button show = new Button("применить");
  private final ToolBar tools = new ToolBar(serachField, next, prev, regex, ignoreCase, new Separator()/*, show*/);
  
  
  private final ChoiceBox<String> types = new ChoiceBox<>();
  
  private RSyntaxTextArea textArea   = new RSyntaxTextArea();
  private RTextScrollPane scrollPane = new RTextScrollPane(textArea);
  private final SwingNode swingNode  = new SwingNode();
  
  private ObjectProperty<Font>   fontProperty = new SimpleObjectProperty<>(new Font("Monospace", Font.PLAIN, 10));
  private StringProperty textProperty = new SimpleStringProperty();
  private boolean textPropertyChange = true;
  
  public FXScriptPanel() {
    this(SyntaxConstants.SYNTAX_STYLE_NONE, new Font("Monospace", Font.PLAIN, 10));
  }

  public FXScriptPanel(String syntaxConstants) {
    this(syntaxConstants, new Font("Monospace", Font.PLAIN, 10));
  }

  public FXScriptPanel(Font font) {
    this(SyntaxConstants.SYNTAX_STYLE_NONE, font);
  }
  
  public FXScriptPanel(String syntaxConstants, Font scriptFont) {
    super(5);
    swingNode.setContent(scrollPane);
    getChildren().addAll(tools, swingNode);
    VBox.setVgrow(swingNode, Priority.ALWAYS);
    
    serachField.setPromptText("Поиск...");
    try {
      for(Field field:SyntaxConstants.class.getDeclaredFields())
        types.getItems().add(String.valueOf(field.get(null)));
    }catch(Exception ex) {
      MsgTrash.out(ex);
    }
    textArea.setCodeFoldingEnabled(true);
    textArea.setAntiAliasingEnabled(true);
    scrollPane.setFoldIndicatorEnabled(true);
    
    contentTypeProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> SwingUtilities.invokeLater(() -> textArea.setSyntaxEditingStyle(newValue)));
    fontProperty().addListener((ObservableValue<? extends Font> observable, Font oldValue, Font newValue) -> SwingUtilities.invokeLater(() -> textArea.setFont(newValue)));
    
    textArea.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        textArea.grabFocus();
      }
    });
    
    //show.setOnAction(e -> textProperty.setValue(textArea.getText()));
    
    textArea.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        textProperty.setValue(textArea.getText());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        textProperty.setValue(textArea.getText());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        textProperty.setValue(textArea.getText());
      }
    });
    
    swingNode.addEventHandler(EventType.ROOT, e -> ((Node)e.getSource()).requestFocus());
    
    contentTypeProperty().setValue(syntaxConstants);
    fontProperty().setValue(scriptFont);
  }
  
  public ObjectProperty<String> contentTypeProperty() {
    return types.valueProperty();
  }
  
  public ObjectProperty<Font> fontProperty() {
    return fontProperty;
  }

  public ReadOnlyStringProperty textProperty() {
    return textProperty;
  }
  
  public void setText(String text) {
    int lastCaretPosition = textArea.getCaretPosition();
    textArea.setText(text);
    textArea.setCaretPosition(lastCaretPosition);
  }

  public ToolBar getTools() {
    return tools;
  }
}