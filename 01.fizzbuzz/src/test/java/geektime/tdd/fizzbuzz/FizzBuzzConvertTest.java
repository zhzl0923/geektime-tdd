package geektime.tdd.fizzbuzz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FizzBuzzConvertTest {
  private FizzBuzzConverter converter;

  @BeforeEach
  public void before() {
    converter = new FizzBuzzConverter();
  }

  @Test
  public void test() {
    for (int i = 1; i <= 100; i++) {
      if (i % 15 == 0) {
        assertEquals("FizzBuzz", converter.convert(i));
      } else if (i % 3 == 0) {
        assertEquals("Fizz", converter.convert(i));
      } else if (i % 5 == 0) {
        assertEquals("Buzz", converter.convert(i));
      } else {
        assertEquals(String.valueOf(i), converter.convert(i));
      }
    }
  }

  // 从 1 至 100 依次打印，如 输入1 输出 “1”，输入 2 输出 “2”
  @Test
  public void should_return_1_when_input_1() {
    assertEquals("1", converter.convert(1));
  }

  // 如果碰到被 3 整除的数则输出 “Fizz”
  @Test
  public void should_return_fizz_when_number_is_divided_by_3() {
    assertEquals("Fizz", converter.convert(3));
  }

  // 如果碰到被 5 整除的数则输出 “Buzz”
  @Test
  public void should_return_Buzz_when_number_is_divided_by_3() {
    assertEquals("Buzz", converter.convert(5));
  }

  // 如果同时被 3 和 5 整除则输出 “FizzBuzz”
  @Test
  public void should_return_FizzBuzz_when_number_is_divided_by_3_and_5() {
    assertEquals("FizzBuzz", converter.convert(15));
  }
}
