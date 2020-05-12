package division.util.yandex;

import division.swing.DivisionComboBox;
import division.util.yandex.YandexApi.MapType;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sf.json.JSONObject;

public class YandexMapPanel extends JPanel {
  private ExecutorService pool = Executors.newSingleThreadExecutor();
  private JSlider slider = new JSlider(SwingConstants.VERTICAL, 2, 18, 10);
  
  private DivisionComboBox mapType = new DivisionComboBox(new MapType[]{MapType.схема, MapType.спутник, MapType.гибрид});
  
  private Image background = null;
  private JSONObject[] divisionAddresses;
  
  private YandexErrorConnection yandexErrorConnection;

  public YandexMapPanel(YandexErrorConnection yandexErrorConnection) {
    super(new GridBagLayout());
    this.yandexErrorConnection = yandexErrorConnection;
    initComponents();
    initEvents();
  }
  
  @Override
  public void paint(Graphics g) {
    g.clearRect(0, 0, getWidth(), getHeight());
    if(background != null)
      g.drawImage(background, 0, 0, null);
    super.paintChildren(g);
    super.paintBorder(g);
  }
  
  public void setAddresses(final JSONObject[] divisionAddresses) {
    this.divisionAddresses = divisionAddresses;
    if(isVisible() && isShowing() && getWidth() > 0 && getHeight() > 0) {
      slider.setVisible(divisionAddresses != null && divisionAddresses.length==1);
      setCursor(YandexMapPanel.this, new Cursor(Cursor.WAIT_CURSOR));
      pool.submit(new Runnable() {
        @Override
        public void run() {
          try {
            background = YandexApi.getMap(
                    (YandexApi.MapType)mapType.getSelectedItem(), 
                    //YandexApi.MapType.valueOf(group.getSelection().getActionCommand()), 
                    getWidth(), getHeight(), 
                    slider.isVisible()?slider.getValue():(divisionAddresses.length==0?10:-1), 
                    null, 
                    divisionAddresses);
          }catch(Exception ex) {
            yandexErrorConnection.connectionError(ex);
          }finally {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                repaint();
                setCursor(YandexMapPanel.this, new Cursor(Cursor.DEFAULT_CURSOR));
              }
            });
          }
        }
      });
    }
  }
  
  private static void setCursor(JComponent component, Cursor cursor) {
    component.setCursor(cursor);
    for(Component comp:component.getComponents())
      if(comp instanceof JComponent)
        setCursor((JComponent)comp, cursor);
  }

  private void initComponents() {
    mapType.setBorder(BorderFactory.createEmptyBorder());
    
    setMinimumSize(new Dimension(YandexApi.minWidth, YandexApi.minHeight));
    setMaximumSize(new Dimension(YandexApi.maxWidth, YandexApi.maxHeight));
    setPreferredSize(new Dimension((int)((YandexApi.maxWidth-YandexApi.minWidth)/1.3), (int)((YandexApi.maxHeight-YandexApi.minHeight)/1.3)));
    
    slider.setMinimumSize(new Dimension(50, 100));
    slider.setMaximumSize(new Dimension(50, 100));
    slider.setOpaque(false);
    
    add(slider,  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(mapType, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    
    setBorder(BorderFactory.createLineBorder(Color.GRAY));
  }

  private void initEvents() {
    mapType.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED) {
          setAddresses(divisionAddresses);
        }
      }
    });
    
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        setAddresses(divisionAddresses);
      }
    });
    
    addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.getWheelRotation() > 0 && slider.getValue() < 20 || e.getWheelRotation() < 1 && slider.getValue() > 1)
          slider.setValue(slider.getValue()+e.getWheelRotation());
      }
    });
    
    slider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        setAddresses(divisionAddresses);
      }
    });
  }
}