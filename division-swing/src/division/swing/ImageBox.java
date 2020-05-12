package division.swing;

import division.swing.actions.LinkBorderActionEvent;
import division.swing.guimessanger.Messanger;
import division.util.Utility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

public class ImageBox extends LinkLabel {
  private byte[] image;
  
  public ImageBox() {
    this(80, 80);
  }
  
  public ImageBox(int width, int height) {
    this(width, height, null);
  }
  
  public ImageBox(int width, int height, String description) {
    setMinimumSize(new Dimension(width, height));
    setPreferredSize(new Dimension(width, height));
    setMaximumSize(new Dimension(width, height));
    setBorder(BorderFactory.createLineBorder(Color.GRAY));
    if(description != null && !description.equals(""))
      setToolTipText(description);
    initEvents();
  }
  
  private byte[] setLabelIcon()  {
    JFileChooser fileChooser = new JFileChooser();
    if(System.getProperty("setLabelIcon") != null)
      fileChooser.setCurrentDirectory(new File(System.getProperty("setLabelIcon")));
    fileChooser.setAccessory(new ImagePreview(fileChooser));
    fileChooser.setDialogTitle("Выберите файл изображения");
    fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        if(f.isDirectory())
          return true;
        String type = f.getName().substring(f.getName().lastIndexOf(".")+1).toLowerCase();
        return type.equals("jpg") || type.equals("jpeg") || type.equals("gif") || type.equals("png") || type.equals("bmp");
      }

      @Override
      public String getDescription() {
        return "Файл изображения (jpg,jpeg,gif,png,bmp)";
      }
    });

    fileChooser.showDialog(null, "выбрать");
    File file = fileChooser.getSelectedFile();
    if(file != null) {
      try {
        setIcon(new ImageIcon(Utility.setScale(ImageIO.read(file), getWidth(), getHeight())));
        return Utility.getBytesFromFile(file);
      }catch(Exception ex) {
        Messanger.showErrorMessage(ex);
      }
    }
    return new byte[0];
  }
  
  public void setImage(byte[] image) throws Exception {
    this.image = image;
    if(image != null && image.length > 0)
      setIcon(new ImageIcon(Utility.setScale(image, getPreferredSize().width, getPreferredSize().height)));
  }
  
  public void setImage(String fileName) throws Exception {
    setImage(Utility.getBytesFromFile(fileName));
  }
  
  public void setImage(File file) throws Exception {
    setImage(Utility.getBytesFromFile(file));
  }
  
  public byte[] getImage() {
    return image;
  }

  private void initEvents() {
    addActionListener(e -> {
      if(getIcon() != null) {
        JPopupMenu pop = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Изменить");
        JMenuItem del  = new JMenuItem("Удалить");
        pop.add(edit);
        pop.add(del);
        
        Point location = ((LinkBorderActionEvent)e).getMouseEvent().getPoint();
        pop.show(this, location.x, location.y);
        edit.addActionListener(t -> {
          byte[] data = setLabelIcon();
          if(data != null && data.length > 0)
            image = data;
        });
        del.addActionListener(t -> {
          image = new byte[0];
          setIcon(null);
        });
      }else image = setLabelIcon();
    });
  }
}