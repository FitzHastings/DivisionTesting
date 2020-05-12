package bum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import util.DBRelation.ActionType;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToOne {
  public ActionType on_delete() default ActionType.CASCADE;
  public ActionType on_update() default ActionType.CASCADE;
  
  public String description() default "";
  public String mappedBy() default "";
  public boolean updateOnChanged() default false;
  public String[] viewFields() default {};
  public String[] viewNames() default {};
  public boolean nullable() default true;
  public boolean saveNow() default false;
}
