package org.wdias.extensions

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.extension_adapter.ExtensionAdapter.GetExtensions
import org.wdias.constant.TimeSeries
import akka.pattern.ask

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExtensionHandler {
  case class OnChangeTimeseries(timeSeries: TimeSeries)
}

class ExtensionHandler extends Actor with ActorLogging {
  import ExtensionHandler._

  implicit val timeout: Timeout = Timeout(15 seconds)

  // implicit val ec: ExecutionContext = context.dispatcher

  var extensionAdapterRef: ActorRef = _
  context.actorSelection("/user/extensionAdapter") ! Identify(None)
  var interpolationRef: ActorRef = _
  context.actorSelection("/user/interpolation") ! Identify(None)
  var transformationRef: ActorRef = _
  context.actorSelection("/user/transformation") ! Identify(None)
  var validationRef: ActorRef = _
  context.actorSelection("/user/validation") ! Identify(None)

  // TODO: Replace with Redis
  var onChangeHashMap: scala.collection.immutable.Map[String, List[String]] = scala.collection.immutable.Map()

  def loadOnChangeTriggers(): Unit = {
    val onChangeExtensionsResponse: Future[Seq[ExtensionObj]] = (extensionAdapterRef ? GetExtensions(triggerType = "OnChange")).mapTo[Seq[ExtensionObj]]
    onChangeExtensionsResponse  map { onChangeExtensions: Seq[ExtensionObj] =>
      onChangeExtensions map { onChangeExtension: ExtensionObj =>
        onChangeExtension.toExtension.trigger.data.map { i: String =>
          if(onChangeHashMap.isDefinedAt(i)) {
            val aa: List[String] = onChangeHashMap.get(i).get
            i ->  (aa :+ onChangeExtension.extensionId)
          } else {
            i -> List(onChangeExtension.extensionId)
          }
        }.toMap
      } foreach { bb: Map[String, List[String]] =>
        log.info("loadOnChangeTriggers: {}", bb)
        onChangeHashMap = bb
      }
    }
  }

  def receive: Receive = {
    case OnChangeTimeseries(timeSeries: TimeSeries) =>
      log.info("On Change Timeseries {}", timeSeries)
      if(onChangeHashMap.isDefinedAt(timeSeries.timeSeriesId)){
        log.info("Trigger for OnChange > {}", onChangeHashMap.get(timeSeries.timeSeriesId))
      } else {
        log.info("No Triggers for OnChange. {}", onChangeHashMap)
      }

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ExtensionHandler): {}", ref.path.name)
      ref.path.name match {
        case "extensionAdapter" => {
          extensionAdapterRef = ref
          loadOnChangeTriggers()
        }
        case "interpolation" => interpolationRef = ref
        case "transformation" => transformationRef = ref
        case "validation" => validationRef = ref
        case default => log.warning("Unknown Actor Identity in ExtensionHandler: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}