package bum.editors.util;

import division.swing.DivisionScrollPane;
import division.swing.frame.RoundRectangleFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class AltMarker extends RoundRectangleFrame {
  private JTextPane    markerBox = new JTextPane();
  private DivisionScrollPane scroll    = new DivisionScrollPane(markerBox);

  private JPanel    buttonsPanel = new JPanel(new GridLayout(3, 3));
  private Color[]   colors = new Color[]{Color.WHITE,Color.BLUE,Color.PINK,Color.RED,Color.GRAY,
    Color.GREEN,Color.YELLOW,Color.MAGENTA,Color.ORANGE};
  private JButton[] buttons = new JButton[colors.length];

  private JButton addMarker = new JButton(">>");

  public AltMarker(final Class clazz,final Integer[] ids) throws HeadlessException {
    super(false);
    setSize(new Dimension(400,90));
    //setOpacity(0.8f);

    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);
    add(buttonsPanel, BorderLayout.WEST);
    add(addMarker, BorderLayout.EAST);
    markerBox.setFont(new Font("Verdana", Font.BOLD, 12));
    markerBox.setEditable(true);

    for(int i=0;i<buttons.length;i++) {
      buttons[i] = new JButton();
      buttons[i].setBackground(colors[i]);
      buttons[i].setMinimumSize(new Dimension(30, 30));
      buttons[i].setMaximumSize(new Dimension(30, 30));
      buttons[i].setPreferredSize(new Dimension(30, 30));
      final int index = i;
      buttons[i].addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          markerBox.setBackground(colors[index]);
        }
      });
      buttonsPanel.add(buttons[i]);
    }
    
    addMarker.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //Marker.getInstance().addMarker(markerBox.getText(), markerBox.getBackground(), clazz, ids);
        //setVisible(false);
        //Marker.getInstance().setVisible(true);
      }
    });
  }
}