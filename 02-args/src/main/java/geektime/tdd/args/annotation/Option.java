package geektime.tdd.args.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令行参数注解.
 * 用于修饰需要绑定的命令行参数,例如, -p 8080 会被绑定到被 @Option("p")修饰的字段上.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {
  /**
   * 注解的值.
   */
  String value();
}
