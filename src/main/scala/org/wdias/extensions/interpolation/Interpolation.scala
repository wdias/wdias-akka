package org.wdias.extensions.interpolation

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{StoreSuccess, StoreTimeSeries}

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}
import org.wdias.constant.TimeSeriesEnvelop
import org.wdias.extensions.interpolation.Interpolation.InterpolationData
import scala.concurrent.ExecutionContext.Implicits.global

object Interpolation {

  case class InterpolationData(timeSeriesEnvelop: TimeSeriesEnvelop)

}

class Interpolation extends Actor {

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
