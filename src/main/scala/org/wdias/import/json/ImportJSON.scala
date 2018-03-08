package org.wdias.`import`.json

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreTimeSeries
import org.wdias.constant.TimeSeriesEnvelop

import scala.concurrent.duration._

object ImportJSON {

  case class ImportJSONData(timeSeriesEnvelop: TimeSeriesEnvelop)

}

class ImportJSON extends Actor with ActorLogging {

  import ImportJSON._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)

  def receive: Receive = {
    case ImportJSONData(timeSeriesEnvelop) =>
      /*val response: Future[StoreSuccess] = (adapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
      pipe(response) to senderRef*/
      log.debug("Forwarding ImportJSONData > ", scalarAdapterRef)
      scalarAdapterRef forward StoreTimeSeries(timeSeriesEnvelop)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ImportJSON): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ImportJSON: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}