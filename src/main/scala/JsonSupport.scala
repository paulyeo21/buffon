import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


// Domain models
final case class Shoe(name: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val shoeFormat: RootJsonFormat[Shoe] = jsonFormat1(Shoe)
}
