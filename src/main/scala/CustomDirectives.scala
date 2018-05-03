import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenges}
import akka.http.scaladsl.server.directives._
import akka.http.scaladsl.util.FastFuture._

import scala.concurrent.Future


trait CustomDirectives {
  import BasicDirectives._
  import SecurityDirectives._

  //#authenticator
  /**
    * @group security
    */
  type CustomAsyncAuthenticator[T] = (String, String) ⇒ Future[Option[T]]

  /**
    * Wraps the inner route with Http Basic authentication support.
    * The given authenticator determines whether the credentials in the request are valid
    * and, if so, which user object to supply to the inner route.
    *
    * @group security
    */
  def customAuthenticateBasicAsync[T](realm: String, authenticator: CustomAsyncAuthenticator[T]): AuthenticationDirective[T] = {
    val failure = AuthenticationResult.failWithChallenge(HttpChallenges.basic(realm))
    extractExecutionContext.flatMap { implicit ec ⇒
      authenticateOrRejectWithChallenge[BasicHttpCredentials, T] {
        case Some(BasicHttpCredentials(email, password)) =>
          authenticator(email, password).fast.map {
            case Some(t) => AuthenticationResult.success(t)
            case None => failure
          }
        case None => Future(failure)
      }
    }
  }
}
