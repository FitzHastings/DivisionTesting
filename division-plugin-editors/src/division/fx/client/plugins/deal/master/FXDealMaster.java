package division.fx.client.plugins.deal.master;

import division.fx.PropertyMap;

public class FXDealMaster extends MasterPane {
  private final PropertyMap deal;

  public FXDealMaster(PropertyMap deal) {
    this.deal = deal;
    
    stepPanels().add(new FXDealProcessStep("Процессы", deal));
    stepPanels().add(new FXDealPositionsStep("Объекты", deal));
    stepPanels().add(new FXDealFinalStep("Реквизиты", deal));
  }
}