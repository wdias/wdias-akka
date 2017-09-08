import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.IOException

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import spray.json.DefaultJsonProtocol

case class Station(name: String, latitude: Double, longitude: Double)

case class Unit(unit: String, `type`: String)

case class MetaData(station: Station, `type`: String, source: String, unit: Unit, tags: Array[String])

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val stationFormat = jsonFormat3(Station.apply)
  implicit val unitFormat = jsonFormat2(Unit.apply)
  implicit val metaDataFormat = jsonFormat5(MetaData.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def storeObservedData(data: MetaData): Either[String, MetaData] = {
    if(data.source == "WeatherStation") Right(data)
    else Left("Source Not handling")
  }

  val routes = {
    logRequestResult("on-demand-input") {
      pathPrefix("observed") {
        (post & entity(as[MetaData])) { observedData =>
          complete(observedData)
        }
      }
    }
  }
}

object OnDemandInput extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}