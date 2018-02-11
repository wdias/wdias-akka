package org.wdias.adapters.extension_adapter

import akka.actor.{Actor, ActorLogging}
import org.wdias.adapters.extension_adapter.ExtensionAdapter._
import org.wdias.adapters.metadata_adapter.models.{LocationsDAO, ParametersDAO, TimeSeriesMetadataDAO, TimeStepsDAO}
import org.wdias.constant._

object ExtensionAdapter {

  case class GetValidationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetTransformationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetInterpolationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class ValidationConfig(name: String, minValue: Double, maxValue: Double)

  case class ValidationConfigResult(option: Option[ValidationConfig], timeSeriesEnvelop: TimeSeriesEnvelop)

}

class ExtensionAdapter extends Actor with ActorLogging {

  var validationConfigs = Set(
    ValidationConfig("Hanwella", 0, 120),
    ValidationConfig("Colombo", 0, 120),
    ValidationConfig("Waga", 0, 120),
  )

  def receive: Receive = {
    case GetValidationConfig(timeSeriesEnvelop) =>
      log.info("GetValidationConfig:: {}, {}", timeSeriesEnvelop, validationConfigs.find(_.name == timeSeriesEnvelop.metaData.location.name))

      sender() ! ValidationConfigResult(validationConfigs.find(_.name == timeSeriesEnvelop.metaData.location.name), timeSeriesEnvelop)
      log.info("<<<<")
    case GetTransformationConfig(timeSeriesEnvelop) =>
      log.info("GetTransformationConfig: {}", timeSeriesEnvelop)
    case GetInterpolationConfig(timeSeriesEnvelop) =>
      log.info("GetInterpolationConfig: {}", timeSeriesEnvelop)
  }
}
