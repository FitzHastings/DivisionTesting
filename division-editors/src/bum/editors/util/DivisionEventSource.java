package bum.editors.util;

public class DivisionEventSource {
    private String sourceClassName;
    private Integer sourceId;

    public DivisionEventSource(String sourceClassName) {
      this(sourceClassName, null);
    }

    public DivisionEventSource(String sourceClassName, Integer sourceId) {
      this.sourceClassName = sourceClassName;
      this.sourceId = sourceId;
    }

    public String getSourceClassName() {
      return sourceClassName;
    }

    public Integer getSourceId() {
      return sourceId;
    }

    @Override
    public boolean equals(Object obj) {
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final DivisionEventSource other = (DivisionEventSource)obj;
      if ((this.sourceClassName == null) ? (other.sourceClassName != null) : !this.sourceClassName.equals(other.sourceClassName)) {
        return false;
      }
      if (this.sourceId != other.sourceId && (this.sourceId == null || !this.sourceId.equals(other.sourceId))) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 67 * hash + (this.sourceClassName != null ? this.sourceClassName.hashCode() : 0);
      hash = 67 * hash + (this.sourceId != null ? this.sourceId.hashCode() : 0);
      return hash;
    }
  }