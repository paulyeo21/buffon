package buffon

import buffon.ElasticsearchUtils._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.index.admin.FlushIndexResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{HttpClient, HttpExecutable, RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.sksamuel.elastic4s.searches.queries.matches.MultiMatchQueryDefinition
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}


object ElasticsearchUtils {
  val ES_SHOES_INDEX = "shoe"
  val ES_INDEX_TYPE = "doc" // type is deprecating so same for all indices
  val ES_SHOES_INDEX_BRAND_FIELD = "brand"
  val ES_SHOES_INDEX_NAME_FIELD = "name"
  val ES_SHOES_INDEX_TIMESTAMP_FIELD = "createdAt"
  val ES_SHOES_INDEX_SKU_FIELD = "sku"
  val ES_SHOES_INDEX_DESCRIPTION_FIELD = "description"
  val ES_SHOES_INDEX_CONDITION_FIELD = "condition"
  val ES_SHOES_INDEX_GENDER_FIELD = "gender"
  val ES_SHOES_INDEX_SIZES_FIELD = "sizes"
  val ES_INDICES = Seq(ES_SHOES_INDEX)

  type ES_SearchResponse = Either[RequestFailure, RequestSuccess[SearchResponse]]
  type ES_IndexResponse = Either[RequestFailure, RequestSuccess[IndexResponse]]
  type ES_FlushIndexResponse = Either[RequestFailure, RequestSuccess[FlushIndexResponse]]
}

class ElasticsearchClient(config: Config)(implicit ec: ExecutionContextExecutor) {
  import com.sksamuel.elastic4s.http.ElasticDsl._

  private val esClient = HttpClient(ElasticsearchClientUri(config.getString("elasticsearch.hostname"), config.getInt("elasticsearch.port")))
  private val MAX_QUERY_SIZE = config.getInt("elasticsearch.maxQuerySize")

  def execute[T, U](request: T)(
    implicit exec: HttpExecutable[T, U]
  ): Future[Either[RequestFailure, RequestSuccess[U]]] = {
    esClient.execute(request)
  }

  def seed(): Unit = {
    // Create shoes index
    esClient.execute {
      createIndex(ES_SHOES_INDEX).mappings(
        mapping(ES_INDEX_TYPE).fields(
          textField(ES_SHOES_INDEX_NAME_FIELD),
          keywordField(ES_SHOES_INDEX_BRAND_FIELD),
          dateField(ES_SHOES_INDEX_TIMESTAMP_FIELD),
          longField(ES_SHOES_INDEX_SKU_FIELD),
          textField(ES_SHOES_INDEX_DESCRIPTION_FIELD),
          keywordField(ES_SHOES_INDEX_CONDITION_FIELD),
          keywordField(ES_SHOES_INDEX_GENDER_FIELD)
        )
      )
    }.await
  }

  def close(): Unit = {
    esClient.close()
  }

  def cleanUp(yes: Boolean = false): Unit = {
    if (yes) {
      esClient.execute(deleteIndex(ES_SHOES_INDEX)).await
    }
  }

  def indexShoeListing(s: ShoeListing): Future[ES_IndexResponse] = {
    esClient.execute {
      indexInto(ES_SHOES_INDEX / ES_INDEX_TYPE).fields(
        ES_SHOES_INDEX_NAME_FIELD -> s.name,
        ES_SHOES_INDEX_BRAND_FIELD -> s.brand,
        ES_SHOES_INDEX_TIMESTAMP_FIELD -> s.createdAt,
        ES_SHOES_INDEX_SKU_FIELD -> s.sku,
        ES_SHOES_INDEX_DESCRIPTION_FIELD -> s.description,
        ES_SHOES_INDEX_CONDITION_FIELD -> s.condition,
        ES_SHOES_INDEX_GENDER_FIELD -> s.gender
      )
    }
  }

  /*
    Search ES  for shoes given query string, document start index (from), number of documents to retrieve (size),
    and a map of fields to values to filter results (filters).
    Note: If query string is empty, method retrieves all documents instead of default result of matching with an empty
          string of returning no documents.
   */
  def searchShoeListings(payload: SearchPayload): Future[ES_SearchResponse] = {
    val filterQueries = payload.filters.map { case (field, values) =>
      termsQuery(field, values)
    }.toSeq

    val multiMatchQuery = MultiMatchQueryDefinition(text = payload.q, fuzziness = Some("AUTO"))
      .fields(ES_SHOES_INDEX_NAME_FIELD, ES_SHOES_INDEX_DESCRIPTION_FIELD)

    //noinspection ScalaDeprecation
    if (payload.q.isEmpty) {
      esClient.execute {
        search(ES_SHOES_INDEX / ES_INDEX_TYPE).from(payload.from).size(payload.size).query {
          BoolQueryDefinition(filters = filterQueries, must = Seq(matchAllQuery))
        }
      }
    } else {
      esClient.execute {
        search(ES_SHOES_INDEX / ES_INDEX_TYPE).from(payload.from).size(payload.size).query {
          BoolQueryDefinition(filters = filterQueries, must = Seq(multiMatchQuery))
        }
      }
    }
  }
}
