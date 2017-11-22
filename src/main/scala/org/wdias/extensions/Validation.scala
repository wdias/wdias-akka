package org.wdias.extensions

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import akka.Done
import akka.pattern.{ask, pipe}
import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{Framing, Source}
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.Point
import org.wdias.adapter.Adapter.{StoreFailure, StoreSuccess, StoreTimeSeries}
import org.wdias.constant.{MetaData, TimeSeriesEnvelop}
import akka.util.Timeout
import org.wdias.adapter.ExtensionAdapter.ValidationConfig
import org.wdias.extensions.ExtensionHandler.{ExtensionHandlerData, ExtensionHandlerResult}

import scala.concurrent._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

object Validation {
    case class ValidationData(validationConfig: ValidationConfig, timeSeriesEnvelop: TimeSeriesEnvelop)
}

class Validation extends Actor with ActorLogging{
    import Validation._

    implicit val timeout: Timeout = Timeout(15 seconds)

    def receive: Receive = {
        case ValidationData(validationConfig,timeSeriesEnvelop) =>
            log.info("Validating Data {}", validationConfig)
            sender() ! ExtensionHandlerResult(timeSeriesEnvelop)
    }

}