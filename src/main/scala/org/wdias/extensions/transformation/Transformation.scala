package org.wdias.extensions.transformation

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{StoreSuccess, StoreTimeSeries}
import org.wdias.constant.TimeSeriesEnvelop
import org.wdias.extensions.transformation.Transformation.TransformationData

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}

import scala.concurrent.ExecutionContext.Implicits.global

object Transformation {
  case class TransformationData(timeSeriesEnvelop: TimeSeriesEnvelop)
}

class Transformation extends Actor with ActorLogging {

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)
  var gridAdapterRef: ActorRef = _
  context.actorSelection("/user/gridAdapter") ! Identify(None)

  def receive: Receive = {
    case TransformationData(timeSeriesEnvelop) =>
      val senderRef = sender()
      /*adapterRef ? StoreTimeSeries(timeSeriesEnvelop) map {
          case StoreSuccess(metadata) =>
              println("On StoreSuccess", metadata)
              senderRef ! metadata
          case StoreFailure() =>
              println("On StoreFailure")
              senderRef ! "failed"
      }*/
      val response: Future[StoreSuccess] = (scalarAdapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
      pipe(response) to senderRef
    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (Transformation): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case "gridAdapter" => gridAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in Transformation: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}
