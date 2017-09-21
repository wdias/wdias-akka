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
import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import spray.json.DefaultJsonProtocol
import com.paulgoldbaum.influxdbclient._
import scala.util.{Success, Failure}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import org.wdias.constant._

trait Service1 extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def getObservedData(query: MetaData): MetaData = {
    val influxdb = InfluxDB.connect("localhost", 8086)
    logger.info(query.source)
    val database = influxdb.selectDatabase("curw")
//    val point = Point("observed")
//      .addTag("station", data.station.name)
//      .addTag("type", data.`type`)
//      .addTag("source", data.source)
//      .addTag("unit", data.unit.unit)


//    val influxQuery = "SELECT * FROM observed"
    val queryResult = database.query("SELECT * FROM observed")
    queryResult.onComplete({
      case Success(result) => {
        println(result.series.head.points("value"))
      }
      case Failure(exception) => {
        println("An error has occurred: " + exception.getMessage)
      }
    })
    println("Function return")
    query
  }

  val routes = {
    logRequestResult("rest-api") {
      pathPrefix("observed") {
        (post & entity(as[MetaData])) { query ⇒
          val response = getObservedData(query)
          complete(response)
        }
      }
    }
  }
}

object API extends App with Service1 {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}