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
import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.paulgoldbaum.influxdbclient.Parameter.{Consistency, Precision}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import spray.json.DefaultJsonProtocol
import com.paulgoldbaum.influxdbclient._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import org.wdias.constant._

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def storeObservedData(data: TimeSeriesEnvelop): Future[Boolean] = {
    val influxdb = InfluxDB.connect("localhost", 8086)
    val database = influxdb.selectDatabase("curw")
    val metaData: MetaData = data.metaData
    var points: List[Point] = List()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val zoneId = ZoneId.systemDefault
    data.timeSeries.timeSeries.foreach { tt: DataPoint =>
      val dateTime:LocalDateTime = LocalDateTime.parse(tt.time, formatter)
      val p = Point("observed", dateTime.atZone(zoneId).toEpochSecond())
        .addTag("station", metaData.station.name)
        .addTag("type", metaData.`type`)
        .addTag("source", metaData.source)
        .addTag("unit", metaData.unit.unit)
        .addField("value", tt.value)

      points = points :+ p
    }

    val bulkWrite = database.bulkWrite(points)
    bulkWrite map { isWritten =>
      println(isWritten)
      isWritten
    }
  }

  val routes = {
    logRequestResult("on-demand-input") {
      pathPrefix("observed") {
        (post & entity(as[TimeSeriesEnvelop])) { observedData =>
          val response = storeObservedData(observedData)
          onSuccess(response) { result =>
            if(result) {
              complete("Success")
            } else {
              complete("Fail")
            }
          }
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