package org.wdias.adapter

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import akka.actor.Actor
import com.paulgoldbaum.influxdbclient.Parameter.Precision
// Check to Run in Actor Context
import scala.concurrent.ExecutionContext.Implicits.global
import com.paulgoldbaum.influxdbclient.{InfluxDB, Point, QueryResult, Record}
import org.wdias.adapter.Adapter._
import org.wdias.constant._

object Adapter {
  case class StoreTimeSeries(timeSeriesEnvelop: TimeSeriesEnvelop)
  case class GetTimeSeries(metaData: MetaData)
  case class Success(metadata: MetaData)
  case class Result(timeSeriesEnvelop: TimeSeriesEnvelop)
  case class Failure()
}

class Adapter extends Actor {
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

  def receive = {
    case StoreTimeSeries(data) =>
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
          .addField("value", tt.value)

        points = points :+ p
      }

      val bulkWrite = database.bulkWrite(points, precision = Precision.SECONDS)
      bulkWrite map { isWritten =>
        println("Written to the DB: " + isWritten)
        if(isWritten) {
          sender() ! Success(metaData)
        } else {
          sender() ! Failure
        }
      }

    case GetTimeSeries(query) =>
      val influxdb = InfluxDB.connect("localhost", 8086)
      val database = influxdb.selectDatabase("curw")

      //    val influxQuery = "SELECT * FROM observed"
      val queryResult = database.query("SELECT * FROM observed")

      queryResult map { result =>
        println(result.series.head.points("time"))
        sender() ! createResponse(query, result)
      }
  }
}