package me.jeffmay.neo4j.client.cypher

/**
  * Thrown when a [[CypherArg]] cannot be extracted from one or more [[CypherResult]]s.
  *
  * @note this will combine multiple exceptions from multiple [[CypherResultInvalid]]s using
  *       suppressed exceptions.
  *
  * @param message the message detailing why the cypher statement could not be constructed
  * @param causes the underlying issues detailing why the individual [[CypherArg]]s are invalid
  */
class InvalidCypherException(val message: String, val causes: Seq[CypherResultInvalid] = Seq.empty) extends Exception({
  causes.zipWithIndex.map {
    case (error, n) => s"($n) ${error.result.message}"
  }.mkString("\n")
}) {
  for (error <- causes) {
    this.addSuppressed(error.exception)
  }
}
