package division.swing.plaf;

/**
 *
 * @author russo
 */
import java.text.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;
import division.swing.bum_Calendar;
import division.swing.DivisionComboBox;

public class BasicJDateTimePickerPopup extends BasicComboPopup
{
  protected bum_Calendar popupEditor;

  public BasicJDateTimePickerPopup(DivisionComboBox comboBox, bum_Calendar popupEditor)
  {
    super(comboBox);
    this.popupEditor = popupEditor;
    setFocusEnabled(popupEditor, false);
    add(popupEditor);
  }

  protected void setFocusEnabled(Component component, boolean flag)
  {

    if (component == null)return;
    if (component instanceof JComponent)
    {
      JComponent jcomponent = (JComponent)component;
      if(flag != jcomponent.isRequestFocusEnabled())
      jcomponent.setRequestFocusEnabled(flag);
    }
    if (component instanceof Container)
    {
      Component components[] = ((Container)component).getComponents();
      for (int i = 0; i < components.length; i++)
      {
        setFocusEnabled(components[i], flag);
        //components[i].setEnabled(true);
      }
    }
  }

  protected void configurePopup()
  {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorderPainted(true);
    //setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    setOpaque(false);
    setDoubleBuffered(true);
    setRequestFocusEnabled(false);
  }

  public void show()
  {
    if (popupEditor == null)return;
    syncPopupDataWithPickerData();
    Dimension popupSize = popupEditor.getPreferredSize();
    Rectangle popupBounds = computePopupBounds(comboBox.getBounds().width-popupSize.width, comboBox.getBounds().height,comboBox.getBounds().width, popupSize.height);
    setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());
    show(comboBox, popupBounds.x, popupBounds.y);
  }

  void syncPopupDataWithPickerData()
  {
    /*Locale l = comboBox.getLocale();
    if (!popupEditor.getLocale().equals(l))
      popupEditor.setLocale(l);
    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.applyPattern(((DNCComboBox)comboBox).getDisplayFormat());
    if (!sdf.format(popupEditor.getDate()).equals(comboBox.getSelectedItem()))
      comboBox.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));*/
  }
}
