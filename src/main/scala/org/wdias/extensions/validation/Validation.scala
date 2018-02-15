package org.wdias.extensions.validation

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
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

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)
  var gridAdapterRef: ActorRef = _
  context.actorSelection("/user/gridAdapter") ! Identify(None)

  def receive: Receive = {
    case ValidationData(validationConfig,timeSeriesEnvelop) =>
      log.info("Validating Data {}", validationConfig)
      sender() ! ExtensionHandlerResult(timeSeriesEnvelop)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (Validation): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case "gridAdapter" => gridAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in Validation: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }

}
