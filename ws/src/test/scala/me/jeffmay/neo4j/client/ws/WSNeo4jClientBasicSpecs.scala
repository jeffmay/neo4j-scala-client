package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.{Cypher, CypherStatement}
import me.jeffmay.util.UniquePerClassNamespace
import me.jeffmay.util.akka.TestGlobalAkka
import me.jeffmay.util.ws.ProxyWSRequest
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar.mock
import play.api.libs.ws.{WSRequest, WSClient}

import scala.concurrent.{ExecutionContext, Future}

class WSNeo4jClientBasicSpecs extends fixture.AsyncWordSpec
  with Safety
  with AssertResultStats
  with UniquePerClassNamespace {

  import ws.json.debug._

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  class FixtureParam extends UniqueNamespace with FixtureQueries {

    val clientConfig = Neo4jClientConfig.fromConfig(TestGlobal.config)

    def scheduler = TestGlobalAkka.scheduler

    val client: Neo4jClient = new WSNeo4jClient(
      TestWSClient.TestWS,
      clientConfig,
      scheduler,
      executionContext
    )
  }

  override def withAsyncFixture(test: OneArgAsyncTest): Future[Outcome] = {
    val fixture = new FixtureParam
    val futureOutcome = test(fixture)
    Neo4jTestNamespace.cleanupAfter(futureOutcome, fixture.namespace, fixture.client)
  }

  "WSNeo4jClient" when {

    "providing authorization" should {

      "not require a password change on boot" in { fixture =>
        import fixture._
        for {
          passwordChangeRequired <- client.passwordChangeRequired()
        } yield {
          assertResult(false, "password change should not be required after initializing") {
            passwordChangeRequired
          }
        }
      }
    }

    "calling openAndCloseTxn" should {

      "start with an empty unique namespace" in { fixture =>
        import fixture._
        val getAllNodes = CypherStatement(
          "MATCH (n { ns: {props}.ns }) RETURN n",
          Map("props" -> Cypher.props("ns" -> namespace.value))
        )
        for {
          rsp <- client.openAndCommitTxn(getAllNodes)
        } yield {
          assertResult(true, "should not encounter errors") {
            rsp.errors.isEmpty
          }
          assertResult(Seq(), "namespace should be empty") {
            rsp.results.head.data
          }
        }
      }

      "add a single node to the graph" in { fixture =>
        import fixture._
        for {
          rsp <- client.openAndCommitTxn(CreateNode.query("singleWithoutStats"))
        } yield {
          assertResult(true, "should not encounter errors") {
            rsp.errors.isEmpty
          }
        }
      }

      "return the correct stats for a single create statement" in { fixture =>
        import fixture._
        for {
          rsp <- client.openAndCommitTxn(CreateNode.query("singleWithStats").withStats)
        } yield {
          assertResult(1, "should have the correct stats included") {
            rsp.results.head.stats.get.nodesCreated
          }
        }
      }

      "return the correct stats for sequence of statements" in { fixture =>
        import fixture._
        val queries = Seq(
          CreateNode.query("a"),
          RenameNode.query("a", "b"),
          AddLabel.query("b", "B")
        ).map(_.withStats)
        val expectedResultStats = Seq(
          CreateNode.successResultStats,
          RenameNode.successResultStats,
          AddLabel.successResultStats
        )
        for {
          rsp <- client.openAndCommitTxn(queries)
        } yield {
          assert(rsp.results.size === expectedResultStats.size)
          val tests = (expectedResultStats zip rsp.results).zipWithIndex
          for (((expected, actual), n) <- tests) {
            assertResultStats(
              expected,
              s"stats for query #${n + 1} '${queries(n).template}' did not have the expected stats") {
              actual.stats.get
            }
          }
          Succeeded
        }
      }
    }

    "calling openTxn followed by commitTxn" should {

      "not affect queries until committed" in { fixture =>
        import fixture._
        val nodeId = "uncommittedNode"
        val findQuery = FindNode.query(nodeId)
        for {
          openTxnRsp <- client.openTxn(CreateNode.query(nodeId).withStats)
          otherTxnRsp <- client.openAndCommitTxn(findQuery)
          commitTxnRsp <- client.commitTxn(openTxnRsp.transaction.txn, Seq(findQuery))
        } yield {
          assertResult(CreateNode.successResultStats, "open transaction updates should complete successfully") {
            openTxnRsp.result.right.get.stats.get
          }
          assertResult(Seq(), "expected 0 results before committing the transaction") {
            otherTxnRsp.result.right.get.data
          }
          assertResult(1, "expected 1 results after committing the create node transaction") {
            commitTxnRsp.results.head.rows.size
          }
        }
      }

//      "use other baseUrl when transaction committed on other host" in { fixture =>
//        import fixture._
//        val fakeIP = 32125
//        val fakeTxn = TxnRef.parse(s"http://127.0.0.1:$fakeIP/db/data/transaction/1/commit")
//        val mockWS = mock[WSClient]
//        val fakeClient = new WSNeo4jClient(mockWS, clientConfig, scheduler, executionContext)
//        val mockRequest = mock[WSRequest]
//        val proxyRequest = new ProxyWSRequest(mockRequest, _ => mockRequest)
//
//        when(mockWS.url(any())).thenReturn(proxyRequest)
//
//        fakeClient.commitTxn(fakeTxn).failed.map {
//          case ex =>
//            verify(mockWS, times(1)).url(fakeTxn.url)
//            Succeeded
//        }
//      }
    }

  }
}
