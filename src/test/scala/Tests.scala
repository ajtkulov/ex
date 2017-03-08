import akka.util.ByteString
import candle.{Aggregate, BinaryConverter, Candle, Order}
import org.joda.time.Instant
import org.scalatest.FunSuite

class Tests extends FunSuite {
  test("BinaryConverter - 1") {
    val input = ByteString(0, 25, 0, 0, 1, 90, -81, 101, -32, -119, 0, 3, 83, 80, 89, 64, 91, 76, -52, -52, -52, -52, -51, 0, 0, 22, -88)
    val output = BinaryConverter.convert(input.toArray)

    val model = Some(Order(new Instant(1489001373833L), "SPY", 109.2, 5800))

    assert(output == model)
  }

  //Ticker len == 4, corrupt
  test("BinaryConverter - 2") {
    val input = ByteString(0, 25, 0, 0, 1, 90, -81, 101, -32, -119, 0, 4, 83, 80, 89, 64, 91, 76, -52, -52, -52, -52, -51, 0, 0, 22, -88)
    val output = BinaryConverter.convert(input.toArray)

    assert(output.isEmpty)
  }

  // Size of data is incomplete
  test("BinaryConverter - 3") {
    val input = ByteString(0, 25, 0, 0, 1, 90, -81, 101, -32, -119, 0, 3, 83, 80, 89, 64, 91, 76, -52, -52, -52, -52, -51, 0, 0, 22)
    val output = BinaryConverter.convert(input.toArray)

    assert(output.isEmpty)
  }

  // Size of data is "over-complete"
  test("BinaryConverter - 4") {
    val input = ByteString(0, 25, 0, 0, 1, 90, -81, 101, -32, -119, 0, 3, 83, 80, 89, 64, 91, 76, -52, -52, -52, -52, -51, 0, 0, 22, -88, 20)
    val output = BinaryConverter.convert(input.toArray)

    assert(output.isEmpty)
  }

  test("Aggregate - 1") {
    val res = Aggregate.fold(List(
      Order(new Instant(1), "SPY", 106.65, 3400),
      Order(new Instant(2), "SPY", 92.65, 5800),
      Order(new Instant(3), "SPY", 106.00, 4600),
      Order(new Instant(4), "SPY", 93.10, 4700),
      Order(new Instant(5), "SPY", 108.15, 4200)
    ))

    assert(res == Candle("SPY", new Instant(0), 106.65, 108.15, 92.65, 108.15, 22700))
  }
}