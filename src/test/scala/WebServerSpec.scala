import org.scalatest.{FlatSpec, Matchers}
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest


class WebServerSpec extends FlatSpec with Matchers with ScalatestRouteTest with SessionTestUtils {
  private val app = new WebServer(config)
  private val routes = app.createRoutes


  behavior of "pathSingleSlash"

  it should "return json response" in {
   Get() ~> routes ~> check {
     status shouldBe OK
     contentType shouldBe `application/json`
   }
  }


  behavior of "login"

  it should "respond with 200" in {
    Post("/api/login") ~> routes ~> check {
      status shouldBe OK
      SessionTestUtils.getSessionToken shouldBe 'defined
      SessionTestUtils.sessionIsExpired shouldBe false
    }
  }


  behavior of "logout"

  it should "respond with bad request with no session token" in {
    Post("/api/logout") ~> routes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }
  }

  it should "get session token and respond with 200" in  {
    Post("/api/login") ~> routes ~> check {
      val Some(sessionToken) = SessionTestUtils.getSessionToken

      Post("/api/logout") ~> addHeader(SessionTestUtils.setSessionHeader(sessionToken)) ~> routes ~> check {
        status shouldBe OK
        SessionTestUtils.sessionIsExpired shouldBe true
      }
    }
  }

  it should "use tampered session and respond with bad request" in {
    Post("/api/login") ~> routes ~> check {
      val Some(sessionToken) = SessionTestUtils.getSessionToken
      val tamperedSession = sessionToken + "tampered"

      Post("/api/logout") ~> addHeader(SessionTestUtils.setSessionHeader(tamperedSession)) ~> routes ~> check {
        rejection shouldBe AuthorizationFailedRejection
      }
    }
  }


  behavior of "current_login"

  it should "respond with bad request with no session token" in {
    Get("/api/current_login") ~> routes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }
  }

  it should "get session token and respond with 200" in  {
    Post("/api/login") ~> routes ~> check {
      val Some(sessionToken) = SessionTestUtils.getSessionToken

      Get("/api/current_login") ~> addHeader(SessionTestUtils.setSessionHeader(sessionToken)) ~> routes ~> check {
        status shouldBe OK
        SessionTestUtils.sessionIsExpired shouldBe false
      }
    }
  }

  it should "use tampered session and respond with bad request" in {
    Post("/api/login") ~> routes ~> check {
      val Some(sessionToken) = SessionTestUtils.getSessionToken
      val tamperedSession = sessionToken + "tampered"

      Get("/api/current_login") ~> addHeader(SessionTestUtils.setSessionHeader(tamperedSession)) ~> routes ~> check {
        rejection shouldBe AuthorizationFailedRejection
      }
    }
  }
}
