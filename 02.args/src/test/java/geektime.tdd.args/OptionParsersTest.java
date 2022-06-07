package geektime.tdd.args;

import geektime.tdd.args.annotation.Option;
import geektime.tdd.args.exceptions.IllegalValueException;
import geektime.tdd.args.exceptions.InsufficientArgumentsException;
import geektime.tdd.args.exceptions.TooManyArgumentsException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.annotation.Annotation;
import java.util.function.Function;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class OptionParsersTest {

    static Option option(String value) {
        return new Option() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }

            @Override
            public String value() {
                return value;
            }
        };
    }

    @Nested
    class UnaryOptionParser {
        @Test
        public void should_not_accept_extra_argument_for_single_value_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
                new OptionParsers().unary(0, Integer::parseInt).parse(asList("-p", "8080", "8081"), option("p"));
            });
            assertEquals("p", e.getOption());
        }

        @ParameterizedTest
        @ValueSource(strings = {"-p -l", "-p"})
        public void should_not_accept_insufficient_argument_for_single_value_option(String arguments) {
            InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
                new OptionParsers().unary(0, Integer::parseInt).parse(asList(arguments.split(" ")), option("p"));
            });
            assertEquals("p", e.getOption());
        }

        @Test
        public void should_set_default_value_for_single_value_option() {
            Function<String, Object> whatever = (it) -> null;
            Object defaultValue = new Object();
            assertSame(defaultValue, new OptionParsers().unary(defaultValue, whatever).parse(asList(), option("p")));
        }

        @Test
        public void should_parse_value_if_flag_present() {
            Object parsed = new Object();
            Function<String, Object> parse = it -> parsed;
            Object defaultValue = new Object();
            assertSame(parsed, new OptionParsers().unary(defaultValue, parse).parse(asList("-p", "8080"), option("p")));
        }
    }

    @Nested
    class BooleanOptionParser {
        @Test
        public void should_not_accept_extra_argument_for_boolean_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () ->
                    new OptionParsers().bool().parse(asList("-l", "t"), option("l")));
            assertEquals("l", e.getOption());
        }

        @Test
        public void should_set_default_value_to_false_for_boolean_option() {
            assertFalse(new OptionParsers().bool().parse(asList(), option("l")));
        }

        @Test
        public void should_set_default_value_to_true_if_flag_present() {
            assertTrue(new OptionParsers().bool().parse(asList("-l"), option("l")));
        }
    }

    @Nested
    class ListOptionParser {
        //TODO: -g "this" "is" {"this", is"}
        @Test
        public void should_parse_list_option() {
            String[] values = new OptionParsers().list(String[]::new, String::valueOf).parse(asList("-g", "this", "is"), option("g"));
            assertArrayEquals(new String[]{"this", "is"}, values);
        }

        //TODO: default value []
        @Test
        public void should_use_empty_array_as_default_value() {
            String[] values = new OptionParsers().list(String[]::new, String::valueOf).parse(asList(), option("g"));
            assertEquals(0, values.length);
        }
        //TODO: -d a throw exception

        @Test
        public void should_throw_exception_if_value_parser_cannot_parse_value() {
            Function<String, String> parser = it -> {
                throw new RuntimeException();
            };
            IllegalValueException e = assertThrows(IllegalValueException.class,
                    () -> new OptionParsers().list(String[]::new, parser).parse(asList("-g", "this", "is"), option("g")));
            assertEquals("g", e.getOption());
            assertEquals("this", e.getValue());
        }

        @Test
        public void should_not_treat_negative_int_as_flag() {
            Integer[] values = new OptionParsers().list(Integer[]::new, Integer::parseInt).parse(asList("-d", "-1", "-2"), option("d"));
            assertArrayEquals(new Integer[]{-1, -2}, values);
        }

    }
}
