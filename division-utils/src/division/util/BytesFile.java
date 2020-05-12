package division.util;

public class BytesFile {
  private final String name;
  private final byte[] source;

  public BytesFile(String name, byte[] source) {
    this.name = name;
    this.source = source;
  }

  public String getName() {
    return name;
  }

  public byte[] getSource() {
    return source;
  }
}