package documents;

public class JSDocument {
  private String stream = "";

  public void write(String str) {
    stream += str;
  }

  public String read() {
    return stream;
  }
}