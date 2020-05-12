package division.plugin;

import bum.editors.DivisionDesktop;

public class RootPlugin implements DivisionPlugin {
  private DivisionDesktop desktop;

  @Override
  public DivisionDesktop getDesktop() {
    return desktop;
  }

  @Override
  public void setDesktop(DivisionDesktop desktop) {
    this.desktop = desktop;
  }
}