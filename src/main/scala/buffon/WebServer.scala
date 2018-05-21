package buffon

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.ActorMaterializer
import buffon.streams.elasticsearch.{IndexShoeListing, SearchShoeListings}
import com.datastax.driver.core.exceptions.InvalidQueryException
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.softwaremill.session.SessionDirectives.{invalidateSession, requiredSession, setSession}
import com.softwaremill.session.SessionOptions.{refreshable, usingHeaders}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContextExecutor


class WebServer(config: Config)(
  implicit cassandraClient: CassandraClient,
  esClient: ElasticsearchClient,
  ec: ExecutionContextExecutor,
  materializer: ActorMaterializer
) extends LazyLogging with JsonSupport with CustomDirectives {
  private val sessionConfig = SessionConfig.fromConfig() // looking for "akka.http.session.server-secret" in application.conf
  private implicit val sessionManager = new SessionManager[String](sessionConfig)
  private implicit val refreshTokenStorage = new CassandraRefreshTokenStorage(logger)

  def createRoutes: Route = handleExceptions(myExceptionHandler) {
    pathSingleSlash {
      complete(jsonHttpEntity(s"""{"body":"Shoe Dawg API V${config.getDouble("api.version")}"}"""))
    } ~
    pathPrefix("api") {
      path("login") {
        get {
          // @formatter:off
          customAuthenticateBasicAsync("", myUserPassAuthenticator) { case User(email, passwordHash) =>
            mySetSession(email) { ctx =>
              logger.info(s"Logging in $email")
              ctx.complete(HttpResponse(StatusCodes.OK))
            }
          }
          // @formatter:on
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
              if (result) {
                complete(HttpResponse(StatusCodes.Created))
              } else {
                complete(HttpResponse(StatusCodes.BadRequest, entity = jsonHttpEntity(s"""{"message":"Email already exists"}""")))
              }
            }
          }
        }
      } ~
      path("shoes") {
        post {
          entity(as[ShoeListing]) { shoe =>
            onSuccess(IndexShoeListing(shoe)) { result =>
              complete(HttpResponse(StatusCodes.Created))
            }
          }
        }
      }
    } ~
    path("search") {
      get {
        parameters('q) { q =>
          onSuccess(SearchShoeListings(q)) {
            case Right(RequestSuccess(_, _, _, result)) => complete(result.hits.hits.map(_.sourceAsString))
            case Left(RequestFailure(status, _, _, error)) => complete(HttpResponse(status = status, entity = error.reason))
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

  // Implicit SessionManager required for below
  private def mySetSession(v: String) = setSession(refreshable, usingHeaders, v)
  private def myRequiredSession = requiredSession(refreshable, usingHeaders)
  private def myInvalidateSession = invalidateSession(refreshable, usingHeaders)
}

