package org.wdias.extensions.transformation

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify}
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

class Transformation extends Actor {

  implicit val timeout: Timeout = Timeout(15 seconds)

  var adapterRef: ActorRef = _
  context.actorSelection("/user/adapter") ! Identify(None)

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
      val response: Future[StoreSuccess] = (adapterRef ? StoreTimeSeries(timeSeriesEnvelop)).mapTo[StoreSuccess]
      pipe(response) to senderRef
    case ActorIdentity(_, Some(ref)) =>
      println("Set Adapter", ref)
      adapterRef = ref
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}
