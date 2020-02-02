package io.github.graphqly.reflector.annotations;

import io.github.graphqly.reflector.execution.ResolutionEnvironment;
import io.github.graphqly.reflector.metadata.strategy.value.ValueMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter representing a value injected from the current execution environment.
 * Currently, the annotated parameter is allowed to be of the following types:
 *
 * <ol>
 *   <li>{@code Set<String>} - Injects the list of names of requested direct sub-fields
 *   <li>{@link graphql.language.Field} - Injects the AST {@link graphql.language.Field} currently
 *       being resolved
 *   <li>{@code List<Field>} - Injects all the AST {@link graphql.language.Field}s on the current
 *       level
 *   <li>{@link ValueMapper} - Injects a {@link
 *       ValueMapper} appropriate for the current
 *       resolver
 *   <li>{@link ResolutionEnvironment} - Injects the entire {@link
 *       ResolutionEnvironment}
 * </ol>
 */
@GraphQLIgnore
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface GraphQLEnvironment {}
