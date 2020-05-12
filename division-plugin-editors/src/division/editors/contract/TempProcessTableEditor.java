package division.editors.contract;

import bum.editors.TableEditor;
import bum.interfaces.ContractProcess;
import mapping.MappingObject.Type;

public class TempProcessTableEditor extends TableEditor {
  public TempProcessTableEditor() {
    super(new String[]{}, new String[]{}, ContractProcess.class, null, "", null, Type.CURRENT);
  }
}