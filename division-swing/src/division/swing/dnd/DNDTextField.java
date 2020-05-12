package division.swing.dnd;

import division.swing.DivisionTextField;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.swing.border.Border;
import mapping.MappingObject;

public class DNDTextField extends DivisionTextField implements DropTargetListener {
  private Hashtable<Class<? extends MappingObject>,Integer> supportsInterfaces = new Hashtable<>();
  private List<DNDListener> listeners = new ArrayList<>();
  private Color dndColor = Color.BLACK;
  private Border border = getBorder();
  //private Hashtable<Class<? extends MappingObject>,Union> filters = new Hashtable<>();
  
  public DNDTextField(Type type, int maxLength) {
    super(type, maxLength);
    DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null);
  }

  public DNDTextField(Type type) {
    super(type);
    DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null);
  }

  public DNDTextField(String text, Type type) {
    super(text, type);
    DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null);
  }
  
  public Color getDndColor() {
    return dndColor;
  }

  public void setDndColor(Color dndColor) {
    this.dndColor = dndColor;
  }
  
  public void addDNDListener(DNDListener listener) {
    if(!listeners.contains(listener))
      listeners.add(listener);
  }
  
  public void removeDNDListener(DNDListener listener) {
    listeners.remove(listener);
  }
  
  private void fireDropDNDListener(Point point, List<MappingObject> objects, Class<? extends MappingObject> interfaceClass) {
    for(int i=listeners.size()-1;i>=0;i--)
      listeners.get(i).drop(point, objects, interfaceClass);
  }
  
  private void fireDragOverDNDListener(Point point, List<MappingObject> objects, Class<? extends MappingObject> interfaceClass) {
    for(int i=listeners.size()-1;i>=0;i--)
      listeners.get(i).dragOver(point, objects, interfaceClass);
  }
  
  /*public void replaceFilter(Class<? extends MappingObject> interfaceClass, Union filter) {
    System.out.println("replaceFilter Field "+interfaceClass.getSimpleName());
    removeFilter(interfaceClass);
    addFilter(interfaceClass, filter);
  }
  
  public void addFilter(Class<? extends MappingObject> interfaceClass, Union filter) {
    if(!filters.containsKey(interfaceClass))
      filters.put(interfaceClass, filter);
  }
  
  public void removeFilter(Class<? extends MappingObject> interfaceClass) {
    filters.remove(interfaceClass);
  }*/
  
  public void addSupportInterface(Class<? extends MappingObject> interfaceClass,Integer actionType) {
    if(!supportsInterfaces.containsKey(interfaceClass))
      supportsInterfaces.put(interfaceClass, actionType);
  }
  
  public void removeSupportInterface(Class<? extends MappingObject> interfaceClass) {
    supportsInterfaces.remove(interfaceClass);
  }
  
  public void clearSupportsInterfaces() {
    supportsInterfaces.clear();
  }
  
  private Class getDragClass(DropTargetDragEvent dtde) {
    try {
      Transferable transferable = dtde.getTransferable();
      for(Class clazz:supportsInterfaces.keySet()) {
        String objectMimeType = DataFlavor.javaRemoteObjectMimeType + ";class="+clazz.getName();
        DataFlavor objectFlavor = new DataFlavor(objectMimeType);
        if(transferable.isDataFlavorSupported(objectFlavor))
          if(supportsInterfaces.get(clazz) != null)
            return clazz;
      }
    }catch(Exception ex){ex.printStackTrace();}
    return null;
  }
  
  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
    /*if(getDragClass(dtde) != null) {
      border = getBorder();
      setBorder(BorderFactory.createLineBorder(getDndColor()));
    }
    try {
      Transferable transferable = dtde.getTransferable();
      for(Class clazz:supportsInterfaces.keySet()) {
        String objectMimeType = DataFlavor.javaRemoteObjectMimeType + ";class="+clazz.getName();
        DataFlavor objectFlavor = new DataFlavor(objectMimeType);
        if(transferable.isDataFlavorSupported(objectFlavor)) {
          if(supportsInterfaces.get(clazz) != null) {
            String interfaceMimeType = DataFlavor.javaSerializedObjectMimeType + ";class="+clazz.getName();
            if(!this.filters.isEmpty()) {
             Union filter = this.filters.get((Class)transferable.getTransferData(new DataFlavor(interfaceMimeType)));
             if(filter != null) {
                for(Object object:(List)transferable.getTransferData(objectFlavor)) {
                  if(!filter.isSatisfy(object)) {
                    dtde.rejectDrag();
                    return;
                  }
                }
             }
            }
            dtde.acceptDrag(supportsInterfaces.get(clazz));
            return;
          }
        }
      }
      dtde.rejectDrag();
    }catch(ClassNotFoundException | UnsupportedFlavorException | IOException ex) {
      ex.printStackTrace();
    }*/
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
    /*try {
      Transferable transferable = dtde.getTransferable();
      for(Class clazz:supportsInterfaces.keySet()) {
        String objectMimeType = DataFlavor.javaRemoteObjectMimeType + ";class="+clazz.getName();
        DataFlavor objectFlavor = new DataFlavor(objectMimeType);
        if(transferable.isDataFlavorSupported(objectFlavor)) {
          if(supportsInterfaces.get(clazz) != null) {
            String interfaceMimeType = DataFlavor.javaSerializedObjectMimeType + ";class="+clazz.getName();
            fireDragOverDNDListener(dtde.getLocation(),
                    (List)transferable.getTransferData(objectFlavor),
                    (Class)transferable.getTransferData(new DataFlavor(interfaceMimeType)));
            dtde.acceptDrag(supportsInterfaces.get(clazz));
            return;
          }
        }
      }
      dtde.rejectDrag();
    }catch(Exception ex){ex.printStackTrace();}*/
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
    setBorder(border);
  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
    setBorder(border);
    Transferable transferable = dtde.getTransferable();
    try {
      fireDropDNDListener(
              dtde.getLocation(), 
              (List<MappingObject>)transferable.getTransferData(transferable.getTransferDataFlavors()[0]),
              (Class)transferable.getTransferData(transferable.getTransferDataFlavors()[1])
              );
    }catch(UnsupportedFlavorException | IOException ex) {
      ex.printStackTrace();
    }
  }
}