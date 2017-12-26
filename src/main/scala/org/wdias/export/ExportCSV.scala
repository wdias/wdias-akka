package org.wdias.export

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapter.Adapter.GetTimeSeries
import org.wdias.constant.MetaData

import scala.concurrent.duration._

object ExportCSV {
  case class ExportCSVFile(metaData: MetaData)
}

class ExportCSV extends Actor with ActorLogging {
  import ExportCSV._
  implicit val timeout: Timeout = Timeout(15 seconds)

  var adapterRef: ActorRef = _
  context.actorSelection("/user/adapter") ! Identify(None)

  def receive = {
    case ExportCSVFile(metaData) =>
      log.debug("Export CSV: {}", metaData)
      adapterRef forward GetTimeSeries(metaData)

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Adapter (API): {}", ref)
      adapterRef = ref
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}