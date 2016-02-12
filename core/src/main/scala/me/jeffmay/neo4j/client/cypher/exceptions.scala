package me.jeffmay.neo4j.client.cypher

/**
  * An exception thrown when there is an error constructing a [[CypherArg]] or [[CypherStatement]].
  */
sealed abstract class CypherException(message: String) extends Exception(message)

/**
  * Thrown when a [[CypherArg]] cannot be extracted from one or more [[CypherResult]]s.
  *
  * @note this will combine multiple exceptions from multiple [[CypherResultInvalid]]s using
  *       suppressed exceptions.
  *
  * @param message the message detailing why the cypher statement could not be constructed
  * @param causes the underlying issues detailing why the individual [[CypherArg]]s are invalid
  */
class InvalidCypherException(val message: String, val causes: Seq[CypherResultInvalid] = Seq.empty)
  extends CypherException({
    causes.zipWithIndex.map {
      case (error, n) => s"($n) ${error.result.message}"
    }.mkString("\n")
  }) {

  for (error <- causes) {
    this.addSuppressed(error.exception)
  }
}

/**
  * An exception thrown when a property is defined twice on the same namespace in the same template.
  *
  * @param namespace the namespace within which the [[CypherProps]] live
  * @param property the name of the property within the [[CypherProps]]
  * @param template the raw cypher template for debugging purposes
  */
class DuplicatePropertyNameException(
  val namespace: String,
  val property: String,
  val conflictingValues: Seq[CypherValue],
  val template: String
) extends CypherException(
    s"'$property' has already been defined for the props object named '$namespace' in:\n" +
    s"$template\n" +
    s"Conflicting values are: ${conflictingValues.mkString(", ")}"
  ) {
  private[this] val conflictingValueSet = conflictingValues.toSet
  assert(
    conflictingValueSet.size >= 2,
    "This exception should not be thrown when there is less than 2 distinct conflicting values.\n" +
    s"Given: $conflictingValueSet"
  )
}
