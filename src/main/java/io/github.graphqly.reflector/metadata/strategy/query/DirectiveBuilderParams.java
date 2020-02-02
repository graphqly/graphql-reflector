package io.github.graphqly.reflector.metadata.strategy.query;

import io.github.graphqly.reflector.execution.GlobalEnvironment;
import io.github.graphqly.reflector.generator.InputFieldBuilderRegistry;

public class DirectiveBuilderParams {

  private final GlobalEnvironment environment;
  private final InputFieldBuilderRegistry inputFieldBuilders;

  private DirectiveBuilderParams(
      GlobalEnvironment environment, InputFieldBuilderRegistry inputFieldBuilders) {
    this.environment = environment;
    this.inputFieldBuilders = inputFieldBuilders;
  }

  public static Builder builder() {
    return new Builder();
  }

  public GlobalEnvironment getEnvironment() {
    return environment;
  }

  public InputFieldBuilderRegistry getInputFieldBuilders() {
    return inputFieldBuilders;
  }

  public static class Builder {
    private GlobalEnvironment environment;
    private InputFieldBuilderRegistry inputFieldBuilders;

    public Builder withEnvironment(GlobalEnvironment environment) {
      this.environment = environment;
      return this;
    }

    public Builder withInputFieldBuilders(InputFieldBuilderRegistry inputFieldBuilders) {
      this.inputFieldBuilders = inputFieldBuilders;
      return this;
    }

    public DirectiveBuilderParams build() {
      return new DirectiveBuilderParams(environment, inputFieldBuilders);
    }
  }
}
