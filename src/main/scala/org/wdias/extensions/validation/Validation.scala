package org.wdias.extensions.validation

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import org.wdias.adapters.extension_adapter.ExtensionAdapter.ValidationConfig
import org.wdias.constant.TimeSeriesEnvelop
import org.wdias.extensions.ExtensionHandler.ExtensionHandlerResult
import org.wdias.extensions.validation.Validation.ValidationData
import scala.concurrent.duration._

object Validation {
  case class ValidationData(validationConfig: ValidationConfig, timeSeriesEnvelop: TimeSeriesEnvelop)
}

class Validation extends Actor with ActorLogging{

  implicit val timeout: Timeout = Timeout(15 seconds)

  def receive: Receive = {
    case ValidationData(validationConfig,timeSeriesEnvelop) =>
      log.info("Validating Data {}", validationConfig)
      sender() ! ExtensionHandlerResult(timeSeriesEnvelop)
  }

}
