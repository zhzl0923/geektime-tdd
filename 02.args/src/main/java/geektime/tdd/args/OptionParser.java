package geektime.tdd.args;

import geektime.tdd.args.annotation.Option;
import java.util.List;

/**
 * 参数解析器接口.
 */
interface OptionParser<T> {
  T parse(List<String> arguments, Option option);
}
