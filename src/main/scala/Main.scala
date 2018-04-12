import akka.actor.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory


object Main extends HttpApp with App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val service = new WebServer(config)
  def routes: Route = service.createRoutes

  startServer(config.getString("api.hostname"), config.getInt("api.port"))
}

