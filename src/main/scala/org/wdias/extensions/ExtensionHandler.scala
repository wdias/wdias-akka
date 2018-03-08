package org.wdias.extensions

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, PoisonPill}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreValidatedTimeSeries
import org.wdias.constant.TimeSeriesEnvelop

import scala.concurrent.duration._

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
        case "validation" => validationRef = ref
        case default => log.warning("Unknown Actor Identity in ExtensionHandler: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}