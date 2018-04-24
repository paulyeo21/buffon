import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration


class CassandraClientSpec extends FlatSpec
  with Matchers with BeforeAndAfter with BeforeAndAfterAll with JsonSupport {
  private val config = ConfigFactory.load()
  private implicit val cassandraClient = new CassandraClient(config)
  private implicit val executor = ExecutionContext.global

  val email = "email"
  val password = "password"

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


  behavior of "selectUser"

  it should "fail to retrieve non-existent user" in {
    val f = cassandraClient.selectUser(email)
    val user = Await.result(f, Duration.Inf)

    user shouldBe None
  }

  it should "query cassandra for correct user given email and password" in {
    val f = cassandraClient.insertUser(email, password).flatMap { r =>
      cassandraClient.selectUser(email)
    }
    val user = Await.result(f, Duration.Inf)

    user shouldBe a [Some[_]]
  }
}
