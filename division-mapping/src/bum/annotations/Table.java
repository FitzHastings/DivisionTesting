package bum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
  String name() default "null";
  boolean history() default false;
  String clientName() default "null";
  String[] viewFields() default {};
  UnicumFields[] unicumFields() default {};
  QueryColumn[] queryColumns() default {};
  
}