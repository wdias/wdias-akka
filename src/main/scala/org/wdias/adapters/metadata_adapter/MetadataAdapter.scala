package org.wdias.adapters.metadata_adapter

import akka.actor.{Actor, ActorLogging}
import org.wdias.adapters.extension_adapter.ExtensionAdapter._
import org.wdias.adapters.metadata_adapter.models.{LocationsDAO, ParametersDAO, TimeSeriesMetadataDAO, TimeStepsDAO}
import org.wdias.constant._

class MetadataAdapter extends Actor with ActorLogging {

  var validationConfigs = Set(
    ValidationConfig("Hanwella", 0, 120),
    ValidationConfig("Colombo", 0, 120),
  )

  def receive: Receive = {
    case GetValidationConfig(timeSeriesEnvelop) =>
      log.info("GetValidationConfig:: {}, {}", timeSeriesEnvelop, validationConfigs.find(_.name == timeSeriesEnvelop.metaData.location.name))

      val stationName = timeSeriesEnvelop.metaData.location.name
      LocationsDAO.create(Location(stationName, stationName, 0, 0))
      val p: ParameterObj = timeSeriesEnvelop.metaData.parameter.toParameterObj
      ParametersDAO.create(p)
      TimeStepsDAO.create(TimeStepObj("every_5_min", TimeStepUnit.Minute, Option(5)))
      TimeSeriesMetadataDAO.create(MetadataIds("asdf", "WeatherStation", ValueType.Scalar, p.parameterId, stationName, TimeSeriesType.ExternalHistorical, "every_5_min"))

      sender() ! ValidationConfigResult(validationConfigs.find(_.name == timeSeriesEnvelop.metaData.location.name), timeSeriesEnvelop)
      log.info("<<<<")
    case GetTransformationConfig(timeSeriesEnvelop) =>
      log.info("GetTransformationConfig: {}", timeSeriesEnvelop)
    case GetInterpolationConfig(timeSeriesEnvelop) =>
      log.info("GetInterpolationConfig: {}", timeSeriesEnvelop)
  }
}
