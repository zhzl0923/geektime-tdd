package geektime.tdd.args.exceptions;

/**
 * 命令行参数不足时抛出.
 */
public class InsufficientArgumentsException extends RuntimeException {
    String option;

    public InsufficientArgumentsException(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}
