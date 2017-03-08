package candle

import org.joda.time.Instant
import com.github.nscala_time.time.Imports._
import play.api.libs.json._

/**
  * Raw binary order
  *
  * @param len       length
  * @param timestamp timestamp
  * @param tickerLen tickerLen
  * @param data      ticker
  * @param price     price
  * @param volume    volume
  */
case class RawModel(len: Int, timestamp: Long, tickerLen: Int, data: Vector[Byte], price: Double, volume: Int) {
  def toModel: Order = {
    Order(new Instant(timestamp), new String(data.toArray), price, volume)
  }
}

/**
  * Order
  *
  * @param timestamp timestamp
  * @param ticker    ticker
  * @param price     price
  * @param volume    volume
  */
case class Order(timestamp: Instant, ticker: String, price: Double, volume: Int) {
  def toCandle: Candle = {
    Candle(ticker, DateUtils.truncateToMinute(timestamp), price, price, price, price, volume)
  }
}

/**
  * Candle
  *
  * @param ticker    ticker
  * @param timestamp timestamp
  * @param open      open
  * @param high      high
  * @param low       low
  * @param close     close
  * @param volume    volume
  */
case class Candle(ticker: String, timestamp: Instant, open: Double, high: Double, low: Double, close: Double, volume: Int) {
  def reduce(other: Order): Candle = {
    require(ticker == other.ticker)
    require(timestamp <= other.timestamp)
    Candle(ticker, timestamp, open, Math.max(high, other.price), Math.min(low, other.price), other.price, volume + other.volume)
  }
}

/**
  * Json formatter companion object
  */
object JsonFormatters {
  implicit val formatInstant: Writes[Instant] = new Writes[Instant] {
    override def writes(instant: Instant): JsValue = JsString(instant.toDateTime.toString)
  }

  implicit val candleFormatter: Writes[Candle] = Json.writes[Candle]

  def format(values: List[Candle]): String = {
    values.map(x => Json.toJson(x)).mkString("", "\n", "\n")
  }
}
