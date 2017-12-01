package org.wdias.adapter

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.pattern.pipe
import akka.util.Timeout
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import org.wdias.extensions.ExtensionHandler.{ExtensionHandlerData, ExtensionHandlerResult}
// Check to Run in Actor Context
import com.paulgoldbaum.influxdbclient.{InfluxDB, Point, QueryResult, Record}
import org.wdias.constant._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Adapter {

  case class StoreTimeSeries(timeSeriesEnvelop: TimeSeriesEnvelop)
  case class StoreValidatedTimeSeries(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetTimeSeries(metaData: MetaData)

  case class StoreSuccess(metadata: MetaData)

  case class Result(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class StoreFailure()

}

class Adapter extends Actor with ActorLogging {

  import Adapter._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var extensionHandlerRef: ActorRef = _
  context.actorSelection("/user/extensionHandler") ! Identify(None)

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

  def receive: Receive = {
    case StoreTimeSeries(data) =>
      log.info("StoringTimeSeries... {}", sender())
      val influxdb = InfluxDB.connect("localhost", 8086)
      val database = influxdb.selectDatabase("curw")
      val metaData: MetaData = data.metaData
      var points: List[Point] = List()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val zoneId = ZoneId.systemDefault
      data.timeSeries.get.timeSeries.foreach { tt: DataPoint =>
        val dateTime: LocalDateTime = LocalDateTime.parse(tt.time, formatter)
        val p = Point("observed", dateTime.atZone(zoneId).toEpochSecond())
          .addTag("station", metaData.station.name)
          .addTag("type", metaData.`type`)
          .addTag("source", metaData.source)
          .addTag("unit", metaData.unit.unit)
          .addTag("variable", metaData.variable)
          .addField("value", tt.value)

        points = points :+ p
      }

      pipe(database.bulkWrite(points, precision = Precision.SECONDS).mapTo[Boolean] map { isWritten =>
        println("Written to the DB: " + isWritten)
        if (isWritten) {
          log.info("Data written to DB Success.")
          log.info("Send Data to Extension Handler: {}", extensionHandlerRef)
          extensionHandlerRef ! ExtensionHandlerData(data)
          StoreSuccess(metaData)
        } else {
          StoreFailure()
        }
      }) to sender()

    case StoreValidatedTimeSeries(data) =>
      log.info("StoreValidatedTimeSeries... {}, {}", sender(), data)


    case GetTimeSeries(query) =>
      val influxdb = InfluxDB.connect("localhost", 8086)
      val database = influxdb.selectDatabase("curw")

      //    val influxQuery = "SELECT * FROM observed"
      val queryResult = database.query("SELECT * FROM observed")

      pipe(queryResult.mapTo[QueryResult] map { result =>
        Result(createResponse(query, result))
      }) to sender()

    case ActorIdentity(_, Some(ref)) =>
      println("Set Extension Handler Ref", ref)
      extensionHandlerRef = ref
    case ActorIdentity(_, None) =>
      context.stop(self)
    case _ =>
      log.info("Unknown message")
  }
}