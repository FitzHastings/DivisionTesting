package division.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.sql.Timestamp;
import java.util.Date;
import javax.swing.ComboBoxEditor;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.MaskFormatter;

/**
 *
 * @author Платонов Р.А.
 */

public class DivisionCalendarComboBox extends DivisionComboBox implements CalendarChangeListener {
  private MaskFormatter formatter;
  protected bum_Calendar popupEditor;
  private JFormattedTextField format;
  
  public DivisionCalendarComboBox() {
    this(null);
  }

  public DivisionCalendarComboBox(Date date) {
    if(date == null)
      date = new Date();
    try {
      formatter = new MaskFormatter("##.##.####");
      formatter.setPlaceholderCharacter('0');
      format = new JFormattedTextField();
    }catch(Exception e){}
    popupEditor.clear();
    setDateInCalendar(date);
    setEditable(true);
    setFont(format.getFont());
    setEditor(new ComboEditor());
    popupEditor.addToListenerAllwaysAction(this);
    format.setValue(popupEditor.getDate());
    format.setEditable(false);
    setBackgroundColor(this);
    
    setMinimumSize(new Dimension(100, 20));
    setPreferredSize(new Dimension(100, 20));
    
    ((division.swing.plaf.firstUI)ui).getPop().addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        setDateInCalendar(getDate());
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
  }
  
  public void addCalendarChangeListener(CalendarChangeListener listener) {
    popupEditor.addToListener(listener);
  }

  public void removeCalendarChangeListener(CalendarChangeListener listener) {
    popupEditor.delFromListener(listener);
  }
  
  private void setBackgroundColor(JComponent component) {
    component.setBackground(Color.WHITE);
    for(int i=0;i<component.getComponentCount();i++) {
      if(component.getComponent(i) instanceof JComponent && ((JComponent)component.getComponent(i)).getComponentCount() > 0)
          setBackgroundColor((JComponent)component.getComponent(i));
      else component.getComponent(i).setBackground(Color.WHITE);
    }
  }

  @Override
  public void setEnabled(boolean aFlag) {
    super.setEnabled(aFlag);
    System.out.println("aFlag = "+aFlag);
    popupEditor.setEnabled(aFlag);
    format.setEnabled(aFlag);
    format.setBackground(aFlag?Color.WHITE:Color.LIGHT_GRAY);
  }

  @Override
  public void updateUI() {
    setUI(division.swing.plaf.firstUI.createUI(this));
    popupEditor = ((division.swing.plaf.firstUI)ui).getPopupEditor();
  }

  public void setDateInCalendar(int dd,int mm,int yyyy, boolean activeAction) {
    popupEditor.clear();
    popupEditor.setDateInCalendar(dd,mm,yyyy,activeAction);
  }

  public void setDateInCalendar(Date date, boolean activeAction) {
    popupEditor.clear();
    popupEditor.setDateInCalendar(date,activeAction);
  }

  public void setDateInCalendar(int dd,int mm,int yyyy) {
    popupEditor.clear();
    popupEditor.setDateInCalendar(dd,mm,yyyy);
  }
  
  public void setDateInCalendar(java.sql.Date date) {
    popupEditor.clear();
    popupEditor.setDateInCalendar(date);
  }

  public void setDateInCalendar(Date date) {
    popupEditor.clear();
    popupEditor.setDateInCalendar(date);
  }
  
  public void setDateInCalendar(Timestamp timestamp) {
    setDateInCalendar(new Date(timestamp.getTime()));
  }

  @Override
  public void CalendarChangeDate(DistanceList dates) {
    fireItemStateChanged(new ItemEvent(this,0,this.getDate(),1));
    fireActionEvent();
    format.setValue(dates.get(0).getStartDate());
  }

  public Date getDate() {
    return getDate(false);
  }
  
  public Date getDate(boolean ignoreEnabled) {
    return popupEditor.getDate(ignoreEnabled);
  }

  @Override
  public void CalendarSelectedDate(DistanceList dates) {
  }

  class ComboEditor implements ComboBoxEditor {
    @Override
    public Component getEditorComponent() {
      return format;
    }

    @Override
    public void setItem(Object anObject) {
    }

    @Override
    public Object getItem() {
      return format.getValue();
    }

    @Override
    public void selectAll() {
    }
    
    @Override
    public void addActionListener(ActionListener l) {
    }
    
    @Override
    public void removeActionListener(ActionListener l) {
    }
  }
}
