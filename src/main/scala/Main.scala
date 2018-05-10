import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.ConfigFactory

import scala.io.StdIn


object Main extends App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val config = ConfigFactory.load()

  implicit val cassandraClient = new CassandraClient(config)
  cassandraClient.seed()
  implicit val esClient = HttpClient(ElasticsearchClientUri(config.getString("elasticsearch.hostname"), config.getInt("elasticsearch.port")))

  val service = new WebServer(config)
  val bindingFuture = Http().bindAndHandle(service.createRoutes, config.getString("api.hostname"), config.getInt("api.port"))

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
  cassandraClient.close()
  esClient.close()
}
