import com.softwaremill.session.{Crypto, SessionUtil}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration


class CassandraClientSpec extends FlatSpec
  with Matchers with BeforeAndAfter with BeforeAndAfterAll with JsonSupport {
  private val config = ConfigFactory.load()
  private implicit val cassandraClient = new CassandraClient(config)
  private implicit val executor = ExecutionContext.global

  val email = "fugazzi@gmail.com"
  val password = "password"
  val selector = SessionUtil.randomString(16)
  val token = SessionUtil.randomString(64)
  val hash = Crypto.hash_SHA256(token)
  val expiration = System.currentTimeMillis()
  val sessionData = "data"

  override def beforeAll {
    cassandraClient.cleanUp(yes = true)
    cassandraClient.seed()
  }

  override def afterAll {
    cassandraClient.close()
  }

  before {
    cassandraClient.flush(yes = true)
  }


  behavior of "insertUser"

  it should "succeed in creating a new user" in {
    val f = cassandraClient.insertUser(email, password)
    val rs = Await.result(f, Duration.Inf)

    rs shouldBe true
  }


  behavior of "selectUser"

  it should "fail to retrieve non-existent user" in {
    val f = cassandraClient.selectUser(email, "email")
    val user = Await.result(f, Duration.Inf)

    user shouldBe None
  }

  it should "query cassandra for correct user given email and password" in {
    val f = cassandraClient.insertUser(email, password).flatMap { r =>
      cassandraClient.selectUser(email, "email")
    }
    val user = Await.result(f, Duration.Inf)

    user shouldBe a [Some[_]]
  }


  behavior of "deleteUser"

  it should "succeed in deleting user" in {
    val f = cassandraClient.insertUser(email, password).flatMap { r =>
      cassandraClient.deleteUser(email, "email")
    }
    val rs = Await.result(f, Duration.Inf)

    rs shouldBe true
  }

  it should "fail to delete unexist user" in {
    val f = cassandraClient.deleteUser(email, "email")
    val rs = Await.result(f, Duration.Inf)

    rs shouldBe false
  }


  behavior of "insertRefreshToken"

  it should "succeed in inserting a new refresh token" in {
    val f = cassandraClient.insertRefreshToken(selector, hash, expiration, sessionData)
    val rs = Await.result(f, Duration.Inf)

    rs shouldBe true
  }


  behavior of "selectRefreshToken"

  it should "succeed in selecting existing refresh token" in {
    val f = cassandraClient.insertRefreshToken(selector, hash, expiration, sessionData).flatMap { r =>
      cassandraClient.selectRefreshToken(selector)
    }
    val refreshToken = Await.result(f, Duration.Inf)

    refreshToken shouldBe a [Some[_]]
  }

  it should "fail in selecting non-existent refresh token" in {
    val f = cassandraClient.selectRefreshToken(selector)
    val refreshToken = Await.result(f, Duration.Inf)

    refreshToken shouldBe None
  }


  behavior of "deleteRefreshToken"

  it should "succeed in deleting refresh token" in {
    val f = cassandraClient.insertRefreshToken(selector, hash, expiration, sessionData).flatMap { r =>
      cassandraClient.deleteRefreshToken(selector)
    }
    val rs = Await.result(f, Duration.Inf)

    rs shouldBe true
  }

  it should "fail to delete non-existent refresh token" in {
    val f = cassandraClient.deleteRefreshToken(selector)
    val rs = Await.result(f, Duration.Inf)

    rs shouldBe false
  }
}
