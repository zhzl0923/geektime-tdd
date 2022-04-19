package geektime.tdd.args.exceptions;

/**
 * 当Option中的字段没有注解时，抛出此异常.
 */
public class IllegalOptionException extends RuntimeException {
  String parameter;

  public IllegalOptionException(String parameter) {
    this.parameter = parameter;
  }

  public String getParameter() {
    return parameter;
  }
}
