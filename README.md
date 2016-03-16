# neo4j-scala-client

A Scala-based driver for interfacing with a stand-alone Neo4j server over HTTP and websockets

<a href='https://travis-ci.org/jeffmay/neo4j-scala-client'>
  <img src='https://travis-ci.org/jeffmay/neo4j-scala-client.svg' alt='Build Status' />
</a>
<a href='https://coveralls.io/github/jeffmay/neo4j-scala-client?branch=master'>
  <img src='https://coveralls.io/repos/jeffmay/neo4j-scala-client/badge.svg?branch=master&service=github' alt='Coverage Status' />
</a>
<table>
  <tr>
    <th>neo4j-scala-client-core</th>
    <th>neo4j-scala-client-test-util</th>
    <th>neo4j-scala-client-ws</th>
    <th>neo4j-scala-client-ws-util</th>
    <th>neo4j-scala-client-ws-test-util</th>
  </tr>
  <tr>
    <td>
      <a href='https://bintray.com/jeffmay/maven/neo4j-scala-client-core/_latestVersion'>
        <img src='https://api.bintray.com/packages/jeffmay/maven/neo4j-scala-client-core/images/download.svg'>
      </a>
    </td>
    <td>
      <a href='https://bintray.com/jeffmay/maven/neo4j-scala-client-test-util/_latestVersion'>
        <img src='https://api.bintray.com/packages/jeffmay/maven/neo4j-scala-client-test-util/images/download.svg'>
      </a>
    </td>
    <td>
      <a href='https://bintray.com/jeffmay/maven/neo4j-scala-client-ws/_latestVersion'>
        <img src='https://api.bintray.com/packages/jeffmay/maven/neo4j-scala-client-ws/images/download.svg'>
      </a>
    </td>
    <td>
      <a href='https://bintray.com/jeffmay/maven/neo4j-scala-client-ws-util/_latestVersion'>
        <img src='https://api.bintray.com/packages/jeffmay/maven/neo4j-scala-client-ws-util/images/download.svg'>
      </a>
    </td>
    <td>
      <a href='https://bintray.com/jeffmay/maven/neo4j-scala-client-ws-test-util/_latestVersion'>
        <img src='https://api.bintray.com/packages/jeffmay/maven/neo4j-scala-client-ws-test-util/images/download.svg'>
      </a>
    </td>
  </tr>
</table>

# Summary

The primary objective of this library is to provide full access to the Neo4j REST API in a non-blocking fashion,
using cypher as the primary exchange medium.

The project has a core library for cypher interpolation and a Play Json / WS implementation (`neo4j-scala-client-ws`).

# Usage

## ReST Client

The primary API is the [Neo4jClient](core/src/main/scala/me/jeffmay/neo4j/client/Neo4jClient.scala). This is used to talk to the [Neo4j REST API](http://neo4j.com/docs/stable/rest-api-transactional.html).

## Cypher

The model used to communicate with the Cypher transaction endpoint is the  [CypherStatement](core/src/main/scala/me/jeffmay/neo4j/client/cypher/CypherStatement.scala) model.
In order to make building this case class simple and easy, you can use the `cypher` string interpolator.

For all of the following examples, you will need to import the `Cypher` object:
```scala
import me.jeffmay.neo4j.client.cypher.Cypher
```

### Cypher.params

The most important component to building a `cypher""` query is the `Cypher.params` builder. This creates a namespace
for adding parameters to the query. The suggested way to use this (until I figure out a more type-safe method) is to
provide the property map up-front and reference the properties from within the query.

The follow example builds a `CypherStatement` that creates a node with the label, `:Node` and the id, `"myId"`:
```scala
val props = Cypher.params("props", Cypher.props("id" -> 1))
cypher"""
  MATCH (n :Node { id: ${props.id} }) RETURN n 
"""
```

You can alternatively use the following syntax for inline dynamic invocation:
```scala
cypher"""
  MATCH (n :Node { id: ${Cypher.params("props").id(1)} }) RETURN n
"""
```

*Note: In both cases, `props.id` is a dynamic method invocation. Be careful about spelling because a mismatch can cause runtime exceptions.*

### Cypher.ident

If you want to refer to matched patterns within the query, you can use a `Cypher.ident`. This will verify that your
identifier uses only valid characters. If an invalid character is used, then a runtime exception will be thrown
when you include the identifier within a query. You can force an early exception by calling `.getOrThrow` or matching
on the `CypherResultInvalid` case.

```scala
val n = Cypher.ident("node")
cypher"""
  MATCH ($n :Node { id: ${Cypher.params("props").id(1)} }) RETURN $n.id, $n.description
"""
```

However, since all `CypherParams` are also `CypherIdentity`s, the following style is preferred:
```scala
val n = Cypher.params("node", Cypher.props("id" -> 1))
cypher"""
  MATCH ($n :Node { id: ${n.id} }) RETURN $n.id, $n.description
"""
```

### Cypher.label

If you want to allow programatically enter a label, but want to validate that it is safe from Cypher injection
attacks, you can use a `Cypher.label`. If an invalid character is used, then a runtime exception will be thrown
when you include the identifier within a query. You can force an early exception by calling `.getOrThrow` or matching
on the `CypherResultInvalid` case.

```scala
val n = Cypher.params("node", Cypher.props("id" -> 1))
cypher"""
  MATCH ($n ${Cypher.label("Node")} { id: ${n.id} }) RETURN $n
"""
```

### Cypher.unsafe

For when you need work around some issue, you know what you are doing, and are not worried about a Cypher injection attack.

**NOT IMPLEMENTED YET**

### Cypher.toProps

If you want to avoid repeating the properties of a node, you can define an implicit `CypherWrites.Props`, which will
enable you to call `Cypher.toProps` on your object instead of enumerating all the fields in the `Cypher.params`.

For example, let's define a case class called `Node` and define a property serializer:
```scala
import play.api.libs.json.Json
import me.jeffmay.neo4j.client.cypher.CypherWrites

case class Node(id: String, description: String)
object Node {
  implicit val format: Format[Node] = Json.format[Node]
  implicit val propsWriter: CypherWrites.Props = CypherWrites.a[Node].asProps { n =>
    Cypher.props(
      "id" -> n.id,
      "description" -> n.description
    )
  }
}
```

Now you can use this in a `Cypher.params` like so:
```scala
val node = Node(1, "node #1")
val n = Cypher.params("node", Cypher.toProps(node))
cypher"""
  MATCH ($n ${Cypher.label("Node")} { id: ${n.id} }) --> (o) RETURN o.id
"""
```

## Play Json and WS Implementation 

The standard implementation (and currently the only implementation) uses Play Json and Play WS. The class you will
likely need to use is the `Neo4jWSClient`. Everything it needs is injected directly into the constructor. 

To instantiate the client in Play <2.3.x:
```scala
import me.jeffmay.neo4j.client.ws.WSNeo4jClient
import org.slf4j.LoggerFactory
import play.api.Application.current
import play.api.libs.ws.WS
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution

object App {

  // Assuming you have some running and configured Play application
  val neo4jClient = new WSNeo4jClient(
    WS.client,
    Neo4jClientConfig("http://localhost:7474"),
    LoggerFactory.getLogger(classOf[WSNeo4jClient]),
    Akka.system.scheduler,
    Execution.defaultContext
  )
}
```

In Play > 2.4.x with Guice:
```scala
import java.lang.reflect.Field

import com.google.inject._
import com.google.inject.matcher.Matchers
import com.google.inject.spi.{TypeEncounter, TypeListener}
import com.typesafe.config.ConfigFactory
import me.jeffmay.neo4j.client.ws.WSNeo4jClient
import me.jeffmay.neo4j.client.{Neo4jClient, Neo4jClientConfig}
import org.slf4j.{Logger, LoggerFactory}

/**
  * @see https://www.playframework.com/documentation/2.4.x/JavaDependencyInjection#Programmatic-bindings
  *      for more information on how to wire this binding in your application.conf
  */
class Neo4jClientModule extends AbstractModule {
  override protected def configure(): Unit = {
    bindListener(Matchers.any(), new Log4JTypeListener())
    // The following binding is required as the listener only replaces the logger after the binding is found
    bind(classOf[Logger]).toInstance(LoggerFactory.getLogger("Some global logger"))
    bind(classOf[Neo4jClientConfig]).toInstance(Neo4jClientConfig.fromConfig(ConfigFactory.load()))
    // Inject arguments into the primary constructor
    // Note: The ugly .asInstanceOf is because Java's unsafe Array could be mutated to have other constructor types
    // Note: Constructor argument types not listed above should be available by Play's Guice modules by default
    bind(classOf[Neo4jClient])
      .toConstructor(classOf[WSNeo4jClient].getConstructors()(0).asInstanceOf[Constructor[WSNeo4jClient]])
  }
}

/**
  * @see https://github.com/google/guice/wiki/CustomInjections
  *      for more information on injecting loggers with the class name
  */
class Log4JTypeListener extends TypeListener {
  override def hear[I](`type`: TypeLiteral[I], encounter: TypeEncounter[I]): Unit = {
    var clazz = `type`.getRawType
    while (clazz != null) {
      for (field <- clazz.getDeclaredFields) {
        if (field.getType == classOf[Logger]) {
          encounter.register(new Log4JMembersInjector[I](field))
        }
      }
      clazz = clazz.getSuperclass
    }
  }
}

/**
  * Replaces the default binding for Logger with a Logger by class name.
  */
class Log4JMembersInjector[T](field: Field) extends MembersInjector[T] {

  private val logger: Logger = {
    val classNameLogger = LoggerFactory.getLogger(field.getDeclaringClass)
    field.setAccessible(true)
    classNameLogger
  }

  override def injectMembers(instance: T): Unit = {
    try {
      field.set(instance, logger)
    } catch {
      case e: IllegalAccessException => throw new RuntimeException(e);
    }
  }
}
```

The following example is how you might want to use the client to find a node in the graph:
```scala
val n = Cypher.params("node", Cypher.toProps(node)).getOrThrow
val stmt = cypher"""
  MATCH ($n ${Cypher.label("Node")} { id: ${n.id} }) RETURN $n
"""
val findNodeIdAndDesc: Future[Option[Node]] = neo4jClient.openAndCommitTxn(stmt).flatMap { resp =>
  resp.result match {
    case Right(result) =>
      val foundNode = result.rows.headOption.map { row =>
        row.col(n.namespace).as[Node]  // can throw a JsResultException
      }
      Future.successful(foundNode)
    case Left(errors) =>
      Future.failed()
  }
}
```
