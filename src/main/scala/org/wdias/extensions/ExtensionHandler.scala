package org.wdias.extensions

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, PoisonPill, Props}
import akka.util.Timeout
import org.wdias.adapter.ExtensionAdapter._
import akka.pattern.ask
import org.wdias.adapter.Adapter.StoreValidatedTimeSeries
import org.wdias.constant.TimeSeriesEnvelop
import org.wdias.extensions.Validation.ValidationData

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.pipe

object ExtensionHandler {
    case class ExtensionHandlerData(timeSeriesEnvelop: TimeSeriesEnvelop)
    case class ExtensionHandlerResult(timeSeriesEnvelop: TimeSeriesEnvelop)
}

class ExtensionHandler extends Actor with ActorLogging {
    import ExtensionHandler._

    implicit val timeout: Timeout = Timeout(15 seconds)

    // implicit val ec: ExecutionContext = context.dispatcher

    var extensionAdapterRef: ActorRef = _
    context.actorSelection("/user/extensionAdapter") ! Identify(None)
    var adapterRef:ActorRef = _

    def receive: Receive = {
        case ExtensionHandlerData(timeSeriesEnvelop) =>
            log.info("Extension Handler Data: {}", timeSeriesEnvelop)
            adapterRef = sender()
            // Request for validation Rules
            extensionAdapterRef ! GetValidationConfig(timeSeriesEnvelop)

        case ValidationConfigResult(validationConfig, timeSeriesEnvelop) =>
            log.info("Got ValidationConfig {}", validationConfig)
            val validationRef: ActorRef = context.actorOf(Props[Validation])
            pipe((validationRef ? ValidationData(validationConfig.get, timeSeriesEnvelop))
                .mapTo[ExtensionHandlerResult] map { validatedTimeseriesEnvelop =>
                log.info("Got Validated Timeseries (ask): {}", validatedTimeseriesEnvelop)
                validationRef ! PoisonPill
                StoreValidatedTimeSeries(validatedTimeseriesEnvelop.timeSeriesEnvelop)
            }) to adapterRef
            // NOTE: Handle without using ASK
            // validationRef ! ValidationData(validationConfig.get, timeSeriesEnvelop)

        case ExtensionHandlerResult(validatedTimeseriesEnvelop) =>
            log.info("Got Validated Timeseries {}", validatedTimeseriesEnvelop)
            sender() ! PoisonPill
            adapterRef ! StoreValidatedTimeSeries(validatedTimeseriesEnvelop)

        case ActorIdentity(_, Some(ref)) =>
            println("Set Extension Handler Ref::",ref, ref.path.name)
            extensionAdapterRef = ref
        case ActorIdentity(_, None) =>
            context.stop(self)
    }
}