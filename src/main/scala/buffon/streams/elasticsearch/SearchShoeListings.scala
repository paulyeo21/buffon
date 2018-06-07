package buffon.streams.elasticsearch

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import buffon.{ElasticsearchClient, SearchPayload}
import buffon.ElasticsearchUtils._

import scala.concurrent.Future


object SearchShoeListings {
  def apply(
    payload: SearchPayload
  )(
    implicit esClient: ElasticsearchClient, materializer: ActorMaterializer
  ): Future[ES_SearchResponse] = {
    val source = Source.single(payload)
    val flow = Flow[SearchPayload].mapAsyncUnordered(parallelism = 1)(esClient.searchShoeListings)
    val sink = Sink.last[ES_SearchResponse]
    source.via(flow).runWith(sink)
  }
}
