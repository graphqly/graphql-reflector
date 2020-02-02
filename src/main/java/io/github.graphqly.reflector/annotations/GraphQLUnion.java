package io.github.graphqly.reflector.annotations;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface GraphQLUnion {

  String name() default "";

  String description() default "";
}
