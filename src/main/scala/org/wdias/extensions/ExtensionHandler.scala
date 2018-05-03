package org.wdias.extensions

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.extension_adapter.ExtensionAdapter.{GetExtensionById, GetExtensions, GetTransformationById}
import org.wdias.constant.TimeSeries
import akka.pattern.ask
import org.wdias.extensions.transformation.Transformation.TriggerTransformation

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExtensionHandler {
  case class OnChangeTimeseries(timeSeries: TimeSeries)

  // TODO: Find a method to get data for a particular extension without couple with ExtensionAdapter
  // Possible solution may be, get out Extension methods s.t. GetTransformationById from ExtensionAdapter and create
  // adapter for each extension.
  case class GetExtensionDataById(extension: String, extensionId: String)
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

  def getExtensionRef(extension: String): ActorRef = {
    extension match {
      case "Transformation" =>
        transformationRef
      case "Interpolation" =>
        interpolationRef
      case "Validation" =>
        validationRef
      case _ =>
        null
    }
  }

  def loadOnChangeTriggers(): Unit = {
    val onChangeExtensionsResponse: Future[Seq[ExtensionObj]] = (extensionAdapterRef ? GetExtensions(triggerType = "OnChange")).mapTo[Seq[ExtensionObj]]
    onChangeExtensionsResponse  map { onChangeExtensions: Seq[ExtensionObj] =>
      onChangeExtensions map { onChangeExtension: ExtensionObj =>
        onChangeExtension.toExtension.trigger.data.map { i: String =>
          if(onChangeHashMap.isDefinedAt(i)) {
            val aa: List[String] = onChangeHashMap(i)
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
        val onChangeExtensions: List[String] = onChangeHashMap(timeSeries.timeSeriesId)
        onChangeExtensions foreach { extensionId:String =>
          (extensionAdapterRef ? GetExtensionById(extensionId)).mapTo[Option[ExtensionObj]] map {extensionObj: Option[ExtensionObj] =>
            log.info("Extension Obj: {}", extensionObj)
            if(extensionObj.isDefined) {
              log.info("Send to actor {} >> {}", extensionObj.get.extension, getExtensionRef(extensionObj.get.extension))
              getExtensionRef(extensionObj.get.extension) ! TriggerExtension(extensionObj.get)
            } else {
              log.warning("Unable to find extension onChangeTimeseries: {}", extensionId)
            }
          }
        }
      } else {
        log.info("No Triggers for OnChange. {}", onChangeHashMap)
      }
    case GetExtensionDataById(extension: String, extensionId: String) =>
      extension match {
        case "Transformation" =>
          extensionAdapterRef forward GetTransformationById(extensionId)
        case _ =>
          null
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