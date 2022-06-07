package geektime.tdd.args.exceptions;

/**
 * 当valueParser无法解析value时，此异常抛出.
 */
public class IllegalValueException extends RuntimeException {
    String option;
    String value;

    public IllegalValueException(String option, String value) {
        this.option = option;
        this.value = value;
    }

    public String getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }
}
