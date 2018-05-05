import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.datastax.driver.core.exceptions.InvalidQueryException
import com.sksamuel.elastic4s.http.HttpClient
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.softwaremill.session.SessionDirectives.{invalidateSession, requiredSession, setSession}
import com.softwaremill.session.SessionOptions.{refreshable, usingHeaders}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContext


class WebServer(config: Config)(implicit cassandraClient: CassandraClient, esClient: HttpClient, materializer: ActorMaterializer) extends LazyLogging with JsonSupport with CustomDirectives {
  private val sessionConfig = SessionConfig.fromConfig() // looking for "akka.http.session.server-secret" in application.conf
  private implicit val executor = ExecutionContext.global
  private implicit val sessionManager = new SessionManager[String](sessionConfig)
  private implicit val refreshTokenStorage = new CassandraRefreshTokenStorage(logger)

  def createRoutes: Route = handleExceptions(myExceptionHandler) {
    pathSingleSlash {
      complete(jsonHttpEntity(s"""{"body":"Shoe Dawg API V${config.getDouble("api.version")}"}"""))
    } ~
      pathPrefix("api") {
        path("login") {
          get {
            // Note: experiencing weird Intellij error highlighting
            customAuthenticateBasicAsync("", myUserPassAuthenticator) { case User(email, passwordHash) =>
              mySetSession(email) { ctx =>
                logger.info(s"Logging in $email")
                ctx.complete(HttpResponse(StatusCodes.OK))
              }
            }
          }
        } ~
        path("logout") {
          post {
            myRequiredSession { session =>
              myInvalidateSession { ctx =>
                logger.info(s"Logging out $session")
                ctx.complete(HttpResponse(StatusCodes.OK))
              }
            }
          }
        } ~
        path("current_login") {
          get {
            myRequiredSession { session =>
              ctx =>
                logger.info(s"Current session: $session")
                ctx.complete(HttpResponse(StatusCodes.OK))
            }
          }
        } ~
        path("users") {
          post {
            entity(as[User]) { user =>
              onSuccess(cassandraClient.insertUser(user.email, user.password)) { result =>
                complete(HttpResponse(StatusCodes.Created))
              }
            }
          }
        } ~
        path("shoes") {
          post {
            entity(as[Shoe]) { shoe =>
              onSuccess(createShoeDocument(shoe)) { result =>
                complete(HttpResponse(StatusCodes.Created))
              }
            }
          }
        }
      }
  }

  private def jsonHttpEntity(s: String) = HttpEntity(ContentTypes.`application/json`, s)

  private def myExceptionHandler = ExceptionHandler {
    case _: InvalidQueryException =>
      extractUri { uri =>
        logger.error(s"Request to $uri could not be handled normally")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
  }

  private def myUserPassAuthenticator(email: String, password: String)(implicit cassandraClient: CassandraClient) = for {
    userF <- cassandraClient.selectUser(email, "email")
  } yield for {
    user <- userF
    if BCrypt.checkpw(password, user.password)
  } yield {
    user
  }

  private def createShoeDocument(shoe: Shoe)(implicit esClient: HttpClient, materializer: ActorMaterializer) = {
    import com.sksamuel.elastic4s.http.ElasticDsl._

    val source = Source.single(shoe)
    val flow =
      Flow[Shoe].mapAsyncUnordered(parallelism = 1) { s =>
        esClient.execute {
          indexInto("shoe" / "type1").fields("name" -> s.name)
        }
      }
    val sink = Sink.ignore
    val r = source.via(flow).toMat(sink)(Keep.right)
    r.run()

//    val g = RunnableGraph.fromGraph(GraphDSL.create() {
//      implicit builder =>
//        import GraphDSL.Implicits._
//
//        val A: Outlet[Shoe] = builder.add(Source.single(shoe)).out
//        val B = builder.add(createShoeDocument)
//        val C = builder.add(Sink.ignore).in
//
//        A ~> B ~> C
//        ClosedShape
//    })
//    g.run()
  }

  // Implicit SessionManager required for below
  private def mySetSession(v: String) = setSession(refreshable, usingHeaders, v)
  private def myRequiredSession = requiredSession(refreshable, usingHeaders)
  private def myInvalidateSession = invalidateSession(refreshable, usingHeaders)
}

