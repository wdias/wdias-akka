package org.wdias.`import`.csv

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreTimeSeries
import org.wdias.constant._

import scala.concurrent.duration._

object ImportCSV {

  case class ImportCSVFile(metaData: Metadata, source: Array[String])

}

class ImportCSV extends Actor with ActorLogging {

  import ImportCSV._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)

  def receive: Receive = {
    case ImportCSVFile(metaData, source) =>
      log.debug("Import CSV: {}", source.length)
      var points: List[DataPoint] = List()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val zoneId = ZoneId.systemDefault

      source.map(_.split(",").toVector)
        .foreach(line => {
          val dateTime: LocalDateTime = LocalDateTime.parse(line(0), formatter)
          val p = DataPoint(dateTime.format(formatter)) addValue  line(1).toDouble

          points = points :+ p
        })

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ImportCSV): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ImportCSV: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}