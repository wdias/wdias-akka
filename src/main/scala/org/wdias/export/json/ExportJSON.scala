package org.wdias.export.json

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.GetTimeSeries
import org.wdias.constant.{Metadata, MetadataObj}

import scala.concurrent.duration._

object ExportJSON {

  case class ExportJSONData(metadataObj: MetadataObj)

}

class ExportJSON extends Actor with ActorLogging {

  import ExportJSON._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)

  def receive = {
    case ExportJSONData(metadataObj: MetadataObj) =>
      log.info("Export JSON: {}", metadataObj)
      log.info("Sender: {}, To: {}", sender(), scalarAdapterRef)
      scalarAdapterRef forward GetTimeSeries(metadataObj)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ExportJSON): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ExportJSON: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}