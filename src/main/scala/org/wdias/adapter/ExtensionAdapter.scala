package org.wdias.adapter

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import org.wdias.constant.{MetaData, TimeSeriesEnvelop}

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
        case GetValidationConfig(timeseriesEnvelop) =>
            log.info("GetValidationConfig:: {}, {}", timeseriesEnvelop, validationConfigs.find(_.name == timeseriesEnvelop.metaData.station.name))
            sender() ! ValidationConfigResult(validationConfigs.find(_.name == timeseriesEnvelop.metaData.station.name), timeseriesEnvelop)
            log.info("<<<<")
        case GetTransformationConfig(timeseriesEnvelop) =>
            log.info("GetTransformationConfig: {}", timeseriesEnvelop)
        case GetInterpolationConfig(timeseriesEnvelop) =>
            log.info("GetInterpolationConfig: {}", timeseriesEnvelop)
    }
}