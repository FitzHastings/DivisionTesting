package bum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * Указывает на то что данное поле 
 * должно сохраняться в базе данных
 */
public @interface Column {
  public String description() default "";
  /**
   * Имя колонки в БД (обязательны атрибут в том случае
   * если имя данного поля совпадает с ключевым словом)
   * @return Имя колонки в БД
   */
  public String name() default "null";
  /**
   * Значение по умолчанию
   * @return Значение по умолчанию
   */
  public String defaultValue() default "null";
  /**
   * Длинна поля (если тип данного поля String, а length <= 255, то
   * тип колонки в БД будет VERCHAR(length), если length > 255, то
   * тип будет TEXT)
   * Если тип данного поля не String, то атрибут игнорируется.
   * @return Длинна поля
   */
  public int length() default 255;
  
  public boolean zip() default false;
  public boolean gzip() default false;
  
  /**
   * Указывает на то, что данное поле учавствует во View этого объекта.
   * @return true - учавствует, false - нет.
   */
  public boolean view() default true;
  public boolean nullable() default true;
  public boolean unique() default false;
  public boolean index() default false;
  public String sqlType() default "";
}
