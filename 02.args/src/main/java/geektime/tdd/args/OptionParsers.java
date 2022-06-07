package geektime.tdd.args;

import geektime.tdd.args.annotation.Option;
import geektime.tdd.args.exceptions.IllegalValueException;
import geektime.tdd.args.exceptions.InsufficientArgumentsException;
import geektime.tdd.args.exceptions.TooManyArgumentsException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

class OptionParsers {
    private static <T> T parseValue(Option option, String value, Function<String, T> valueParser) {
        try {
            return valueParser.apply(value);
        } catch (Exception e) {
            throw new IllegalValueException(option.value(), value);
        }
    }

    private static Optional<List<String>> values(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        return Optional.ofNullable(index == -1 ? null : values(arguments, index));
    }

    private static Optional<List<String>> values(List<String> arguments,
                                                 Option option,
                                                 int expectedSize) {
        return values(arguments, option).map(it -> checkSize(it, option, expectedSize));
    }

    private static List<String> values(List<String> arguments, int index) {
        int following = IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).matches("^-[a-zA-Z-]+$"))
                .findFirst()
                .orElse(arguments.size());
        return arguments.subList(index + 1, following);
    }

    private static List<String> checkSize(List<String> values, Option option, int expectedSize) {
        if (values.size() < expectedSize) {
            throw new InsufficientArgumentsException(option.value());
        }
        if (values.size() > expectedSize) {
            throw new TooManyArgumentsException(option.value());
        }
        return values;
    }

    public OptionParser<Boolean> bool() {
        return (arguments, option) -> values(arguments, option, 0).isPresent();
    }

    public <T> OptionParser<T> unary(T defaultValue, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option, 1)
                .map(it -> parseValue(option, it.get(0), valueParser))
                .orElse(defaultValue);
    }

    public <T> OptionParser<T[]> list(IntFunction<T[]> generator, Function<String, T> valueParser) {
        return (arguments, option) -> values(arguments, option)
                .map(it -> it.stream().map(v -> parseValue(option, v, valueParser)).toArray(generator))
                .orElse(generator.apply(0));
    }

}
