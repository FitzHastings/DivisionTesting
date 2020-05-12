package division.swing;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;

/**
 * Этот класс расширение класса JTextField. Предназначен для создания TextField 
 * с ограничением по вводу символов. Например, создание текстового поля с 
 * ограничением ввода только символов '1', '2' и 'a':
 * <CODE>
 *      UserTextField textField = new UserTextField(UserTextField.Type.ALLOW);
 *      // установка символов
 *      textField.setAllowsSymbols("12a");
 * </CODE>
 * 
 * Пример текстового поля с ограничением ввода только целых и дробных чисел:
 * <CODE>
 *      UserTextField textField = new UserTextField(UserTextField.Type.FLOAT);
 *      // если есть необходимость установка символа разделения ',' дробной и целой части
 *      textField.setFloatSeparator(',');
 * </CODE>
 * @author Yuri Didenko
 * @version 1.0
 */
public class DivisionTextField extends JTextField implements FocusListener {
  public enum Type {
    /**
     * Поле для ввода только целочисленное чисел
     */
    INTEGER,
    /**
     * Поле для ввода только целочисленных и дробных чисел
     */
    FLOAT,
    /**
     * Поле для ввода любых символов
     */
    ALL,
    /**
     * Поле для ввода только символов заданных методом setAllowsSymbols()
     */
    ALLOW, 
    /**
     * Поле для ввода любых символов, кроме символов заданных методом setAllowsSymbols()
     */
    UNALLOW}
  
/**
 * Тип вводимых символов
 */  

  private Type type;

/**
 * Строка разрешенных/запрещенных вводимых символов
 */  

  private String allowsSymbols;
  
/**
 * Разделитель между целой и дробной частью для типа FLOAT. По умолчанию установлен '.'
 */  

  private char separator = '.';
  
  //private Double maxValue;
  //private Double minValue;
  
/**
 * Максимальное количество вводимых символов в поле. По умолчанию, количество не ограничено и установлен -1
 */  
  
  private int maxLength = -1;
  
  private int maxLengthAfterPoint = -1;
  
  private String emptyText = null;
  private Color emptyColor        = Color.LIGHT_GRAY;
  private Color defaultForeground = getForeground();
  
/** Creates a new instance of UserTextField */
  
  public DivisionTextField(String text, String emptyText, Type type, int maxLength) {
     super(text);
     setEmptyText(emptyText);
     this.type = type;
     this.maxLength = maxLength;
     addFocusListener(this);
     init();
  }
  
  public DivisionTextField(String text, String emptyText) {
     this(text, emptyText, Type.ALL, -1);
  }
  
  public DivisionTextField(String emptyText) {
     this("", emptyText, Type.ALL, -1);
  }
  public DivisionTextField(Type type, String emptyText) {
     this("", emptyText, type, -1);
  }

/**
 * Создает новый UserTextField с текстом
 * @param text текст в поле
 * @param type тип создаваемого поля
 */

  public DivisionTextField(String text, Type type) {
     this(text, null, type, -1);
  }
  
/**
 * Создает новый UserTextField
 * @param type тип создаваемого поля
 */  
  
  public DivisionTextField(Type type) {
     this(null, type);
  }
  
  public DivisionTextField(Type type, int maxLength) {
     this(null, null, type, maxLength);
  }

  public int getMaxLengthAfterPoint() {
    return maxLengthAfterPoint;
  }

  public void setMaxLengthAfterPoint(int maxLengthAfterPoint) {
    this.maxLengthAfterPoint = maxLengthAfterPoint;
  }
  
  public void setType(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public Color getEmptyColor() {
    return emptyColor;
  }

  public void setEmptyColor(Color emptyColor) {
    this.emptyColor = emptyColor;
  }

  public String getEmptyText() {
    return emptyText;
  }

  public void setEmptyText(String emptyText) {
    this.emptyText = emptyText;
    focusLost(null);
  }

  @Override
  public String getText() {
    if(emptyText != null && getForeground().equals(emptyColor))
      return "";
    return super.getText();
  }

  @Override
  public void setText(String t) {
    setForeground(defaultForeground);
    super.setText(t);
    if(isFocusOwner())
      focusGained(null);
    else focusLost(null);
  }
  
  @Override
  public void focusGained(FocusEvent e) {
    if(emptyText != null && super.getText().equals(emptyText)) {
      setForeground(defaultForeground);
      super.setText("");
    }
  }

  @Override
  public void focusLost(FocusEvent e) {
    if(emptyText != null && super.getText().equals("")) {
      defaultForeground = getForeground();
      setForeground(emptyColor);
      super.setText(emptyText);
    }
  }

  /*public Double getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(Double maxValue) {
    this.maxValue = maxValue;
  }

  public Double getMinValue() {
    return minValue;
  }

  public void setMinValue(Double minValue) {
    this.minValue = minValue;
  }*/
  
/**
 * Метод инициализации этого элемента
 */
  
  private void init () {
    this.addKeyListener( new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        char typedChar = e.getKeyChar();
        if(getText().length() >= maxLength && maxLength > 0) e.consume();
        
        switch(type) {
          case INTEGER:
            if(!((getText().length() == 0 || getText().equals(getSelectedText())) && typedChar == '-') && !Character.isDigit(typedChar))
              e.consume();
            break;
          case FLOAT:
            if(!((getText().length() == 0 || getText().equals(getSelectedText())) && typedChar == '-') &&
              (!Character.isDigit(typedChar) &&
              ((typedChar != separator) ||
              (typedChar == separator && getText().indexOf(separator) > 0) ||
              (typedChar == separator && getText().equals("-")) ||
              (typedChar == separator && getText().length() == 0))) ||
                    (getMaxLengthAfterPoint() > 0 && getText().indexOf(separator) > 0 && getText().length() - getText().indexOf(separator) > getMaxLengthAfterPoint()))
                e.consume();
            break;
          case ALLOW: 
            if (allowsSymbols != null)
              if (allowsSymbols.indexOf(typedChar) < 0)
                e.consume();
            break;
          case UNALLOW:
            if (allowsSymbols != null)
              if (allowsSymbols.indexOf(typedChar) >= 0)
                e.consume();
            break;
          case ALL: 
            break;
          default:break;
        }
      }
    });
  }
  
  /**
   * Устанавливает строку разрешенных/запрещенных для ввода символов
   * @param allowsSymbols строка символов
   */
  
  public void setAllowsSymbols(String allowsSymbols){
    this.allowsSymbols = allowsSymbols;
  }
  
  /**
   * Устанавливает разделитель целой и дробной части для типа FLOAT
   * @param separator разделитель
   */

  public void setFloatSeparator(char separator) {
    this.separator = separator;
  }
}