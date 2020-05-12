package division.swing;

import java.util.Arrays;
import javax.swing.AbstractSpinnerModel;

public class CircularSpinnerModel extends AbstractSpinnerModel {
  private Object[] data;
  private int curent = 0;

  public CircularSpinnerModel(Object[] data) {
    this.data = data;
  }

  @Override
  public Object getValue() {
    return data.length==0?null:data[curent];
  }
  
  public int indexOf(Object value) {
    return Arrays.binarySearch(data, value);
  }
  
  public void setIndexValue(int index) {
    setValue(data[index]);
  }

  @Override
  public void setValue(Object value) {
    int index = -1;
    for(int i = 0; i < data.length; i++) {
      if(equals(value, data[i])) {
        index = i;
        break;
      }
    }
    if(index == -1)
      throw new IllegalArgumentException("invalid sequence element");
    curent = index;
    fireStateChanged();
  }

  @Override
  public Object getNextValue() {
    return (curent == data.length - 1) ? data[0] : data[curent + 1];
  }

  @Override
  public Object getPreviousValue() {
    return (curent == 0) ? data[data.length - 1] : data[curent - 1];
  }

  private boolean equals(Object o1, Object o2) {
    if(o1 == null)
      return o2 == null;
    else
      return o1.equals(o2);
  }
}