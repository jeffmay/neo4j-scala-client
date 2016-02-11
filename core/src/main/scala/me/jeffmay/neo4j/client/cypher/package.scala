package me.jeffmay.neo4j.client

/**
  * This is meant to be imported as [[me.jeffmay.neo4j.client.cypher]] and then all
  * classes in this package are prefixed.
  */
package object cypher extends CypherInterpolation {

  type CypherProps = Map[String, CypherValue]

  type CypherParams = Map[String, CypherProps]

  @deprecated("Use CypherStatement instead", "0.4.0")
  type Statement = CypherStatement
  @deprecated("Use CypherStatement instead", "0.4.0")
  val Statement = CypherStatement
}

