package me.jeffmay.neo4j.client.ws.json

import me.jeffmay.neo4j.client.cypher._
import me.jeffmay.neo4j.client.{Neo4jError, Neo4jStatusCode}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.util.{Success, Try}

private[json] trait SharedFormats {

  /**
    * Formats [[Neo4jStatusCode]]s as a String in Json using the full key path.
    */
  implicit lazy val formatStatusCode = new Format[Neo4jStatusCode] {
    override def reads(json: JsValue): JsResult[Neo4jStatusCode] = {
      json.validate[String].flatMap { code =>
        Neo4jStatusCode.findByCode(code) match {
          case Some(status) => JsSuccess(status)
          case None => JsError("error.expected.Neo4jStatusCode")
        }
      }
    }
    override def writes(status: Neo4jStatusCode): JsValue = JsString(status.code)
  }

  implicit lazy val readsNeo4jError: Reads[Neo4jError] = Json.reads[Neo4jError]

  lazy val datetimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("EEE, d MMM YYYY HH:mm:ss Z")
  implicit lazy val readsDateTime: Reads[DateTime] = Reads.of[String].map {
    case str => Try(datetimeFormatter.parseDateTime(str))
  }.collect(ValidationError("error.expected.datetime.format")) {
    case Success(valid) => valid
  }
  implicit lazy val writesDateTime: Writes[DateTime] = Writes { dt =>
    JsString(datetimeFormatter.print(dt.withZone(DateTimeZone.UTC)))
  }

  implicit lazy val writesCypherValue: Writes[CypherValue] = Writes {
    case CypherArray(vs) => JsArray(vs.map(writesCypherValue.writes))
    case CypherString(v) => Json.toJson(v)
    case CypherInt(v) => Json.toJson(v)
    case CypherDouble(v) => Json.toJson(v)
    case CypherBoolean(v) => Json.toJson(v)
    case CypherLong(v) => Json.toJson(v)
    case CypherFloat(v) => Json.toJson(v)
    case CypherByte(v) => Json.toJson(new String(Array(v), "UTF-8"))
    case CypherChar(v) => Json.toJson(v.toString)
    case CypherShort(v) => Json.toJson(v)
  }
}
