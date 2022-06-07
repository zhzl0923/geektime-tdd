package geektime.tdd.args;

import geektime.tdd.args.annotation.Option;
import geektime.tdd.args.exceptions.IllegalOptionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {

    // -l -p 8080 -d /usr/logs
    @Test
    public void should_parse_multi_options() {
        Args<MultiOptions> args = new Args<>();
        MultiOptions option = args.parse(MultiOptions.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(option.logging());
        assertEquals(8080, option.port());
        assertEquals("/usr/logs", option.directory());
    }

    @Test
    public void should_throw_illegal_option_exception_if_annotation_not_present() {
        Args<OptionsWithoutAnnotation> args = new Args<>();
        IllegalOptionException e = assertThrows(
                IllegalOptionException.class,
                () -> args.parse(OptionsWithoutAnnotation.class, "-l", "-p", "8080", "-d", "/usr/logs")
        );
        assertEquals("port", e.getParameter());
    }

    //-g this is a list -d 1 2 -3 5 "g"表示一个字符串列表[“this”, “is”, “a”, “list”]，“d"标志表示一个整数列表[1, 2, -3, 5]。
    @Test
    public void should_parse_group_option() {
        Args<ListOptions> args = new Args<>();
        ListOptions option = args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, option.group());
        assertArrayEquals(new Integer[]{1, 2, -3, 5}, option.decimals());
    }

    @Test
    public void should_throw_runtime_exception_if_cannot_parse_to_expected_option() {
        Args<ListOptions> args = new Args<>();
        assertThrows(RuntimeException.class,
                () -> args.parse(ListOptions.class, "-l", "-p", "8080", "-d", "/usr/logs"));
    }

    record MultiOptions(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
    }

    record OptionsWithoutAnnotation(@Option("l") boolean logging, int port, @Option("d") String directory) {
    }

    record ListOptions(@Option("g") String[] group, @Option("d") Integer[] decimals) {
    }
}
