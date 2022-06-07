package geektime.tdd.fizzbuzz;

import java.util.Arrays;
import java.util.List;

/**
 * FizzBuzz 转换器.
 */
public class FizzBuzzConverter {

    /**
     * 数字转换.
     *
     * @param number int
     * @return String
     */
    public String convert(int number) {
        return getRules().stream()
                .filter(r -> !r.result(number).isEmpty())
                .findFirst()
                .orElse(new DefaultRule())
                .result(number);
    }

    private List<Rule> getRules() {
        return Arrays.asList(
                DivideRule.create("FizzBuzz", 15),
                DivideRule.create("Fizz", 3),
                DivideRule.create("Buzz", 5)
        );
    }

}
