package io.github.graphqly.reflector.execution.complexity;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.AstPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexityAnalysisInstrumentation extends SimpleInstrumentation {

  private static final Logger log =
      LoggerFactory.getLogger(ComplexityAnalysisInstrumentation.class);
  private final ComplexityFunction complexityFunction;
  private final int maximumComplexity;

  public ComplexityAnalysisInstrumentation(
      ComplexityFunction complexityFunction, int maximumComplexity) {
    this.complexityFunction = complexityFunction;
    this.maximumComplexity = maximumComplexity;
  }

  @Override
  public InstrumentationContext<ExecutionResult> beginExecuteOperation(
      InstrumentationExecuteOperationParameters parameters) {
    ResolvedField root =
        new ComplexityAnalyzer(complexityFunction, maximumComplexity)
            .collectFields(parameters.getExecutionContext());
    if (log.isDebugEnabled()) {
      log.debug(
          "Operation {} has total complexity of {}",
          AstPrinter.printAst(
              parameters
                  .getExecutionContext()
                  .getOperationDefinition()
                  .getSelectionSet()
                  .getSelections()
                  .get(0)),
          root.getComplexityScore());
    }
    log.info("Total operation complexity: {}", root.getComplexityScore());
    return super.beginExecuteOperation(parameters);
  }
}
