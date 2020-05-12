package division.editors.tables;

import bum.editors.TableEditor;
import bum.interfaces.Factor;
import division.editors.objects.FactorEditor;

public class FactorTableEditor extends TableEditor {
  public FactorTableEditor() {
    super(
            new String[]{"id","Наименование","Тип","ед. измерения","Уникальность"},
            new String[]{"id","name","factorType","unit","unique"},
            Factor.class, FactorEditor.class, "реквизиты");
    getTable().getTableFilters().addTextFilter(1);
  }

  @Override
  public String getEmptyObjectTitle() {
    return "[реквизиты]";
  }
}