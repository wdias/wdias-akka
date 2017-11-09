package org.wdias.`import`

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import akka.Done
import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{Framing, Source}
import akka.util.{ByteString, Timeout}
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.Point
import org.wdias.adapter.Adapter.{StoreFailure, StoreSuccess, StoreTimeSeries}
import org.wdias.constant._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._
import scala.concurrent.Future

object ImportCSV {

  case class ImportCSVFile(metaData: MetaData, source: Array[String])

}

class ImportCSV extends Actor with ActorLogging {

  import ImportCSV._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var adapterRef: ActorRef = _
  context.actorSelection("/user/adapter") ! Identify(None)

  def receive: Receive = {
    case ImportCSVFile(metaData, source) =>
      log.debug("Import CSV: {}", source.length)
      var points: List[DataPoint] = List()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val zoneId = ZoneId.systemDefault

      source.map(_.split(",").toVector)
          .foreach(line => {
            val dateTime: LocalDateTime = LocalDateTime.parse(line(0), formatter)
            val p = DataPoint(dateTime.format(formatter), line(1).toDouble)

            points = points :+ p
      })

      adapterRef forward StoreTimeSeries(TimeSeriesEnvelop(metaData, Some(TimeSeries(points)), None))

    case ActorIdentity(_, Some(ref)) =>
      println("Set Adapter", ref)
      adapterRef = ref
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}