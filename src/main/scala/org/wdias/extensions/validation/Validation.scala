package org.wdias.extensions.validation

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout

import scala.concurrent.duration._

object Validation {
}

class Validation extends Actor with ActorLogging{

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)
  var gridAdapterRef: ActorRef = _
  context.actorSelection("/user/gridAdapter") ! Identify(None)

  def receive: Receive = {
    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (Validation): {}", ref.path.name)
      ref.path.name match {
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case "gridAdapter" => gridAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in Validation: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }

}
