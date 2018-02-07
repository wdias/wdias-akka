package org.wdias.`import`.json

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapter.scalar_adapter.ScalarAdapter.StoreTimeSeries
import org.wdias.constant.TimeSeriesEnvelop

import scala.concurrent.duration._

object ImportJSON {

  case class ImportJSONData(timeSeriesEnvelop: TimeSeriesEnvelop)

}

class ImportJSON extends Actor with ActorLogging {

  import ImportJSON._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var adapterRef: ActorRef = _
  context.actorSelection("/user/adapter") ! Identify(None)

  def receive: Receive = {
    case ImportJSONData(timeSeriesEnvelop) =>
      /*val response: Future[StoreSuccess] = (adapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
      pipe(response) to senderRef*/
      log.debug("Forwarding ImportJSONData > ", adapterRef)
      adapterRef forward StoreTimeSeries(timeSeriesEnvelop)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Adapter (Input): {}", ref)
      adapterRef = ref
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}