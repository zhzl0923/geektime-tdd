package geektime.tdd.fizzbuzz;

/**
 * 默认规则.
 */
class DefaultRule implements Rule {

  @Override
  public String result(int number) {
    return String.valueOf(number);
  }
}
