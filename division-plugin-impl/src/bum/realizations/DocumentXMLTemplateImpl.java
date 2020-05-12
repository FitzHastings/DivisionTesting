package bum.realizations;

import bum.annotations.Column;
import bum.annotations.ManyToOne;
import bum.annotations.Table;
import bum.annotations.Trigger;
import bum.annotations.Triggers;
import bum.interfaces.CompanyPartition;
import bum.interfaces.Document;
import bum.interfaces.DocumentXMLTemplate;
import java.rmi.RemoteException;

@Table

@Triggers(triggers = {
  @Trigger(timeType=Trigger.TIMETYPE.BEFORE,
  actionTypes={Trigger.ACTIONTYPE.UPDATE},
  procedureText=
    "DECLARE "
    + "BEGIN"
    + "  IF NEW.main != OLD.main AND NEW.main = true THEN"
    + "    UPDATE [DocumentXMLTemplate] SET main=false "
    + "      WHERE id != NEW.id AND [DocumentXMLTemplate(document)] = NEW.[!DocumentXMLTemplate(document)] AND [DocumentXMLTemplate(companyPartition)] = NEW.[!DocumentXMLTemplate(companyPartition)];"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;")
})

public class DocumentXMLTemplateImpl extends XMLTemplateImpl implements DocumentXMLTemplate {
  @ManyToOne
  private Document document;
  
  @ManyToOne
  private CompanyPartition companyPartition;
  
  @Column(defaultValue="false")
  private Boolean main = false;

  public DocumentXMLTemplateImpl() throws RemoteException {
    setObjectClassName(DocumentImpl.class.getName());
  }
  
  @Override
  public CompanyPartition getCompanyPartition() throws RemoteException {
    return companyPartition;
  }
  
  @Override
  public void setCompanyPartition(CompanyPartition companyPartition) throws RemoteException {
    this.companyPartition = companyPartition;
  }

  @Override
  public Document getDocument() throws RemoteException {
    return document;
  }

  @Override
  public void setDocument(Document document) throws RemoteException {
    this.document = document;
  }

  @Override
  public Boolean isMain() throws RemoteException {
    return main;
  }

  @Override
  public void setMain(Boolean main) throws RemoteException {
    this.main = main;
  }
}