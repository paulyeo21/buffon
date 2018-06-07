package buffon

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.sksamuel.elastic4s.http.search.SearchResponse
import spray.json.{DefaultJsonProtocol, JsonParser}


// Domain models
case class ShoeListing(
  name: String,
  brand: String,
  createdAt: Long,
  sku: Long,
  description: String,
  condition: String,
  gender: String
)
case class User(email: String, password: String)
case class RefreshToken(selector: String, hash: String, expiration: Long, sessionData: String)
case class ShoeListings(shoeListings: Array[ShoeListing])
case class SearchPayload(q: String, from: Int, size: Int, filters: Map[String, Seq[String]])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val shoeListingFormat = jsonFormat7(ShoeListing)
  implicit val userFormat = jsonFormat2(User)
  implicit val refreshTokenFormat = jsonFormat4(RefreshToken)
  implicit val shoeListingsFormat = jsonFormat1(ShoeListings)
  implicit val searchPayloadFormat = jsonFormat4(SearchPayload)

  def marshallSearchResponse(result: SearchResponse) = {
    result.hits.hits
      .map(_.sourceAsString)
      .map(JsonParser(_))
      .map(jsValue => jsValue.convertTo[ShoeListing])
  }
}
