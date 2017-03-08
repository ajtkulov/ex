package candle

import org.joda.time.Instant

/**
  * Date utils
  */
object DateUtils {
  def truncateToMinute(time: Instant): Instant = time.toDateTime.minuteOfDay.roundFloorCopy().toInstant
}
