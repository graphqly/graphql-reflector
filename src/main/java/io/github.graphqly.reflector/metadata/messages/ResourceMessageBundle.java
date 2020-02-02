package io.github.graphqly.reflector.metadata.messages;

import java.util.ResourceBundle;

public class ResourceMessageBundle implements MessageBundle {

  private final ResourceBundle resourceBundle;

  public ResourceMessageBundle(ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
  }

  @Override
  public String getMessage(String key) {
    return resourceBundle.getString(key);
  }
}
