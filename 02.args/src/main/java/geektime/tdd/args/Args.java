package geektime.tdd.args;

import geektime.tdd.args.annotation.Option;
import geektime.tdd.args.exceptions.IllegalOptionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 命令行解析.
 */
public class Args<T> {

  static OptionParsers optionParsers = new OptionParsers();
  static Map<Class<?>, OptionParser<?>> PARSERS = Map.of(
      boolean.class, optionParsers.bool(),
      int.class, optionParsers.unary(0, Integer::parseInt),
      String.class, optionParsers.unary("", String::valueOf),
      String[].class, optionParsers.list(String[]::new, String::valueOf),
      Integer[].class, optionParsers.list(Integer[]::new, Integer::parseInt)
  );

  private static Object parseOption(List<String> arguments, Parameter parameter) {
    if (!parameter.isAnnotationPresent(Option.class)) {
      throw new IllegalOptionException(parameter.getName());
    }
    return getOptionParser(parameter.getType())
        .parse(arguments, parameter.getAnnotation(Option.class));
  }

  private static OptionParser<?> getOptionParser(Class<?> type) {
    return PARSERS.get(type);
  }

  /**
   * 将命令行参数解析到目标对象.
   *
   * @param options 目标对象
   * @param args  命令行参数
   * @return T 返回目标对象
   */
  @SuppressWarnings("unchecked")
  public T parse(Class<T> options, String... args) {
    List<String> arguments = Arrays.asList(args);
    Constructor<?> constructor = options.getDeclaredConstructors()[0];
    try {
      Object[] values = Arrays.stream(constructor.getParameters())
          .map(it -> parseOption(arguments, it))
          .toArray();

      return (T) constructor.newInstance(values);
    } catch (IllegalOptionException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
