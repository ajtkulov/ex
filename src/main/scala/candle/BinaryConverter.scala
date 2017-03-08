package candle

import scodec._
import scodec.bits._
import codecs._
import scala.util._

/**
  * Binary converter from binary stream to model
  */
object BinaryConverter {

  val codec = (((("len" | int16) ::
    ("timestamp" | int64) ::
    (("ticker_len" | uint16) >>:~ { length =>
      ("data" | vectorOfN(provide(length), byte)).hlist
    })) :+  ("price" | double)) :+ ("volume" | int32))
    .as[RawModel]

  def convert(value: Array[Byte]): Option[Order] = {
    Try {
      val decode = codec.decode(BitVector(value))

      assert(decode.require.remainder.isEmpty)

      decode.getOrElse(???).map(_.toModel).value
    }.toOption
  }
}
