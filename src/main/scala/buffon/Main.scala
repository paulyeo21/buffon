package buffon

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.util.Try


object Main extends HttpApp with App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()

  implicit val cassandraClient = new CassandraClient(config)
  implicit val esClient = new ElasticsearchClient(config)
  cassandraClient.seed()
  esClient.seed()

  val service = new WebServer(config)

  startServer(config.getString("api.hostname"), config.getInt("api.port"), system)

  override def routes: Route = service.createRoutes

  private def cleanupResources(): Unit = {
    system.terminate()
    cassandraClient.close()
    esClient.close()
  }

  override def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = {
    cleanupResources()
  }
}
