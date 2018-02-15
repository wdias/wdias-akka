package org.wdias.extensions

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, PoisonPill, Props}
import akka.util.Timeout
import akka.pattern.ask
import org.wdias.constant.TimeSeriesEnvelop

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.pipe
import org.wdias.adapters.extension_adapter.ExtensionAdapter.{GetValidationConfig, ValidationConfigResult}
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreValidatedTimeSeries
import org.wdias.extensions.validation.Validation
import org.wdias.extensions.validation.Validation.ValidationData

object ExtensionHandler {
  case class ExtensionHandlerData(timeSeriesEnvelop: TimeSeriesEnvelop)
  case class ExtensionHandlerResult(timeSeriesEnvelop: TimeSeriesEnvelop)
}

class ExtensionHandler extends Actor with ActorLogging {
  import ExtensionHandler._

  implicit val timeout: Timeout = Timeout(15 seconds)

  // implicit val ec: ExecutionContext = context.dispatcher

  var extensionAdapterRef: ActorRef = _
  context.actorSelection("/user/extensionAdapter") ! Identify(None)
  var interpolationRef: ActorRef = _
  context.actorSelection("/user/interpolation") ! Identify(None)
  var transformationRef: ActorRef = _
  context.actorSelection("/user/transformation") ! Identify(None)
  var validationRef: ActorRef = _
  context.actorSelection("/user/validation") ! Identify(None)

  def receive: Receive = {
    case ExtensionHandlerData(timeSeriesEnvelop) =>
      log.info("Extension Handler Data: {}", timeSeriesEnvelop)
      // Request for validation Rules
      extensionAdapterRef ! GetValidationConfig(timeSeriesEnvelop)

    case ValidationConfigResult(validationConfig, timeSeriesEnvelop) =>
      log.info("Got ValidationConfig {}", validationConfig)
      val validationRef: ActorRef = context.actorOf(Props[Validation])
      pipe((validationRef ? ValidationData(validationConfig.get, timeSeriesEnvelop))
        .mapTo[ExtensionHandlerResult] map { validatedTimeseriesEnvelop =>
        log.info("Got Validated Timeseries (ask): {}", validatedTimeseriesEnvelop)
        validationRef ! PoisonPill
        StoreValidatedTimeSeries(validatedTimeseriesEnvelop.timeSeriesEnvelop)
      }) to interpolationRef
    // NOTE: Handle without using ASK
    // validationRef ! ValidationData(validationConfig.get, timeSeriesEnvelop)

    case ExtensionHandlerResult(validatedTimeseriesEnvelop) =>
      log.info("Got Validated Timeseries {}", validatedTimeseriesEnvelop)
      sender() ! PoisonPill
      extensionAdapterRef ! StoreValidatedTimeSeries(validatedTimeseriesEnvelop)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ExtensionHandler): {}", ref.path.name)
      ref.path.name match {
        case "extensionAdapter" => extensionAdapterRef = ref
        case "interpolation" => interpolationRef = ref
        case "transformation" => transformationRef = ref
        case "interpolation" => interpolationRef = ref
        case default => log.warning("Unknown Actor Identity in ExtensionHandler: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}