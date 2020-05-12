package division.util;

import java.io.Serializable;

public class ClientSocketId implements Serializable {
  private Long id;

  public ClientSocketId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || getClass() != obj.getClass())
      return false;
    if(this.id.longValue() != ((ClientSocketId)obj).getId().longValue())
      return false;
    return true;
  }
}