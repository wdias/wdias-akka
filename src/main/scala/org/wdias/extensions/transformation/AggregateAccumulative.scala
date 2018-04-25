package org.wdias.extensions.transformation

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{StoreSuccess, StoreTimeSeries}
import org.wdias.constant.TimeSeries

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}
import org.wdias.adapters.extension_adapter.ExtensionAdapter.GetExtensionById
import org.wdias.extensions.ExtensionHandler.GetExtensionDataById
import org.wdias.extensions._

import scala.concurrent.ExecutionContext.Implicits.global

object AggregateAccumulative {
}

class AggregateAccumulative extends Actor with ActorLogging {
  def receive: Receive = {
    case TriggerExtension(extensionObj: ExtensionObj) =>
      log.info("TriggerExtension > {}", extensionObj)
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}