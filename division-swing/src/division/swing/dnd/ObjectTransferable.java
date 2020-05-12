package division.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mapping.MappingObject;

public class ObjectTransferable implements Transferable {
  private DataFlavor[] flavors;
  private List objects;
  private List<Integer> objectsId;
  private Class<? extends MappingObject> interfaceClass;

  public ObjectTransferable(Object[] objects, Class interfaceClass) {
    try {
      this.interfaceClass = interfaceClass;
      String objectMimeType = DataFlavor.javaRemoteObjectMimeType + ";class="+interfaceClass.getName();
      String interfaceMimeType = DataFlavor.javaSerializedObjectMimeType + ";class="+interfaceClass.getName();

      flavors = new DataFlavor[]{
        new DataFlavor(objectMimeType),
        new DataFlavor(interfaceMimeType)
      };
      this.objects = new ArrayList(Arrays.asList(objects));
    }catch(ClassNotFoundException ex) {
      ex.printStackTrace();
    }
  }

  public ObjectTransferable(Integer[] objectsId, Class interfaceClass) {
    try {
      this.interfaceClass = interfaceClass;
      String objectMimeType = DataFlavor.javaRemoteObjectMimeType + ";class="+interfaceClass.getName();
      String interfaceMimeType = DataFlavor.javaSerializedObjectMimeType + ";class="+interfaceClass.getName();

      flavors = new DataFlavor[]{
        new DataFlavor(objectMimeType),
        new DataFlavor(interfaceMimeType)
      };
      this.objectsId = new ArrayList(Arrays.asList(objectsId));
    }catch(ClassNotFoundException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return flavors;
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    for(DataFlavor df:flavors)
      if(df.equals(flavor))
        return true;
    return false;
  }

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if(isDataFlavorSupported(flavor)) {
      if(flavors[0].equals(flavor))
        return objects == null?objectsId:objects;
      if(flavors[1].equals(flavor))
        return interfaceClass;
    }
    return null;
  }
}