package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client.cypher._
import me.jeffmay.neo4j.client._
import play.api.libs.json.{JsArray, Reads, Json, Writes}

package object json {

  implicit lazy val readsNeo4jError: Reads[Neo4jError] = Json.reads[Neo4jError]

  implicit lazy val writesTxnInfo: Writes[TxnInfo] = Json.writes[TxnInfo]

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

  implicit lazy val writesCypherStatement: Writes[CypherStatement] = Json.writes[CypherStatement]

  implicit val writesTxnResponse: Writes[TxnResponse] = Writes {
    case open: OpenedTxnResponse => writesOpenedTxnResponse.writes(open)
    case closed: CommittedTxnResponse => writesCommittedTxnResponse.writes(closed)
  }

  implicit lazy val writesOpenedTxnResponse: Writes[OpenedTxnResponse] = Writes {
    case single: SingleOpenedTxnResponse => writesSingleOpenedTxnResponse.writes(single)
    case generic: GenericOpenedTxnResponse => writesGenericOpenedTxnResponse.writes(generic)
  }
  implicit lazy val writesSingleOpenedTxnResponse: Writes[SingleOpenedTxnResponse] = Json.writes[SingleOpenedTxnResponse]
  implicit lazy val writesGenericOpenedTxnResponse: Writes[GenericOpenedTxnResponse] = Json.writes[GenericOpenedTxnResponse]

  implicit lazy val writesCommittedTxnResponse: Writes[CommittedTxnResponse] = Writes {
    case single: SingleCommittedTxnResponse => writesSingleCommittedTxnResponse.writes(single)
    case generic: GenericCommittedTxnResponse => writesGenericCommittedTxnResponse.writes(generic)
  }
  implicit lazy val writesSingleCommittedTxnResponse: Writes[SingleCommittedTxnResponse] = Json.writes[SingleCommittedTxnResponse]
  implicit lazy val writesGenericCommittedTxnResponse: Writes[GenericCommittedTxnResponse] = Json.writes[GenericCommittedTxnResponse]

}
