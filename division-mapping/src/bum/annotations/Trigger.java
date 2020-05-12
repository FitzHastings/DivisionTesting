package bum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trigger {
  enum TIMETYPE{AFTER,BEFORE};
  enum ACTIONTYPE{INSERT,DELETE,UPDATE};
  String name() default "";
  TIMETYPE timeType() default TIMETYPE.AFTER;
  ACTIONTYPE[] actionTypes() default {ACTIONTYPE.INSERT,ACTIONTYPE.DELETE,ACTIONTYPE.UPDATE};
  String linkProcedure() default "";
  String procedureText() default "";
  String language() default "plpgsql";
  String classname() default "";
}