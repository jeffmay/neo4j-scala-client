package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.{Cypher, CypherStatement}
import me.jeffmay.util.Namespace

object FixtureQueries extends FixtureQueries
trait FixtureQueries {

  /**
    * The property keys for the nodes in the queries
    */
  object PropKeys {

    val Id = "id"
    val Namespace = "ns"
  }

  /*
   * Each query has some pre-conditions / post-conditions for the expected success result stats to work.
   */

  object CreateNode {
    def query(id: String, includeStats: Boolean = true)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"CREATE (n { ns: {props}.ns, ${PropKeys.Id}: {props}.id })",
        Map("props" -> Cypher.props(
          "id" -> id,
          "ns" -> ns.value
        )),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, nodesCreated = 1, propertiesSet = 2)
    }
  }

  object FindNode {
    val NodeName = "n"
    def query(id: String, includeStats: Boolean = false)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH ($NodeName { ns: {props}.ns, ${PropKeys.Id}: {props}.id }) RETURN $NodeName",
        Map("props" -> Cypher.props(
          "id" -> id,
          "ns" -> ns.value
        )),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty
    }
  }

  object RenameNode {
    def query(id: String, newId: String, includeStats: Boolean = true)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH (n { ns: {props}.ns, ${PropKeys.Id}: {props}.id }) SET n.${PropKeys.Id} = {new}.id",
        Map(
          "props" -> Cypher.props(
            "id" -> id,
            "ns" -> ns.value
          ),
          "new" -> Cypher.props(
            "id" -> newId
          )
        ),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, propertiesSet = 1)
    }
  }

  object AddLabel {
    def query(id: String, label: String, includeStats: Boolean = true)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH (n { ns: {props}.ns, ${PropKeys.Id}: {props}.id }) SET n ${Cypher.label(label).getOrThrow.template}",
        Map(
          "props" -> Cypher.props(
            "id" -> id,
            "ns" -> ns.value
          )
        ),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, labelsAdded = 1)
    }
  }
}
