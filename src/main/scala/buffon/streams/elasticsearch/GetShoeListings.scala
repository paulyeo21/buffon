package buffon.streams.elasticsearch

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import buffon.ElasticsearchClient
import buffon.ElasticsearchUtils.ES_SearchResponse

import scala.concurrent.Future


object GetShoeListings {
  //noinspection ScalaDeprecation
  def apply(fromAndSize: (Int, Int))(implicit esClient: ElasticsearchClient, materializer: ActorMaterializer): Future[ES_SearchResponse] = {
    val source = Source.single(fromAndSize)
    val flow = Flow[(Int, Int)].mapAsyncUnordered(parallelism = 1)(esClient.getShoeListings)
    val sink = Sink.last[ES_SearchResponse]
    source.via(flow).runWith(sink)
  }
}
