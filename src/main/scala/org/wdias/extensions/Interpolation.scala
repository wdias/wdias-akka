package org.wdias.`import`

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import akka.Done
import akka.pattern.{ask, pipe}
import akka.actor.{Actor, ActorIdentity, ActorRef, Identify}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{Framing, Source}
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.Point
import org.wdias.adapter.Adapter.{StoreFailure, StoreSuccess, StoreTimeSeries}
import org.wdias.constant.{MetaData, TimeSeriesEnvelop}
import akka.util.Timeout

import scala.concurrent._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

object Interpolation {

    case class InterpolationData(timeSeriesEnvelop: TimeSeriesEnvelop)

}

class Interpolation extends Actor {

    import Interpolation._

    import context.dispatcher

    implicit val timeout: Timeout = Timeout(15 seconds)

    var adapterRef: ActorRef = _
    context.actorSelection("/user/adapter") ! Identify(None)

    def receive: Receive = {
        case InterpolationData(timeSeriesEnvelop) =>
            val senderRef = sender()
            /*adapterRef ? StoreTimeSeries(timeSeriesEnvelop) map {
                case StoreSuccess(metadata) =>
                    println("On StoreSuccess", metadata)
                    senderRef ! metadata
                case StoreFailure() =>
                    println("On StoreFailure")
                    senderRef ! "failed"
            }*/
            val response: Future[StoreSuccess] = (adapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
            pipe(response) to senderRef
        case ActorIdentity(_, Some(ref)) =>
            println("Set Adapter", ref)
            adapterRef = ref
        case ActorIdentity(_, None) =>
            context.stop(self)
    }
}