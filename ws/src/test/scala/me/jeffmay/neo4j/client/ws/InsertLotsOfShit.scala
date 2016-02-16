package me.jeffmay.neo4j.client.ws

import java.util.UUID

import jawn.AsyncParser
import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher._
import me.jeffmay.neo4j.client.ws.json.rest.RawStatementResult
import me.jeffmay.util.Namespace
import me.jeffmay.util.akka.TestGlobalAkka
import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import play.api.libs.json.JsObject
import play.api.mvc.BodyParser

import scala.concurrent.duration._
import scala.concurrent.{Future, Await, ExecutionContext}

object InsertLotsOfShit {

  def main(args: Array[String]): Unit = {
    import ExecutionContext.Implicits.global
    implicit val namespace = new Namespace("lotsoshit")

    val timeout = 20.seconds

    val clientConfig = Neo4jClientConfig.fromConfig(TestGlobal.config).copy(timeout = timeout)
    val client: Neo4jClient = new WSNeo4jClient(
      TestWSClient.TestWS,
      clientConfig,
      TestGlobalAkka.scheduler,
      global
    )

//    val createNodes = (1 to 10000).map { _ =>
//      val id = UUID.randomUUID().toString
//      CypherStatement("CREATE ({ id: \"" + id + "\" });")
//    }.foldLeft(Seq.empty[CypherStatement])(_ :+ _)
//    Await.result(client.openAndCommitTxn(createNodes), timeout)

    val startTimeMs = System.currentTimeMillis()
    val props = Cypher.params("props", Cypher.props("ns" -> namespace.value))
    val query = client.openAndCommitTxn(cypher"MATCH (n) RETURN n.id").map { r =>
      val endTimeMs = System.currentTimeMillis() - startTimeMs
      (r, endTimeMs)
    }
    val (result, totalTimeMs) = Await.result(query, timeout)
    println(s"Time: ${totalTimeMs}ms")
    println(s"Size: ${result.result.right.get.data.size}")
  }
}

class StreamingClient extends TestWSNeo4jClient() {

  import play.api.libs.iteratee.Execution.Implicits.trampoline


  def streamTxn(stmt: CypherStatement): Future[Enumerator[RawStatementResult]] = {
    val parser = AsyncParser[JsObject](AsyncParser.UnwrapArray)

//    http("/db/data/transaction/").getStream().map { case (headers, body) =>
//      new Iteratee[Array[Byte], AsyncParser[JsObject]] {
//
//      }
//      Enumeratee.heading(body)
//      Enumeratee.mapConcat.apply { (bytes: Array[Byte]) =>
//
//      }
//      val enum: Enumeratee[Array[Byte], JsObject] = ???
//      val x = body &> enum
//      x.map(_.as[RawStatementResult])
//    }
    ???
  }
}
