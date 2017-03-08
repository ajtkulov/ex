package candle

import org.joda.time.{Duration, Instant}
import com.github.nscala_time.time.Imports._

/**
  * Aggregate companion object
  */
object Aggregate {
  lazy val defaultInitLagInMinutes = ConfigUtils.config().getInt("initLagInMinutes")

  def init(values: List[Order], time: Instant): List[Candle] = {
    val beginTime = time.minus(Duration.standardMinutes(defaultInitLagInMinutes))
    val filtered = values.filter(x => x.timestamp >= beginTime && x.timestamp < time)
    filtered.groupBy(x => DateUtils.truncateToMinute(x.timestamp)).mapValues(aggregate).values.flatten.toList.sortBy(_.timestamp)
  }

  def lastMinute(values: List[Order], time: Instant): List[Candle] = {
    val beginTime = time.minus(Duration.standardMinutes(1))
    val filtered = values.filter(x => x.timestamp >= beginTime && x.timestamp < time)
    aggregate(filtered)
  }

  def aggregate(values: List[Order]): List[Candle] = {
    values.groupBy(_.ticker).mapValues(_.sortBy(_.timestamp)).mapValues(fold).values.toList
  }

  def fold(values: List[Order]): Candle = {
    require(values.nonEmpty)
    val head = values.head
    values.drop(1).foldLeft(head.toCandle)((a, b) => a.reduce(b))
  }
}
