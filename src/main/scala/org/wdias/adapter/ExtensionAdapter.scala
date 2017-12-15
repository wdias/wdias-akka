package org.wdias.adapter

import akka.actor.{Actor, ActorLogging}
import org.wdias.adapter.models._
import org.wdias.constant._
import org.wdias.constant.TimeSeriesEnvelop

object ExtensionAdapter {

  case class GetValidationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetTransformationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetInterpolationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class ValidationConfig(name: String, minValue: Double, maxValue: Double)

  case class ValidationConfigResult(option: Option[ValidationConfig], timeSeriesEnvelop: TimeSeriesEnvelop)

}

class ExtensionAdapter extends Actor with ActorLogging {

  import ExtensionAdapter._

  var validationConfigs = Set(
    ValidationConfig("Hanwella", 0, 120),
    ValidationConfig("Colombo", 0, 120),
  )

  def receive: Receive = {
    case GetValidationConfig(timeSeriesEnvelop) =>
      log.info("GetValidationConfig:: {}, {}", timeSeriesEnvelop, validationConfigs.find(_.name == timeSeriesEnvelop.metaData.station.name))

      val stationName = timeSeriesEnvelop.metaData.station.name
      LocationsDAO.create(Location(stationName, stationName, 0, 0))
      val p: Parameter = timeSeriesEnvelop.metaData.parameter

      ParametersDAO.create(ParameterObj("1234", "Discharge", "mm", ParameterType.Accumulative))
      TimeStepsDAO.create(TimeStep("every_5_min", TimeStepUnit.Minute, 5, 0))
      TimeSeriesMetadataDAO.create(TimeSeriesMetadata("asdf", "WeatherStation", ValueType.Scalar, "1234", stationName, TimeSeriesType.ExternalHistorical, "every_5_min"))

      sender() ! ValidationConfigResult(validationConfigs.find(_.name == timeSeriesEnvelop.metaData.station.name), timeSeriesEnvelop)
      log.info("<<<<")
    case GetTransformationConfig(timeseriesEnvelop) =>
      log.info("GetTransformationConfig: {}", timeseriesEnvelop)
    case GetInterpolationConfig(timeseriesEnvelop) =>
      log.info("GetInterpolationConfig: {}", timeseriesEnvelop)
  }
}