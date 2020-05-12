package division.fx.image;

import division.fx.FXUtility;
import division.fx.util.MsgTrash;
import division.util.Utility;
import java.io.ByteArrayInputStream;
import java.io.File;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

public class ImageBox extends BorderPane {
  private ImageView im = new ImageView();
  private ObjectProperty<byte[]> bytesImageProperty = new SimpleObjectProperty<>();
  
  public ImageBox() {
    FXUtility.initCss(this);
    setCenter(im);
    initEvents();
    im.setSmooth(true);
    
    im.imageProperty().bind(Bindings.createObjectBinding(() -> {
      return bytesImageProperty.getValue() == null || bytesImageProperty.getValue().length == 0 ? null : 
              new Image(new ByteArrayInputStream(bytesImageProperty.getValue()), 
                      getWidth() > 0 ? getWidth() : getPrefWidth() > 0 ? getPrefWidth() : getMinWidth(), 
                      getHeight() > 0 ? getHeight() : getPrefHeight() > 0 ? getPrefHeight() : getMinHeight(), 
                      true, true);
    }, bytesImageProperty, widthProperty(), heightProperty(), minWidthProperty(), minHeightProperty(), maxWidthProperty(), maxHeightProperty(), prefWidthProperty(), prefHeightProperty()));
  }
  
  public ObjectProperty<byte[]> bytesImageProperty() {
    return bytesImageProperty;
  }
  
  private void image()  {
    FileChooser fileChooser = new FileChooser();
    if(System.getProperty("setLabelIcon") != null)
      fileChooser.setInitialDirectory(new File(System.getProperty("setLabelIcon")));
    fileChooser.setTitle("Выберите файл изображения");
    fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.jpg", "*.jpeg", "*.gif", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("JPG",    "*.jpg"),
                new FileChooser.ExtensionFilter("JPEG",   "*.jpeg"),
                new FileChooser.ExtensionFilter("GIF",    "*.gif"),
                new FileChooser.ExtensionFilter("PNG",    "*.png"),
                new FileChooser.ExtensionFilter("BMP",    "*.bmp"));
    File file = fileChooser.showOpenDialog(null);
    if(file != null) {
      try {
        bytesImageProperty.setValue(Utility.getBytesFromFile(file));
      }catch (Exception ex) {
        MsgTrash.out(ex);
      }
    }
  }

  private void initEvents() {
    addEventFilter(MouseEvent.ANY, e -> {
      if(e.getEventType() == MouseEvent.MOUSE_CLICKED) {
        if(im.getImage() != null) {
          MenuItem edit = new MenuItem("Изменить");
          MenuItem del  = new MenuItem("Удалить");
          ContextMenu pop = new ContextMenu(edit, del);

          edit.setOnAction(event -> image());
          del.setOnAction(event -> bytesImageProperty.setValue(null));
          pop.show(this, localToScreen(e.getX(), e.getY()).getX(), localToScreen(e.getX(), e.getY()).getY());
        }else image();
      }
    });
  }
}