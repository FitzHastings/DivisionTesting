package bum.realizations;

import bum.annotations.Column;
import bum.annotations.OneToMany;
import bum.annotations.Table;
import bum.annotations.Trigger;
import bum.annotations.Triggers;
import bum.interfaces.CompanyPartitionDocument;
import bum.interfaces.Document;
import bum.interfaces.DocumentXMLTemplate;
import bum.interfaces.ProductDocument;
import bum.interfaces.ProductDocument.ActionType;
import bum.interfaces.Service;
import bum.interfaces.Service.Owner;
import java.rmi.RemoteException;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import mapping.MappingObjectImpl;

@Table

@Triggers(triggers = {
  @Trigger(
          timeType = Trigger.TIMETYPE.AFTER,
          actionTypes = {Trigger.ACTIONTYPE.UPDATE},
          procedureText = ""
                  + "DECLARE\n"
                  + "  cp record;\n"
                  + "BEGIN\n"
                  
                  + "  IF NEW.type != OLD.type THEN\n"
                  + "    UPDATE [CompanyPartitionDocument] SET [!CompanyPartitionDocument(type)]=NEW.type WHERE [CompanyPartitionDocument(document)]=NEW.id;\n"
                  + "  END IF;\n"
                  
                  + "  IF NEW.tmp != OLD.tmp THEN\n"
                  + "    UPDATE [CompanyPartitionDocument] SET [!CompanyPartitionDocument(tmp)]=NEW.tmp WHERE [CompanyPartitionDocument(document)]=NEW.id;\n"
                  + "  END IF;\n"
                  
                  + "  RETURN NEW;\n"
                  + "END\n"
  ),
  
  @Trigger(
          timeType = Trigger.TIMETYPE.BEFORE,
          actionTypes = {Trigger.ACTIONTYPE.UPDATE},
          procedureText = ""
                  + "BEGIN\n"
                  + "  IF NEW.system = true THEN\n"
                  
                  + "    IF NEW.[!Document(actionType)] != OLD.[!Document(actionType)] THEN\n"
                  
                  + "      IF NEW.[!Document(actionType)] = 'СТАРТ' THEN\n"
                  + "        NEW.[!Document(movable)]    = NULL;\n"
                  + "      END IF;\n"
                  
                  + "      IF NEW.[!Document(actionType)] = 'ОПЛАТА' THEN\n"
                  + "        NEW.[!Document(movable)]    = true;\n"
                  + "      END IF;\n"
                  
                  + "    END IF;\n"
                  
                  + "    IF NEW.[!Document(actionType)] != OLD.[!Document(actionType)] OR "
                  + "        NEW.[!Document(movable)] != OLD.[!Document(movable)] OR "
                  + "        NEW.[!Document(movable)] IS NULL AND OLD.[!Document(movable)] IS NOT NULL OR "
                  + "        NEW.[!Document(movable)] IS NOT NULL AND OLD.[!Document(movable)] IS NULL "
                  + "    THEN\n"
                  + "      NEW.[!Document(moneyCash)]       = CASE WHEN NEW.[!Document(movable)] IS NULL THEN true ELSE NEW.[!Document(movable)] END;\n"
                  + "      NEW.[!Document(moneyCashLess)]   = NEW.[!Document(moneyCash)];\n"
                  + "      NEW.[!Document(tmcCash)]         = NEW.[!Document(moneyCash)];\n"
                  + "      NEW.[!Document(tmcCashLess)]     = NEW.[!Document(moneyCash)];\n"
                  + "    END IF;\n"
                  
                  + "  END IF;\n"
                  + "  RETURN NEW;\n"
                  + "END\n"
  )
})

public class DocumentImpl extends MappingObjectImpl implements Document {
  @Column
  private String description;

  @OneToMany(mappedBy="document")
  private List<DocumentXMLTemplate> templates = new ArrayList<>();

  @Column(length=1000)
  private String script;
  
  @Column
  private String scriptLanguage;
  
  /**
   * Формировать документ, если:
   */
  
  @Column(defaultValue = "false", nullable = false)
  private Boolean system          = false;
  
  @Column(defaultValue = "false", nullable = false)
  private boolean actionConfirm = false;
  
  
  //Источник документа
  @Column(defaultValue = "SELLER", nullable = false)
  private Owner documentSource    = Service.Owner.SELLER;
  
  //Источник документа плательщик НДС???? (да, нет, неважно)
  @Column(defaultValue = "NULL")
  private Boolean ndsPayer        = null;
  
  //Произошло данное событие
  @Column
  private ProductDocument.ActionType actionType;
  
  //Перемещение при событии (да, нет, неважно)
  @Column(defaultValue = "true")
  private Boolean movable         = true;
  
  //Депозитарий - наличные деньги
  @Column(defaultValue = "true", nullable = false)
  private Boolean moneyCash       = true;
  
  //Депозитарий - безналичные деньги
  @Column(defaultValue = "true", nullable = false)
  private Boolean moneyCashLess   = true;
  
  //Депозитарий - наличные ТМЦ
  @Column(defaultValue = "true", nullable = false)
  private Boolean tmcCash         = true;
  
  //Депозитарий - безналичные ТМЦ
  @Column(defaultValue = "true", nullable = false)
  private Boolean tmcCashLess     = true;
  
  
  
  
  @Column(defaultValue="")
  private String prefix;
  
  @Column
  private String prefixTypeFormat;
  
  @Column(defaultValue="")
  private String prefixSplit;
  
  @Column(defaultValue="")
  private String suffixSplit;
  
  @Column
  private String suffixTypeFormat;

  @Column(defaultValue="")
  private String suffix;

  @Column
  private Period periodForZero;
  
  @Column(defaultValue="0")
  private Integer startNumber;
  
  @Column(defaultValue = "false")
  private Boolean grabFreeNumber;
  

  public DocumentImpl() throws RemoteException {
  }

  @Override
  public boolean isActionConfirm() {
    return actionConfirm;
  }

  @Override
  public void setActionConfirm(boolean actionConfirm) {
    this.actionConfirm = actionConfirm;
  }
  
  
  @Override
  public String getPrefix() {
    return prefix;
  }
  
  @Override
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
  
  @Override
  public String getPrefixTypeFormat() {
    return prefixTypeFormat;
  }
  
  @Override
  public void setPrefixTypeFormat(String prefixTypeFormat) {
    this.prefixTypeFormat = prefixTypeFormat;
  }
  
  @Override
  public String getPrefixSplit() {
    return prefixSplit;
  }
  
  @Override
  public void setPrefixSplit(String prefixSplit) {
    this.prefixSplit = prefixSplit;
  }
  
  @Override
  public String getSuffixSplit() {
    return suffixSplit;
  }
  
  @Override
  public void setSuffixSplit(String suffixSplit) {
    this.suffixSplit = suffixSplit;
  }
  
  @Override
  public String getSuffixTypeFormat() {
    return suffixTypeFormat;
  }
  
  @Override
  public void setSuffixTypeFormat(String suffixTypeFormat) {
    this.suffixTypeFormat = suffixTypeFormat;
  }
  
  @Override
  public String getSuffix() {
    return suffix;
  }
  
  @Override
  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }
  
  @Override
  public Period getPeriodForZero() {
    return periodForZero;
  }
  
  @Override
  public void setPeriodForZero(Period periodForZero) {
    this.periodForZero = periodForZero;
  }
  
  @Override
  public Integer getStartNumber() {
    return startNumber;
  }
  
  @Override
  public void setStartNumber(Integer startNumber) {
    this.startNumber = startNumber;
  }
  
  @Override
  public Boolean isGrabFreeNumber() {
    return grabFreeNumber;
  }
  
  @Override
  public void setGrabFreeNumber(Boolean grabFreeNumber) {
    this.grabFreeNumber = grabFreeNumber;
  }
  
  
  
  @Override
  public String getScriptLanguage() throws RemoteException {
    return scriptLanguage;
  }
  
  @Override
  public void setScriptLanguage(String scriptLanguage) throws RemoteException {
    this.scriptLanguage = scriptLanguage;
  }
  
  @Override
  public Boolean isSystem() throws RemoteException {
    return system;
  }

  @Override
  public void setSystem(Boolean system) throws RemoteException {
    this.system = system;
  }
  
  @Override
  public Owner getDocumentSource() throws RemoteException {
    return documentSource;
  }
  
  @Override
  public void setDocumentSource(Owner documentSource) throws RemoteException {
    this.documentSource = documentSource;
  }
  
  @Override
  public Boolean isNdsPayer() throws RemoteException {
    return ndsPayer;
  }
  
  @Override
  public void setNdsPayer(Boolean ndsPayer) throws RemoteException {
    this.ndsPayer = ndsPayer;
  }
  
  @Override
  public Boolean isMovable() throws RemoteException {
    return movable;
  }
  
  @Override
  public void setMovable(Boolean movable) throws RemoteException {
    this.movable = movable;
  }

  @Override
  public ActionType getActionType() throws RemoteException {
    return actionType;
  }

  @Override
  public void setActionType(ActionType actionType) throws RemoteException {
    this.actionType = actionType;
  }

  @Override
  public Boolean isMoneyCash() throws RemoteException {
    return moneyCash;
  }

  @Override
  public void setMoneyCash(Boolean moneyCash) throws RemoteException {
    this.moneyCash = moneyCash;
  }

  @Override
  public Boolean isMoneyCashLess() throws RemoteException {
    return moneyCashLess;
  }

  @Override
  public void setMoneyCashLess(Boolean moneyCashLess) throws RemoteException {
    this.moneyCashLess = moneyCashLess;
  }

  @Override
  public Boolean isTmcCash() throws RemoteException {
    return tmcCash;
  }

  @Override
  public void setTmcCash(Boolean tmcCash) throws RemoteException {
    this.tmcCash = tmcCash;
  }

  @Override
  public Boolean isTmcCashLess() throws RemoteException {
    return tmcCashLess;
  }

  @Override
  public void setTmcCashLess(Boolean tmcCashLess) throws RemoteException {
    this.tmcCashLess = tmcCashLess;
  }
  
  
  
  
  
  
  /*@Override
  public Boolean isWithoutProducts() throws RemoteException {
    return withoutProducts;
  }

  @Override
  public void setWithoutProducts(Boolean withoutProducts) throws RemoteException {
    this.withoutProducts = withoutProducts;
  }

  @Override
  public Boolean isPaymentReason() throws RemoteException {
    return paymentReason;
  }

  @Override
  public void setPaymentReason(Boolean paymentReason) throws RemoteException {
    this.paymentReason = paymentReason;
  }*/
  
  @Override
  public void setScript(String script) throws RemoteException {
    this.script = script;
  }

  @Override
  public String getScript() throws RemoteException {
    return this.script;
  }

  @Override
  public List<DocumentXMLTemplate> getTemplates() throws RemoteException {
    return templates;
  }

  @Override
  public void setTemplates(List<DocumentXMLTemplate> templates) throws RemoteException {
    this.templates = templates;
  }

  @Override
  public String getDescription() throws RemoteException {
    return description;
  }

  @Override
  public void setDescription(String description) throws RemoteException {
    this.description = description;
  }
}