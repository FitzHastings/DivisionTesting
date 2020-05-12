package bum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Procedure {
  String name() default "";
  String procedureText() default "";
  String language() default "plpgsql";
  String[] arguments() default {};
  String returnType() default "TEXT";
}