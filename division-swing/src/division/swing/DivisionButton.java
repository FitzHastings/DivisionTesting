package division.swing;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Расширяет JButton, устанавливая 
 * отступы от краёв кнопки до надписи равными нулю
 * @author Платонов Р.А.
 */
public class DivisionButton extends JButton {
  private TreeMap<String, ActionListener> actions = new TreeMap<>();
  
  /**
   * Создаёт кнопку без надписи
   */
  public DivisionButton() {
    this("");
  }
  
  /**
   * Создаёт кнопку с надписью
   * @param text надпись
   */
  public DivisionButton(String text) {
    super(text);
    setMargin(new Insets(0,0,0,0));
    addActionListener((ActionEvent e) -> {
      if(!actions.isEmpty()) {
        JPopupMenu pop = new JPopupMenu();
        for(String name:actions.keySet()) {
          JMenuItem item = new JMenuItem(name);
          item.addActionListener(actions.get(name));
          pop.add(item);
        }
        pop.show(DivisionButton.this, 0, DivisionButton.this.getHeight());
      }
    });
  }
  
  public void addAction(String name, ActionListener listener) {
    actions.put(name, listener);
  }
  
  public void removeAction(String name) {
    actions.remove(name);
  }
  
  public void clearActions() {
    actions.clear();
  }
}
