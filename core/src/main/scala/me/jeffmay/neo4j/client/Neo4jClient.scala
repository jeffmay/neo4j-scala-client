package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.CypherStatement

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait Neo4jClient {

  def passwordChangeRequired(): Future[Boolean]

  def changePassword(newPassword: String): Future[Unit]

  // TODO: Use a single statement return type
  def openAndCommitTxn(first: CypherStatement, others: CypherStatement*): Future[CommittedTxnResponse]

  // TODO: Use a single statement return type
  def openTxn(statements: CypherStatement*): Future[OpenedTxnResponse]

  // TODO: Use a single statement return type
  def commitTxn(ref: TxnRef, alongWith: CypherStatement*): Future[CommittedTxnResponse]

  def withStatsIncludedByDefault(includeStatsByDefault: Boolean): Neo4jClient

  def withBaseUrl(baseUrl: String): Neo4jClient

  def withCredentials(username: String, password: String): Neo4jClient

  def withTimeout(timeout: FiniteDuration): Neo4jClient
}
