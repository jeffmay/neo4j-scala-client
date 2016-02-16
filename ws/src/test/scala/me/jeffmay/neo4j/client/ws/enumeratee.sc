import jawn.AsyncParser
import play.api.libs.iteratee._
import play.api.libs.json.{JsValue, JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}

val json = Json.obj(
  "hello" -> "world",
  "welcome" -> "to",
  "array" -> Json.arr(
    Json.obj(
      "data" -> Json.arr(1, 2, 3, 4)
    ),
    Json.obj(
      "data" -> Json.arr(5, 6, 7, 8)
    )
  )
)
val jsonAsString = Json.stringify(json)
val jsonAsBytes = jsonAsString.getBytes("UTF-8")
val jsonChunked = jsonAsBytes.grouped(4).toSeq
jsonChunked.size

val enumerator: Enumerator[Array[Byte]] = Enumerator(jsonChunked: _*)

val enumeratee: Enumeratee[Array[Byte], Seq[(String, JsValue)]] = Enumeratee.grouped[Array[Byte]].apply {
  val parser = AsyncParser[JsValue](AsyncParser.SingleValue)
  Iteratee.fold2(Seq.empty[(String, JsValue)]) {
    case (fields, bytes) =>
      parser.absorb(bytes)
      if (parser.)
  }
}