package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show
import me.jeffmay.neo4j.client.cypher.Cypher.ImmutableParam

import scala.language.implicitConversions
import scala.util.Try

/**
  * A container for the success or failure to build a Cypher argument.
  *
  * This avoids throwing an exception on the construction of a [[CypherArg]], but captures the exception
  * at the point it occurs so that when you attempt to use the argument in the interpolated Cypher query,
  * you see an exception with a stack trace that points to the where the failure happened.
  */
sealed trait CypherResult[+T <: CypherArg] {

  def isValid: Boolean
  @inline final def isInvalid: Boolean = !isValid

  def getOrThrow: T
  @inline final def tryGet: Try[T] = Try(getOrThrow)
}
object CypherResult {

  /**
    * All [[CypherArg]]s are valid [[CypherArg]]s.
    */
  implicit def valid[T <: CypherArg](arg: T): CypherResultValid[T] = CypherResultValid(arg)

  /**
    * All [[ImmutableParam]]s are valid [[CypherArg]]s.
    */
  implicit def immutableParams(arg: ImmutableParam): CypherResultValid[CypherParamObject] = {
    CypherResultValid(new CypherParamObject(arg.namespace, arg.props))
  }
}

/**
  * Contains a valid [[CypherArg]] that can be used
 *
  * @param result the valid result.
  */
case class CypherResultValid[+T <: CypherArg](result: T) extends CypherResult[T] {
  final override def isValid: Boolean = true
  final override def getOrThrow: T = result
}

// TODO: Use compile-time exception information?
/**
  * A [[CypherResult]] that did not meet the pre-conditions.
  *
  * @param result The invalid result
  */
case class CypherResultInvalid(result: InvalidCypherArg) extends CypherResult[Nothing] {
  // Construct the exception on instantiation to insure that the stack-trace captures where the failure occurs.
  val exception: Throwable = new InvalidCypherException(result.message, result.template)
  final override def isValid: Boolean = false
  final override def getOrThrow: Nothing = throw exception
}

/**
  * The base trait for error messages related to building a [[CypherArg]].
  */
sealed trait InvalidCypherArg {
  def message: String
  def template: Option[String]
}

/**
  * The cypher argument requested is not available on the given props.
  */
case class MissingCypherProperty(props: CypherProps, name: String)(implicit showProps: Show[CypherProps])
  extends InvalidCypherArg {

  require(!(props contains name), s"${showProps show props} does contain the key '$name'")

  final override def template: Option[String] = None

  override lazy val message: String = {
    s"The property name '$name' cannot be found in the props: ${showProps show props}"
  }
}

/**
  * The base trait for error messages dealing with an invalid format when building the Cypher query string.
  */
sealed trait InvalidCypherArgFormat extends InvalidCypherArg {
  def literal: String
  override def template: Option[String] = Some(literal)
}

/**
  * Could not construct a [[CypherIdentifier]] because the provided label name did not match the required format.
  *
  * @param literal the invalid identifier name used
  */
case class CypherIdentifierInvalidFormat(literal: String) extends InvalidCypherArgFormat {
  override lazy val message: String = s"Invalid identifier format '$literal'. Must match /${CypherIdentifier.Valid.pattern.pattern}/"
}

/**
  * Could not construct a [[CypherLabel]] because the provided label name did not match the required format.
  *
  * @param literal the invalid label name used
  */
case class CypherLabelInvalidFormat(literal: String) extends InvalidCypherArgFormat {
  override lazy val message: String = s"Invalid label format '$literal'. Must match /${CypherLabel.Valid.pattern.pattern}/"
}

/**
  * A valid argument to pass into the [[CypherStringContext]] used to insert some literal string or parameter(s)
  * into a Cypher [[CypherStatement]].
  */
sealed trait CypherArg {

  /**
    * The string of Cypher text sent to the server as the "statement" argument in the
    * <a href="http://neo4j.com/docs/stable/rest-api-transactional.html">Transaction Endpoint</a>
    *
    * @note This may still contain placeholders (surrounded by {}) that are substituted on the
    *       Neo4j server to avoid Cypher injection attacks.
    */
  def template: String
}

/**
  * A literal string to insert into the Cypher string.
  *
  * @param template the literal value to insert into the [[CypherStatement.template]].
  */
sealed abstract class CypherTemplatePart(override val template: String) extends CypherArg with Proxy {
  override def self: Any = template
}

/**
  * An identifier to refer to nodes, relationships, or paths in a pattern.
  *
  * @see <a href="http://neo4j.com/docs/stable/cypher-identifiers.html">Cypher Identifiers</a>
  * @param name the name of the identifier
  */
final class CypherIdentifier private (name: String) extends CypherTemplatePart(name)
object CypherIdentifier {

  val Valid = "^[a-zA-Z][a-zA-Z0-9_]*$".r

  def isValid(literal: String): Boolean = {
    Valid.findFirstMatchIn(literal).isDefined
  }

  def apply(literal: String): CypherResult[CypherIdentifier] = {
    if (isValid(literal)) {
      CypherResultValid(new CypherIdentifier(literal))
    } else {
      CypherResultInvalid(CypherIdentifierInvalidFormat(literal))
    }
  }
}

/**
  * A label to add to either a node or relationship.
  *
  * @see <a href="http://neo4j.com/docs/stable/graphdb-neo4j.html#graphdb-neo4j-labels">Label Documentation</a>
  * @param name the name of the label (without the preceding colon ':')
  */
final class CypherLabel private (name: String) extends CypherTemplatePart(s":$name")
object CypherLabel {

  val Valid = "^[a-zA-Z0-9_]+$".r

  private[this] var validated: Map[String, CypherLabel] = Map.empty

  def isValid(label: String): Boolean = {
    Valid.findFirstMatchIn(label).isDefined
  }

  def apply(label: String): CypherResult[CypherLabel] = {
    validated.get(label).map(CypherResultValid(_)) getOrElse {
      if (isValid(label)) {
        val valid = new CypherLabel(label)
        validated += label -> valid
        CypherResultValid(valid)
      } else {
        CypherResultInvalid(CypherLabelInvalidFormat(label))
      }
    }
  }
}

/**
  * Holds a single parameter field within one of the [[CypherStatement.parameters]] namespaces.
  *
  * @note This is not constructed directly. Rather, you use the [[Cypher.params]] methods to build this.
  * @param namespace the key of the [[CypherProps]] within which field names are unique
  * @param id the field name within the namespace
  * @param value the value of the parameter object's field
  */
case class CypherParamField private[cypher] (namespace: String, id: String, value: CypherValue) extends CypherArg {
  override val template: String = s"{$namespace}.$id"
}

/**
  * Holds a single parameter object as one of the [[CypherStatement.parameters]] namespaces.
  *
  * @note This is not constructed directly. Rather, you use the [[Cypher.params]] methods to build this.
  * @param namespace the key of the [[CypherProps]] within which field names are unique
  * @param props the map of fields to unfold as properties in place
  */
case class CypherParamObject private[cypher] (namespace: String, props: CypherProps) extends CypherArg {
  override val template: String = s"{ $namespace }"
}
