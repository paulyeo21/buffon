package buffon

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.sksamuel.elastic4s.http.search.SearchResponse
import spray.json.{DefaultJsonProtocol, JsonParser}


// Domain models
case class ShoeListing(name: String, brand: String, createdAt: Long, sku: Long)
case class User(email: String, password: String)
case class RefreshToken(selector: String, hash: String, expiration: Long, sessionData: String)
case class ShoeListings(shoeListings: Array[ShoeListing])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val shoeListingFormat = jsonFormat4(ShoeListing)
  implicit val userFormat = jsonFormat2(User)
  implicit val refreshTokenFormat = jsonFormat4(RefreshToken)
  implicit val shoeListingsFormat = jsonFormat1(ShoeListings)

  def marshallSearchResponse(result: SearchResponse) = {
    result.hits.hits
      .map(_.sourceAsString)
      .map(JsonParser(_))
      .map(jsValue => jsValue.convertTo[ShoeListing])
  }
}
