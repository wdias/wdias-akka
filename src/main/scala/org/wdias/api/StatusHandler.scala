package org.wdias.api

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreTimeSeries
import org.wdias.constant.TimeSeriesEnvelop

import scala.concurrent.duration._

object StatusHandler {

  case class ImportJSONData(timeSeriesEnvelop: TimeSeriesEnvelop)

}

class StatusHandler extends Actor with ActorLogging {

  import StatusHandler._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var metadataAdapterRef: ActorRef = _
  context.actorSelection("/user/metadataAdapter") ! Identify(None)

  def receive: Receive = {
    case ImportJSONData(timeSeriesEnvelop) =>
      /*val response: Future[StoreSuccess] = (adapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
      pipe(response) to senderRef*/
      log.debug("Forwarding ImportJSONData > ", metadataAdapterRef)
      metadataAdapterRef forward StoreTimeSeries(timeSeriesEnvelop)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (StatusHandler): {}", ref.path.name)
      ref.path.name match {
        case "metadataAdapter" => metadataAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in StatusHandler: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}
