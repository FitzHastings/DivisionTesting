package division.editors.products;

import division.swing.CircularSpinnerModel;
import division.util.Utility;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.time.Period;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class DurationReccurencePanel extends JPanel {
  JSpinner count  = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
  JSpinner type   = new JSpinner(new CircularSpinnerModel(new Object[0]));
  JButton  button = new JButton(">>>");
  
  public DurationReccurencePanel() {
    this(true);
  }

  public DurationReccurencePanel(boolean enterButtonVisible) {
    super(new GridBagLayout());
    add(count,  new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 3, 2), 0, 0));
    add(type,   new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
    if(enterButtonVisible)
      add(button, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
    count.addChangeListener(e -> setTypes());
  }

  public void setButtonActionListener(ActionListener listener) {
    for(ActionListener l:button.getActionListeners())
      button.removeActionListener(l);
    button.addActionListener(listener);
  }

  private void setTypes() {
    int index = ((CircularSpinnerModel)type.getModel()).indexOf(type.getValue());
    Object[] typeItems = new Object[]{"дней","месяцев","лет"};
    int value = (Integer)count.getValue();
    if(value == 0) {
      //typeItems = new Object[0];
      index = -1;
    }else if(value != 11 && value%10 == 1)
      typeItems = new Object[]{"день","месяц","год"};
    else if((value > 21 || value < 10) && value%10 < 5 && value%10 > 0)
      typeItems = new Object[]{"дня","месяца","года"};
    type.setModel(new CircularSpinnerModel(typeItems));
    if(index >= 0)
      ((CircularSpinnerModel)type.getModel()).setIndexValue(index);
  }

  public void setString(String string) {
    if(string != null) {
      Period p = Utility.convert(string);
      if(p.getDays() > 0 || p.getMonths() > 0 || p.getYears() > 0) {
        count.setValue(p.getDays() > 0 ? p.getDays() : p.getMonths() > 0 ? p.getMonths() : p.getYears() > 0 ? p.getYears() : 0);
        //setTypes();
        String[] items = Utility.getPeriodTypes((Integer)count.getValue());
        type.setModel(new CircularSpinnerModel(items));
        type.setValue(p.getDays() > 0 ? items[0] : p.getMonths() > 0 ? items[1] : p.getYears() > 0 ? items[2] : null);
      }
    }
  }

  public String getString() {
    return (int)count.getValue() > 0 ? count.getValue()+" "+type.getValue() : null;
  }
}