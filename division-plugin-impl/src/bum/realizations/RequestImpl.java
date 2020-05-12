package bum.realizations;

import bum.annotations.*;
import bum.interfaces.*;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;
import mapping.MappingObjectImpl;

@Table(clientName="Заявки")

@Triggers(triggers = {
  @Trigger(timeType=Trigger.TIMETYPE.BEFORE,
  actionTypes={Trigger.ACTIONTYPE.INSERT},
  procedureText=
    "DECLARE "
    + "BEGIN"
    //+ "  IF NEW.type != OLD.type THEN"
    //+ "    UPDATE [DealPosition] SET type=NEW.type WHERE [DealPosition(deal)]=OLD.id;"
    //+ "  END IF;"
    + "  IF NEW.number ISNULL THEN"
    + "    NEW.number=NEW.id;"
    + "  END IF;"
    + "  RETURN NEW;"
    + "END;")
})

public class RequestImpl extends MappingObjectImpl implements Request {
  @ManyToOne(description="Заявитель", viewFields={"name"}, viewNames={"applicant_name"})
  private Company applicant;

  @ManyToOne(description="Группа исполнителя",viewFields={"name"},viewNames={"performer_name"})
  private CFC performer;
  
  @ManyToOne(description="Исполнитель",viewFields={"people"},viewNames={"people_id"})
  private Worker performerWorker;
  
  @Column(description="Описание заявки",length=1000)
  private String reason;
  
  @Column(description = "Номер заявки", length = 20)
  private String number;
  
  @Column(defaultValue = "CURRENT_TIMESTAMP")
  private LocalDateTime startDate;
  @Column
  private LocalDateTime acceptDate;
  @Column
  private LocalDateTime executDate;
  @Column
  private LocalDateTime finishDate;
  @Column
  private LocalDateTime exitDate;
  
  @ManyToMany
  private List<Equipment> equipments;

  public RequestImpl() throws RemoteException {
  }
  
  @Override
  public String getNumber() {
    return number;
  }

  @Override
  public void setNumber(String number) {
    this.number = number;
  }

  @Override
  public List<Equipment> getEquipments() {
    return equipments;
  }

  @Override
  public void setEquipments(List<Equipment> equipments) {
    this.equipments = equipments;
  }

  @Override
  public LocalDateTime getStartDate() {
    return startDate;
  }

  @Override
  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  @Override
  public LocalDateTime getAcceptDate() {
    return acceptDate;
  }

  @Override
  public void setAcceptDate(LocalDateTime acceptDate) {
    this.acceptDate = acceptDate;
  }

  @Override
  public LocalDateTime getExecutDate() {
    return executDate;
  }

  @Override
  public void setExecutDate(LocalDateTime executDate) {
    this.executDate = executDate;
  }

  @Override
  public LocalDateTime getFinishDate() {
    return finishDate;
  }

  @Override
  public void setFinishDate(LocalDateTime finishDate) {
    this.finishDate = finishDate;
  }

  @Override
  public LocalDateTime getExitDate() {
    return exitDate;
  }

  @Override
  public void setExitDate(LocalDateTime exitDate) {
    this.exitDate = exitDate;
  }

  @Override
  public Worker getPerformerWorker() throws RemoteException {
    return performerWorker;
  }

  @Override
  public void setPerformerWorker(Worker performerWorker) throws RemoteException {
    this.performerWorker = performerWorker;
  }

  @Override
  public Company getApplicant() throws RemoteException {
    return applicant;
  }

  @Override
  public void setApplicant(Company applicant) throws RemoteException {
    this.applicant = applicant;
  }

  @Override
  public CFC getPerformer() throws RemoteException {
    return performer;
  }

  @Override
  public void setPerformer(CFC performer) throws RemoteException {
    this.performer = performer;
  }

  @Override
  public String getReason() throws RemoteException {
    return reason;
  }

  @Override
  public void setReason(String reason) throws RemoteException {
    this.reason = reason;
  }
}