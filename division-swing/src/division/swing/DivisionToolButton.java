package division.swing;

import java.awt.Dimension;
import javax.swing.ImageIcon;

/**
 *
 * @author Платонов Р.А.
 */

public class DivisionToolButton extends DivisionButton {

  public DivisionToolButton() {
    super();
  }

  public DivisionToolButton(String toolTipText) {
    this(null,"...",toolTipText);
  }
  
  public DivisionToolButton(String text, String toolTipText) {
    this(null,text,toolTipText);
  }
  
  public DivisionToolButton(ImageIcon icon, String toolTipText) {
    this(icon,"",toolTipText);
  }
  
  public DivisionToolButton(ImageIcon icon) {
    this(icon,"","");
  }



  public DivisionToolButton(ImageIcon icon, String text, String toolTipText) {
    super(text);
    setIcon(icon);
    setToolTipText(toolTipText);
    if(text.equals("...")) {
      setPreferredSize(new Dimension(18,18));
      setMinimumSize(new Dimension(18,18));
      setMaximumSize(new Dimension(18,18));
    }
  }
}