import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest


class WebServerSpec extends FlatSpec
  with Matchers with ScalatestRouteTest with SessionTestUtils with JsonSupport with BeforeAndAfter with BeforeAndAfterAll {
  private implicit val cassandraClient = new CassandraClient(config)
  private val app = new WebServer(config)
  private val routes = app.createRoutes

  val email = "email"
  val password = "password"
  val user = User(email, password)
  val validCredentials = BasicHttpCredentials(email, password)

  override def beforeAll {
    cassandraClient.cleanUp(yes = true)
    cassandraClient.seed()
  }

  override def afterAll {
    cassandraClient.close()
    cleanUp() // shutdown actor system used by ScalatestRouteTest
  }

  before {
    cassandraClient.flush(yes = true)
  }


  behavior of "pathSingleSlash"

  it should "return json response" in {
   Get() ~> routes ~> check {
     status shouldBe StatusCodes.OK
     contentType shouldBe `application/json`
   }
  }


  behavior of "non-existent path"

  it should "return not found" in {
    Get("/missing") ~> Route.seal(routes) ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }


  behavior of "login"

  it should "respond with 200 if user exists" in {
    Post("/api/users", user) ~> routes ~> check {
      status shouldBe StatusCodes.Created

      Get("/api/login") ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        SessionTestUtils.getSessionToken shouldBe 'defined
        SessionTestUtils.sessionIsExpired shouldBe false
      }
    }
  }

  it should "respond with 401 if user does not exist" in {
    Get("/api/login") ~> addCredentials(validCredentials) ~> Route.seal(routes) ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }


  behavior of "logout"

  it should "respond with bad request with no session token" in {
    Post("/api/logout") ~> Route.seal(routes) ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "get session token and respond with 200" in  {
    Post("/api/users", user) ~> routes ~> check {
      status shouldBe StatusCodes.Created

      Get("/api/login") ~> addCredentials(validCredentials) ~> routes ~> check {
        val Some(sessionToken) = SessionTestUtils.getSessionToken

        Post("/api/logout") ~> addHeader(SessionTestUtils.setSessionHeader(sessionToken)) ~> routes ~> check {
          status shouldBe StatusCodes.OK
          SessionTestUtils.sessionIsExpired shouldBe true
        }
      }
    }
  }

  it should "use tampered session and respond with bad request" in {
    Post("/api/users", user) ~> routes ~> check {
      status shouldBe StatusCodes.Created

      Get("/api/login") ~> addCredentials(validCredentials) ~> routes ~> check {
        val Some(sessionToken) = SessionTestUtils.getSessionToken
        val tamperedSession = sessionToken + "tampered"

        Post("/api/logout") ~> addHeader(SessionTestUtils.setSessionHeader(tamperedSession)) ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.Forbidden
          responseAs[String] shouldEqual "The supplied authentication is not authorized to access this resource"
        }
      }
    }
  }


  behavior of "current_login"

  it should "respond with bad request with no session token" in {
    Get("/api/current_login") ~> Route.seal(routes) ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  it should "get session token and respond with 200" in  {
    Post("/api/users", user) ~> routes ~> check {
      status shouldBe StatusCodes.Created

      Get("/api/login") ~> addCredentials(validCredentials) ~> routes ~> check {
        val Some(sessionToken) = SessionTestUtils.getSessionToken

        Get("/api/current_login") ~> addHeader(SessionTestUtils.setSessionHeader(sessionToken)) ~> routes ~> check {
          status shouldBe StatusCodes.OK
          SessionTestUtils.sessionIsExpired shouldBe false
        }
      }
    }
  }

  it should "use tampered session and respond with bad request" in {
    Post("/api/users", user) ~> routes ~> check {
      status shouldBe StatusCodes.Created

      Get("/api/login") ~> addCredentials(validCredentials) ~> routes ~> check {
        val Some(sessionToken) = SessionTestUtils.getSessionToken
        val tamperedSession = sessionToken + "tampered"

        Get("/api/current_login") ~> addHeader(SessionTestUtils.setSessionHeader(tamperedSession)) ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.Forbidden
        }
      }
    }
  }


  behavior of "POST users"

  it should "take email and password and store salt and hashed password" in {
    Post("/api/users", user) ~> routes ~> check {
      status shouldBe StatusCodes.Created
    }
  }

  it should "fail when missing either email or password" in {
    Post("/api/users", User("", "password")) ~> routes ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }
}
