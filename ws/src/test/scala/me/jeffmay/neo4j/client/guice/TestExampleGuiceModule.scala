package me.jeffmay.neo4j.client.guice

import akka.actor.Scheduler
import com.google.inject._
import com.google.inject.matcher.Matchers
import com.google.inject.util.Modules
import me.jeffmay.neo4j.client.ws.WSNeo4jClient
import me.jeffmay.neo4j.client.{Neo4jClient, Neo4jClientConfig}
import org.mockito.Mockito
import org.scalatest.WordSpec
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

/**
  * Tests that the ExampleGuiceModule.scala file works as documented.
  */
class TestExampleGuiceModule extends WordSpec {

  "Example Neo4jClientModule" should {

    "create injector with a Logger of the same class name" in {
      val injector = Guice.createInjector(new TestLoggerInjectionModule)
      val test = injector.getInstance(classOf[TestLoggerInjection])
      assertResult(classOf[TestLoggerInjection].getName) {
        test.logger.getName
      }
    }

    "be able to create an injector that is able to find an instance of Neo4jClient" in {
      val expectedConfig = Neo4jClientConfig("example")
      val module = Modules.`override`(new Neo4jClientModule).`with`(new TestPlayBindingsAndOverrides(expectedConfig))
      val injector = Guice.createInjector(module)
      val test = injector.getInstance(classOf[Neo4jClient])
      test match {
        case wsClient: WSNeo4jClient =>
          assertResult(expectedConfig) {
            wsClient.config
          }
        case other =>
          fail(s"wrong client returned: expected ${classOf[WSNeo4jClient].getName} found ${other.getClass.getName}")
      }
    }
  }
}

/**
  * Tests class name logger injection.
  */
class TestLoggerInjection @Inject() (val logger: Logger)
class TestLoggerInjectionModule extends AbstractModule {
  override def configure(): Unit = {
    bindListener(Matchers.any(), new Log4JTypeListener())
    bind(classOf[Logger]).toInstance(LoggerFactory.getLogger("Fallback logger required"))
    bind(classOf[TestLoggerInjection])
  }
}

/**
  * Tests that the documentation for using dependency injection with [[WSNeo4jClient]] is accurate
  * by providing the configs that .
  */
class TestPlayBindingsAndOverrides(config: Neo4jClientConfig) extends AbstractModule {
  override protected def configure(): Unit = {
    // These bindings should be provided by Play automatically
    bind(classOf[WSClient]).toInstance(Mockito.mock(classOf[WSClient]))
    bind(classOf[ExecutionContext]).toInstance(ExecutionContext.global)
    bind(classOf[Scheduler]).toInstance(Mockito.mock(classOf[Scheduler]))
    // Override this for testing purposes
    bind(classOf[Neo4jClientConfig]).toInstance(config)
  }
}
