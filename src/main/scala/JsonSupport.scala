import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


// Domain models
case class Shoe(name: String)
case class User(email: String, password: String)
case class RefreshToken(selector: String, hash: String, expiration: Long, sessionData: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val shoeFormat = jsonFormat1(Shoe)
  implicit val userFormat = jsonFormat2(User)
  implicit val refreshTokenFormat = jsonFormat4(RefreshToken)
}
