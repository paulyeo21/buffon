package buffon.streams.elasticsearch

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import buffon.{ElasticsearchClient, ShoeListing}
import buffon.ElasticsearchUtils._

import scala.concurrent.Future


object IndexShoeListing {
  def apply(listing: ShoeListing)(implicit esClient: ElasticsearchClient, materializer: ActorMaterializer): Future[ES_IndexResponse] = {
    val source = Source.single(listing)
    val flow = Flow[ShoeListing].mapAsyncUnordered(parallelism = 1)(esClient.indexShoeListing)
    val sink = Sink.last[ES_IndexResponse]
    source.via(flow).runWith(sink)
  }
}
