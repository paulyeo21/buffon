package buffon.streams.elasticsearch

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import buffon.ElasticsearchClient
import buffon.ElasticsearchUtils._

import scala.concurrent.Future


object SearchShoeListings {
  //noinspection ScalaDeprecation
  def apply(queryFromAndSize: (String, Int, Int))(implicit esClient: ElasticsearchClient, materializer: ActorMaterializer): Future[ES_SearchResponse] = {
    val source = Source.single(queryFromAndSize)
    val flow = Flow[(String, Int, Int)].mapAsyncUnordered(parallelism = 1)(esClient.searchShoeListings)
    val sink = Sink.last[ES_SearchResponse]
    source.via(flow).runWith(sink)
  }
}
