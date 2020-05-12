package util;

import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;

public class Client implements Serializable {
  private String IP;
  private Integer cfcId;
  private Integer workerId;
  private Integer peopleId;
  private Long sessionId;
  private Integer[] cfcTree = new Integer[0];

  public Client() {
  }
  
  public boolean isPartner() {
    return ArrayUtils.contains(cfcTree, 1942);
  }

  public String getIP() {
    return this.IP;
  }

  public void setIP(String IP) {
    this.IP = IP;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }

  public Integer getWorkerId() {
    return workerId;
  }

  public void setWorkerId(Integer workerId) {
    this.workerId = workerId;
  }
  
  public Integer getPeopleId() {
    return peopleId;
  }

  public void setPeopleId(Integer peopleId) {
    this.peopleId = peopleId;
  }

  public Integer getCfcId() {
    return cfcId;
  }

  public void setCfcId(Integer cfcId) {
    this.cfcId = cfcId;
  }

  public Integer[] getCfcTree() {
    return cfcTree;
  }

  public void setCfcTree(Integer[] cfcTree) {
    this.cfcTree = cfcTree;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Client other = (Client) obj;
    if (!Objects.equals(this.workerId, other.workerId)) {
      return false;
    }
    if (!Objects.equals(this.peopleId, other.peopleId)) {
      return false;
    }
    if (!Objects.equals(this.sessionId, other.sessionId)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 61 * hash + Objects.hashCode(this.workerId);
    hash = 61 * hash + Objects.hashCode(this.peopleId);
    hash = 61 * hash + Objects.hashCode(this.sessionId);
    return hash;
  }
}