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
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

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

trait Service1 extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def createResponse(metaData: MetaData, result: QueryResult): TimeSeriesEnvelop = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    var points: List[DataPoint] = List()
    val records: List[Record] = result.series.head.records
    records.foreach { record =>
      println(record.allValues)
      val dateTimeStr: String = record.allValues(0).toString.split('Z')(0)
      val dateTime = LocalDateTime.parse(dateTimeStr)
      val value: Double = record.allValues(5).toString.toDouble
      points = points :+ DataPoint(dateTime.format(formatter), value)
    }
    val timeSeries = Some(TimeSeries(points))
    println("Created Response TimeSeries")
    TimeSeriesEnvelop(metaData, timeSeries, None)
  }

  def getObservedData(query: MetaData): Future[TimeSeriesEnvelop] = {
    val influxdb = InfluxDB.connect("localhost", 8086)
    logger.info(query.source)
    val database = influxdb.selectDatabase("curw")

//    val influxQuery = "SELECT * FROM observed"
    val queryResult = database.query("SELECT * FROM observed")

    queryResult map { result =>
      println(result.series.head.points("time"))
      createResponse(query, result)
    }
  }

  val routes = {
    logRequestResult("rest-api") {
      path("observed") {
        (post & entity(as[MetaData])) { query ⇒
          val response = getObservedData(query)
          onSuccess(response) { result =>
            complete(result)
          }
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

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port1"))
}