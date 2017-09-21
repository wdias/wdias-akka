package org.wdias.constant

import java.time.LocalDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Station(name: String, latitude: Double, longitude: Double)

case class Unit(unit: String, `type`: String)

case class MetaData(station: Station, `type`: String, source: String, unit: Unit, tags: Array[String])

case class DataPoint(time: String, value: Double)

case class TimeSeries(timeSeries: Array[DataPoint] = Array()) {
  def addDataPoint(time: String, value:Double) = copy(timeSeries = timeSeries :+ DataPoint(time, value))
  def addDataPoint(dataPoint: DataPoint) = copy(timeSeries =  timeSeries :+ dataPoint)
  def addDataPoints(dataPoints: Array[DataPoint]) = copy(timeSeries =  timeSeries ++ dataPoints)
}

case class TimeSeriesEnvelop(metaData: MetaData, timeSeries: TimeSeries)

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val stationFormat = jsonFormat3(Station.apply)
  implicit val unitFormat = jsonFormat2(Unit.apply)
  implicit val metaDataFormat = jsonFormat5(MetaData.apply)
  implicit val pointFormat = jsonFormat2(DataPoint.apply)
  implicit val timeSeriesFormat = jsonFormat1(TimeSeries.apply)
  implicit val timeSeriesEnvelopFormat = jsonFormat2(TimeSeriesEnvelop.apply)
}
