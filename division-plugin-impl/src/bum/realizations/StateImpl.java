package bum.realizations;

import bum.annotations.Column;
import bum.annotations.Table;
import bum.interfaces.State;
import java.awt.Color;
import java.rmi.RemoteException;
import mapping.MappingObjectImpl;

@Table
public class StateImpl extends MappingObjectImpl implements State {
  @Column
  private Color color;
  @Column
  private String nickForContractSeller;
  @Column
  private String nickForDealSeller;
  @Column
  private String nickForContractCustomer;
  @Column
  private String nickForDealCustomer;
  @Column
  private Integer stateNumber;

  public StateImpl() throws RemoteException {
  }

  @Override
  public Color getColor() throws RemoteException {
    return color;
  }

  @Override
  public void setColor(Color color) throws RemoteException {
    this.color = color;
  }

  @Override
  public Integer getStateNumber() throws RemoteException {
    return stateNumber;
  }

  @Override
  public void setStateNumber(Integer stateNumber) throws RemoteException {
    this.stateNumber = stateNumber;
  }

  @Override
  public String getNickForContractCustomer() throws RemoteException {
    return nickForContractCustomer;
  }

  @Override
  public void setNickForContractCustomer(String nickForContractCustomer) throws RemoteException {
    this.nickForContractCustomer = nickForContractCustomer;
  }

  @Override
  public String getNickForContractSeller() throws RemoteException {
    return nickForContractSeller;
  }

  @Override
  public void setNickForContractSeller(String nickForContractSeller) throws RemoteException {
    this.nickForContractSeller = nickForContractSeller;
  }

  @Override
  public String getNickForDealCustomer() throws RemoteException {
    return nickForDealCustomer;
  }

  @Override
  public void setNickForDealCustomer(String nickForDealCustomer) throws RemoteException {
    this.nickForDealCustomer = nickForDealCustomer;
  }

  @Override
  public String getNickForDealSeller() throws RemoteException {
    return nickForDealSeller;
  }

  @Override
  public void setNickForDealSeller(String nickForDealSeller) throws RemoteException {
    this.nickForDealSeller = nickForDealSeller;
  }

  public static void createPrimaryData() {
    /*try {
      RMITable table = DBTableFactory.getInstance().get(State.class);
      RMIDBObject[] objects = table.getObjects();
      if(objects.length == 0) {
        State state = (State)table.createObject();
        state.setStateNumber(1);
        state.setNickForContractSeller("Проект");
        state.setNickForContractCustomer("Проект");
        state.setNickForDealSeller("Проект");
        state.setNickForDealCustomer("Проект");
        state.setColor(Color.LIGHT_GRAY);
        state.saveObject();

        state = (State)table.createObject();
        state.setStateNumber(2);
        state.setNickForContractSeller("Пуск");
        state.setNickForContractCustomer("Оферта");
        state.setNickForDealSeller("Пуск");
        state.setNickForDealCustomer("Оферта");
        state.setColor(Color.YELLOW);
        state.saveObject();

        state = (State)table.createObject();
        state.setStateNumber(3);
        state.setNickForContractSeller("Пауза");
        state.setNickForContractCustomer("-");
        state.setNickForDealSeller("Пауза");
        state.setNickForDealCustomer("-");
        state.setColor(Color.LIGHT_GRAY);
        state.saveObject();

        state = (State)table.createObject();
        state.setStateNumber(4);
        state.setNickForContractSeller("Выручка");
        state.setNickForContractCustomer("Расчёт");
        state.setNickForDealSeller("Выручка");
        state.setNickForDealCustomer("Расчёт");
        state.setColor(Color.RED);
        state.saveObject();

        state = (State)table.createObject();
        state.setStateNumber(5);
        state.setNickForContractSeller("Исполнено");
        state.setNickForContractCustomer("Принято");
        state.setNickForDealSeller("Исполнено");
        state.setNickForDealCustomer("Принято");
        state.setColor(Color.BLUE);
        state.saveObject();

        state = (State)table.createObject();
        state.setStateNumber(6);
        state.setNickForContractSeller("Финиш");
        state.setNickForContractCustomer("Финиш");
        state.setNickForDealSeller("Финиш");
        state.setNickForDealCustomer("Финиш");
        state.setColor(Color.GREEN);
        state.saveObject();

        state = (State)table.createObject();
        state.setStateNumber(7);
        state.setNickForContractSeller("Неопределённость");
        state.setNickForContractCustomer("Неопределённость");
        state.setNickForDealSeller("Неопределённость");
        state.setNickForDealCustomer("Неопределённость");
        state.setColor(Color.BLACK);
        state.saveObject();
      }
    }catch(Exception ex) {
      ex.printStackTrace();
    }*/
  }
}