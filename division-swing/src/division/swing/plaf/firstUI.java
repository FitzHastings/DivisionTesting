package division.swing.plaf;

/**
 *
 * @author russo
 */
import division.swing.DivisionComboBox;
import division.swing.bum_Calendar;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;

public class firstUI extends MetalComboBoxUI {
  protected bum_Calendar popupEditor;
  private BasicJDateTimePickerPopup pop;

  public firstUI() {
  }

  public static ComponentUI createUI(JComponent c) {
    return new firstUI();
  }

  @Override
  protected ComboPopup createPopup() {
    popupEditor = new bum_Calendar();
    pop = new BasicJDateTimePickerPopup((DivisionComboBox)comboBox, popupEditor);
    pop.getAccessibleContext().setAccessibleParent(comboBox);
    return pop;
  }

  public BasicJDateTimePickerPopup getPop() {
    return pop;
  }

  public bum_Calendar getPopupEditor() {
    return popupEditor;
  }
}
