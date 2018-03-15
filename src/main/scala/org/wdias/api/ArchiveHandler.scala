package org.wdias.api

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreTimeSeries
import org.wdias.constant.TimeSeries

import scala.concurrent.duration._

object ArchiveHandler {

  case class ImportJSONData1(timeSeriesEnvelop: TimeSeries)

}

class ArchiveHandler extends Actor with ActorLogging {

  import ArchiveHandler._

  implicit val timeout: Timeout = Timeout(15 seconds)

  def receive: Receive = {
    case ImportJSONData1(timeSeriesEnvelop) =>
      /*val response: Future[StoreSuccess] = (adapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
      pipe(response) to senderRef*/
      log.debug("Forwarding ImportJSONData > ")
  }
}