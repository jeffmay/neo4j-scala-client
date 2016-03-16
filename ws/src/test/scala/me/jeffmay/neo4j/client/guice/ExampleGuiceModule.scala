package me.jeffmay.neo4j.client.guice

/*
 * The following is used for documentation purposes.
 */

import java.lang.reflect.{Field, Constructor}

import com.google.inject.{MembersInjector, TypeLiteral, AbstractModule}
import com.google.inject.matcher.Matchers
import com.google.inject.spi.{TypeEncounter, TypeListener}
import com.typesafe.config.ConfigFactory
import me.jeffmay.neo4j.client.ws.WSNeo4jClient
import me.jeffmay.neo4j.client.{Neo4jClient, Neo4jClientConfig}
import org.slf4j.{LoggerFactory, Logger}

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

/*
 * End documentation
 */
