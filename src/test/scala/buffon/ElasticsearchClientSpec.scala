package buffon

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import buffon.streams.elasticsearch.IndexShoeListing
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Random


class ElasticsearchClientSpec extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {
  import com.sksamuel.elastic4s.http.ElasticDsl._

  private val config = ConfigFactory.load()
  private implicit val system = ActorSystem()
  private implicit val executor = system.dispatcher
  private implicit val materializer = ActorMaterializer()
  private implicit val esClient = new ElasticsearchClient(config)

  override def afterAll {
    esClient.close()
    system.terminate()
  }

  before {
    esClient.cleanUp(yes = true)
    esClient.seed()
  }


  behavior of "searchShoeListings"

  it should "multi search on fields 'name' and 'brand'" in {
    val shoe = ShoeListing("air force 1", "nike", System.currentTimeMillis(), Random.nextLong(), "description", "deadstock", "male", Seq())
    val payload = SearchPayload("", 0, 20, Map("condition" -> Seq("ds")))

    val index = IndexShoeListing(shoe).await
    val search = esClient.searchShoeListings(payload).await

    search.right.map { res =>
      res.status shouldBe 200
    }
  }
}
