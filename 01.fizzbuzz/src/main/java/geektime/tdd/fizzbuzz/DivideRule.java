package geektime.tdd.fizzbuzz;

/**
 * 求余规则.
 */
class DivideRule implements Rule {

    private final String result;
    private final int divisor;

    private DivideRule(String result, int divisor) {
        this.result = result;
        this.divisor = divisor;
    }

    public static Rule create(String result, int divisor) {
        return new DivideRule(result, divisor);
    }

    @Override
    public String result(int number) {
        return number % divisor == 0 ? result : "";
    }

}
