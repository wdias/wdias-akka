package org.wdias.constant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

// 1. moduleId
// 2. ValueType
object ValueType extends Enumeration {
  type ValueType = Value
  val Scalar: ValueType.Value = Value("Scalar")
  val Vector: ValueType.Value = Value("Vector")
  val Grid: ValueType.Value = Value("Grid")
}

import ValueType._

// 3. Parameter
object ParameterType extends Enumeration {
  type ParameterType = Value
  val Instantaneous: ParameterType.Value = Value("Instantaneous")
  val Accumulative: ParameterType.Value = Value("Accumulative")
  val Mean: ParameterType.Value = Value("Mean")
}

import ParameterType._

case class Parameter(
                      parameterId: String,
                      variable: String,
                      unit: String,
                      parameterType: String
                    ) {
  def toParameterObj: ParameterObj = ParameterObj(this.parameterId, this.variable, this.unit, ParameterType.withName(this.parameterType))
}
case class ParameterObj(parameterId: String, variable: String, unit: String, parameterType: ParameterType) {
  def toParameter: Parameter = Parameter(this.parameterId, this.variable, this.unit, this.parameterType.toString)
}

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
// 4. Location
case class Station(name: String, latitude: Double, longitude: Double)
// TODO: Set locationId optional
case class Location(
                     locationId: String,
                     name: String,
                     lat: Float,
                     lon: Float,
                     elevation: Option[Float] = Option(0),
                     description: Option[String] = Option("")
                   )
// 5. TimeSeriesType
object TimeSeriesType extends Enumeration {
  type TimeSeriesType = Value
  val ExternalHistorical: TimeSeriesType.Value = Value("External Historical")
  val ExternalForecasting: TimeSeriesType.Value = Value("External Forecasting")
  val SimulatedHistorical: TimeSeriesType.Value = Value("Simulated Historical")
  val SimulatedForecasting: TimeSeriesType.Value = Value("Simulated Forecasting")
}

import TimeSeriesType._

// 6. TimeStep
object TimeStepUnit extends Enumeration {
  type TimeStepUnit = Value
  val Second: TimeStepUnit.Value = Value("Second")
  val Minute: TimeStepUnit.Value = Value("Minute")
  val Hour: TimeStepUnit.Value = Value("Hour")
  val Day: TimeStepUnit.Value = Value("Day")
  val Week: TimeStepUnit.Value = Value("Week")
  val Month: TimeStepUnit.Value = Value("Month")
  val Year: TimeStepUnit.Value = Value("Year")
  val NonEquidistant: TimeStepUnit.Value = Value("NonEquidistant")
}

import TimeStepUnit._

case class TimeStep(
                     timeStepId: String,
                     unit: String,
                     multiplier: Option[Int] = Option(0),
                     divider: Option[Int] = Option(0)
                   ) {
  def toTimeStepObj: TimeStepObj = TimeStepObj(this.timeStepId, TimeStepUnit.withName(this.unit), this.multiplier, this.divider)
}
case class TimeStepObj(
                        timeStepId: String,
                        unit: TimeStepUnit,
                        multiplier: Option[Int] = Option(0),
                        divider: Option[Int] = Option(0)
                      ) {
  def toTimeStep: TimeStep = TimeStep(this.timeStepId, this.unit.toString, this.multiplier, this.divider)
}

// Meta Data definition
case class MetaData(
                     moduleId: String,
                     valueType: String,
                     parameter: Parameter,
                     location: Location,
                     timeSeriesType: String,
                     timeStep: TimeStep,
                     tags: Array[String]
                   ) {
  def toMetadataObj: MetadataObj = MetadataObj(null, this.moduleId, ValueType.withName(this.valueType), this.parameter.toParameterObj, this.location, TimeSeriesType.withName(this.timeSeriesType), this.timeStep.toTimeStepObj, this.tags)
}
case class MetadataObj(timeSeriesId: String, moduleId: String, valueType: ValueType, parameter: ParameterObj, location: Location, timeSeriesType: TimeSeriesType, timeStep: TimeStepObj, tags: Array[String]) {
  def toMetadata: MetaData = MetaData(this.moduleId, this.valueType.toString, this.parameter.toParameter, this.location, this.timeSeriesType.toString, this.timeStep.toTimeStep, this.tags)
}
// Meta Data with Ids
case class MetadataIds(timeSeriesId: String, moduleId: String, valueType: ValueType, parameterId: String, locationId: String, timeSeriesType: TimeSeriesType, timeStepId: String)

// Time Series Data Points
case class DataPoint(time: String, value: Double)

// Time Series
case class TimeSeries(timeSeries: List[DataPoint] = List()) {
  def addDataPoint(time: String, value: Double): TimeSeries = copy(timeSeries = timeSeries :+ DataPoint(time, value))

  def addDataPoint(dataPoint: DataPoint): TimeSeries = copy(timeSeries = timeSeries :+ dataPoint)

  def addDataPoints(dataPoints: Array[DataPoint]): TimeSeries = copy(timeSeries = timeSeries ++ dataPoints)
}

case class DataLocation(dateType: String, fileType: String, location: String)

case class TimeSeriesEnvelop(metaData: MetaData, timeSeries: Option[TimeSeries], dataLocation: Option[DataLocation])

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val parameterFormat: RootJsonFormat[Parameter] = jsonFormat4(Parameter.apply)
  implicit val stationFormat: RootJsonFormat[Station] = jsonFormat3(Station.apply)
  implicit val locationFormat: RootJsonFormat[Location] = jsonFormat6(Location.apply)
  implicit val timeStepFormat: RootJsonFormat[TimeStep] = jsonFormat4(TimeStep.apply)
  implicit val metaDataFormat: RootJsonFormat[MetaData] = jsonFormat7(MetaData.apply)
  implicit val pointFormat: RootJsonFormat[DataPoint] = jsonFormat2(DataPoint.apply)
  implicit val timeSeriesFormat: RootJsonFormat[TimeSeries] = jsonFormat1(TimeSeries.apply)
  implicit val dataLocationFormat: RootJsonFormat[DataLocation] = jsonFormat3(DataLocation.apply)
  implicit val timeSeriesEnvelopFormat: RootJsonFormat[TimeSeriesEnvelop] = jsonFormat3(TimeSeriesEnvelop.apply)
}
