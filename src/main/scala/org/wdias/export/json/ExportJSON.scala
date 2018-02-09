package org.wdias.export.json

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.GetTimeSeries
import org.wdias.constant.Metadata

import scala.concurrent.duration._

object ExportJSON {

  case class ExportJSONData(metaData: Metadata)

}

class ExportJSON extends Actor with ActorLogging {

  import ExportJSON._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var adapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)

  def receive = {
    case ExportJSONData(metaData) =>
      log.info("Export JSON: {}", metaData)
      log.info("Sender: {}, To: {}", sender(), adapterRef)
      adapterRef forward GetTimeSeries(metaData)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Adapter (API): {}", ref)
      adapterRef = ref
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}