package org.wdias.export.csv

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.GetTimeSeries
import org.wdias.constant.Metadata

import scala.concurrent.duration._

object ExportCSV {
  case class ExportCSVFile(metaData: Metadata)
}

class ExportCSV extends Actor with ActorLogging {
  import ExportCSV._
  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)

  def receive = {
    case ExportCSVFile(metaData) =>
      log.debug("Export CSV: {}", metaData)
      scalarAdapterRef forward GetTimeSeries(metaData)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ExportCSV): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ExportCSV: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}