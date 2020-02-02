package io.github.graphqly.reflector.metadata.strategy.query;

public class MemberOperationInfoGenerator extends DefaultOperationInfoGenerator {

  public MemberOperationInfoGenerator() {
    withDelegate(new AnnotatedOperationInfoGenerator());
  }
}
