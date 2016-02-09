package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.{Cypher, Statement}
import me.jeffmay.util.UniquePerClassNamespace
import me.jeffmay.util.akka.TestGlobalAkka
import org.scalatest._

import scala.concurrent.{ExecutionContext, Future}

class WSNeo4jClientBasicSpecs extends fixture.AsyncWordSpec
  with Safety
  with AssertResultStats
  with UniquePerClassNamespace {

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  class FixtureParam extends UniqueNamespace with FixtureQueries {
    val client: Neo4jClient = new WSNeo4jClient(
      TestWSClient.TestWS,
      Neo4jClientConfig.fromConfig(TestGlobal.config),
      TestGlobalAkka.scheduler,
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
        val getAllNodes = Statement(
          "MATCH (n { ns: {props}.ns }) RETURN n",
          Map("props" -> Cypher.props("ns" -> namespace.value))
        )
        for {
          rsp <- client.openAndCommitTxn(getAllNodes)
        } yield {
          assert(rsp.errors.isEmpty, "should not encounter errors")
          assertResult(Seq(), "namespace should be empty") {
            rsp.results.head.data
          }
        }
      }

      "add a single node to the graph" in { fixture =>
        import fixture._
        for {
          rsp <- client.openAndCommitTxn(CreateNode.query("singleWithoutStats", includeStats = false))
        } yield {
          assert(rsp.errors.isEmpty, "should not encounter errors")
        }
      }

      "return the correct stats for a single create statement" in { fixture =>
        import fixture._
        for {
          rsp <- client.openAndCommitTxn(CreateNode.query("singleWithStats"))
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
        )
        val expectedResultStats = Seq(
          CreateNode.successResultStats,
          RenameNode.successResultStats,
          AddLabel.successResultStats
        )
        for {
          rsp <- client.openAndCommitTxn(queries.head, queries.tail: _*)
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

    "calling openTxn" should {

      "add a single node to the graph" in { fixture =>
        import fixture._
        for {
          openRsp <- client.openTxn(CreateNode.query("singleWithoutStats", includeStats = false))
          commitRsp <- client.commitTxn(openRsp.transaction.ref)
        } yield {
          assert(openRsp.results.isEmpty, "should not have any results yet")
          assert(openRsp.errors.isEmpty, "should not encounter any errors yet")
          assert(commitRsp.errors.isEmpty, "should not encounter any errors")
          assert(commitRsp.results.nonEmpty, "should not encounter any errors")
        }
      }

      "return the correct stats for a single create statement" in { fixture =>
        import fixture._
        for {
          openRsp <- client.openTxn(CreateNode.query("singleWithStats"))
          rsp <- client.openTxn(CreateNode.query("singleWithStats"))
        } yield {
          assertResult(1, "should have the correct stats included") {
            rsp.results.head.stats.get.nodesCreated
          }
        }
      }

      "return the correct stats for sequence of statements executed on open and commit" in { fixture =>
        import fixture._
        val queries = Seq(
          CreateNode.query("a"),
          RenameNode.query("a", "b"),
          AddLabel.query("b", "B")
        )
        val expectedResultStats = Seq(
          CreateNode.successResultStats,
          RenameNode.successResultStats,
          AddLabel.successResultStats
        )
        for {
          openRsp <- client.openTxn(queries.head)
        } yield {
          assert(openRsp.results.size === expectedResultStats.size)
          val tests = (expectedResultStats zip openRsp.results).zipWithIndex
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

  }
}
