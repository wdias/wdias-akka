package org.wdias.constant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

case class Station(name: String, latitude: Double, longitude: Double)

// TODO: Set locationId optional
case class Location(locationId: String, name: String, lat: Float, lon: Float, elevation: Option[Float] = Option(0), description: Option[String] = Option(""))

object ParameterType extends Enumeration {
  type ParameterType = Value
  val Instantaneous: ParameterType.Value = Value("Instantaneous")
  val Accumulative: ParameterType.Value = Value("Accumulative")
  val Mean: ParameterType.Value = Value("Mean")
}

import ParameterType._

case class Parameter(parameterId: String, variable: String, unit: String, parameterType: String)
case class ParameterObj(parameterId: String, variable: String, unit: String, parameterType: ParameterType)

/* TODO: Implement conversion between Objects
object ParameterJsonProtocol extends DefaultJsonProtocol {
  implicit object ParameterJsonFormat extends JsonFormat[ParameterObj] {
    def write(p: ParameterObj) = JsObject(
      "parameterId" -> JsString(p.parameterId),
      "variable" -> JsString(p.variable),
      "unit" -> JsString(p.unit),
      "parameterType" -> JsString(p.parameterType.toString)
    )

    def read(value: JsValue) = value.asJsObject.getFields("name", "red", "green", "blue") match {
      case Seq(JsString(parameterId), JsString(variable), JsString(unit), JsString(parameterType)) =>
        new ParameterObj(parameterId, variable, unit, ParameterType.Instantaneous)
      case _ => throw new DeserializationException("Color expected")
    }
  }
}*/

case class Unit(unit: String, `type`: String)

case class MetaData(station: Station, parameter: Parameter, `type`: String, source: String, unit: Unit, variable:String, tags: Array[String])

case class DataPoint(time: String, value: Double)

case class TimeSeries(timeSeries: List[DataPoint] = List()) {
  def addDataPoint(time: String, value: Double) = copy(timeSeries = timeSeries :+ DataPoint(time, value))

  def addDataPoint(dataPoint: DataPoint) = copy(timeSeries = timeSeries :+ dataPoint)

  def addDataPoints(dataPoints: Array[DataPoint]) = copy(timeSeries = timeSeries ++ dataPoints)
}

case class DataLocation(dateType: String, fileType: String, location: String)

case class TimeSeriesEnvelop(metaData: MetaData, timeSeries: Option[TimeSeries], dataLocation: Option[DataLocation])

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val stationFormat = jsonFormat3(Station.apply)
  implicit val locationFormat = jsonFormat6(Location.apply)
  implicit val parameterFormat = jsonFormat4(Parameter.apply)
  implicit val unitFormat = jsonFormat2(Unit.apply)
  implicit val metaDataFormat = jsonFormat7(MetaData.apply)
  implicit val pointFormat = jsonFormat2(DataPoint.apply)
  implicit val timeSeriesFormat = jsonFormat1(TimeSeries.apply)
  implicit val dataLocationFormat = jsonFormat3(DataLocation.apply)
  implicit val timeSeriesEnvelopFormat = jsonFormat3(TimeSeriesEnvelop.apply)
}
