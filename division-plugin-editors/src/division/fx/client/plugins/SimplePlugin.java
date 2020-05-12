package division.fx.client.plugins;

import division.fx.PropertyMap;
import division.fx.editor.FXEditor;

public class SimplePlugin extends FXPlugin {
  private final FXEditor<PropertyMap> content;

  public SimplePlugin(FXEditor<PropertyMap> content) {
    this.content = content;
  }

  @Override
  public FXEditor<PropertyMap> getContent() {
    return content;
  }
  
  @Override
  public void start() {
    content.initData();
    show();
  }
}