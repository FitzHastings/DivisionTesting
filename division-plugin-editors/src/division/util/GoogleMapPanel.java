package division.util;

import bum.editors.EditorGui;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;

public class GoogleMapPanel extends JPanel {
  private static JSlider slider = new JSlider(SwingConstants.VERTICAL, 1, 20, 17);
  private Image background = null;

  public GoogleMapPanel() {
    super(new BorderLayout());
    slider.setOpaque(false);
    add(slider, BorderLayout.WEST);
    setBorder(BorderFactory.createLineBorder(Color.GRAY));
  }
  
  @Override
  public void paint(Graphics g) {
    if(background != null)
      g.drawImage(background, 0, 0, null);
    super.paintChildren(g);
    super.paintBorder(g);
  }
  
  public void paintMap(JSONObject[] divisionFormatAddresses) throws Exception {
    if(getWidth() > 0 && getHeight() > 0) {
      EditorGui.setCursor(this, new Cursor(Cursor.WAIT_CURSOR));
      try {
        String[] markers = new String[0];
        for(JSONObject json:divisionFormatAddresses) {
          if(json.containsKey("lat") && json.containsKey("lng"))
            markers = (String[]) ArrayUtils.add(divisionFormatAddresses, json.getString("lat")+","+json.getString("lng"));
          else if(json.containsKey("title"))
            markers = (String[]) ArrayUtils.add(divisionFormatAddresses, json.getString("title"));
        }
        if(divisionFormatAddresses.length == 0)
          background = AddressUtil.getMap(getWidth(), getHeight(), slider.getValue(), "Россия", markers);
        else background = AddressUtil.getMap(getWidth(), getHeight(), slider.getValue(), markers);
        repaint();
      }catch(Exception ex) {
        throw new Exception(ex);
      }finally {
        EditorGui.setCursor(this, new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
}