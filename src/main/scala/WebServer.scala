import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.softwaremill.session.SessionDirectives.{invalidateSession, requiredSession, setSession}
import com.softwaremill.session.SessionOptions.{oneOff, usingHeaders}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging


final class WebServer(config: Config) extends LazyLogging {
  private val sessionConfig = SessionConfig.fromConfig() // looking for "akka.http.session.server-secret" in application.conf
  private implicit val sessionManager = new SessionManager[String](sessionConfig)

  def createRoutes: Route = {
    pathSingleSlash {
      complete(jsonHttpEntity(s"""{"body":"Shoe Dawg API V${config.getDouble("api.version")}"}"""))
    } ~
      pathPrefix("api") {
        path("login") {
          post {
            entity(as[String]) { body =>
              logger.info(s"Logging in $body")
              mySetSession(body) { ctx =>
                ctx.complete("ok")
              }
            }
          }
        } ~
        path("logout") {
          post {
            myRequiredSession { session =>
              myInvalidateSession { ctx =>
                logger.info(s"Logging out $session")
                ctx.complete("ok")
              }
            }
          }
        } ~
        path("current_login") {
          get {
            myRequiredSession { session =>
              ctx =>
                logger.info(s"Current session: $session")
                ctx.complete("ok")
            }
          }
        }
      }

    //    path("shoe") {
    //      get {
    //        complete(Shoe("Air Force 1"))
    //      }
    //    } ~
    //    path("shoes") {
    //      post {
    //        entity(as[Shoe]) { shoe =>
    //          complete(s"shoe: ${shoe.name}")
    //        }
    //      }
    //    }
  }

  private def jsonHttpEntity(s: String) = HttpEntity(ContentTypes.`application/json`, s)

  // Implicit SessionManager required for below
  private def mySetSession(v: String) = setSession(oneOff, usingHeaders, v)
  private def myRequiredSession = requiredSession(oneOff, usingHeaders)
  private def myInvalidateSession = invalidateSession(oneOff, usingHeaders)
}

