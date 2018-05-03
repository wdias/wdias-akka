package org.wdias.adapters.extension_adapter
import akka.pattern.pipe
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import org.wdias.adapters.extension_adapter.ExtensionAdapter._
import org.wdias.adapters.extension_adapter.models.{ExtensionsDAO, TransformationsDAO}
import org.wdias.adapters.metadata_adapter.MetadataAdapter.{CreateTimeseries, CreateTimeseriesWithIds, GetTimeseriesById}
import org.wdias.constant._
import org.wdias.extensions.transformation.TransformationExtensionObj
import org.wdias.extensions.{Extension, ExtensionObj, Trigger}

import scala.collection.mutable.ArrayOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

object ExtensionAdapter {
  // Extension
  case class GetExtensionById(extensionId: String)

  case class GetExtensions(extensionId: String = "", extension: String = "", function: String = "", triggerType: String = "")

  case class CreateExtension(extensionObj: ExtensionObj)

  case class ReplaceExtension(extensionId: String, extensionObj: ExtensionObj)

  case class DeleteExtensionById(extensionId: String)

  // Transformation
  case class GetTransformationById(extensionId: String)

  case class GetTransformations(extensionId: String = "")

  case class CreateTransformation(extensionObj: TransformationExtensionObj)

  case class ReplaceTransformation(extensionId: String, transformation: TransformationExtensionObj)

  case class DeleteTransformationById(extensionId: String)

}

class ExtensionAdapter extends Actor with ActorLogging {

  implicit val timeout: Timeout = Timeout(25 seconds)

  var metadataAdapterRef: ActorRef = _
  context.actorSelection("/user/metadataAdapter") ! Identify(None)

  def receive: Receive = {
    // Handle Extension MSGs
    case GetExtensionById(extensionId: String) =>
      log.info("GET Extension By Id: {}", extensionId)
      pipe(ExtensionsDAO.findById(extensionId).mapTo[Option[ExtensionObj]] map { result: Option[ExtensionObj] =>
        result
      }) to sender()
    case GetExtensions(extensionId: String, extension: String, function: String, triggerType: String) =>
      log.info("GET Query Extensions: {} {}", extensionId)
      pipe(ExtensionsDAO.find(extensionId, extension, function, triggerType).mapTo[Seq[ExtensionObj]] map { result: Seq[ExtensionObj] =>
        result
      }) to sender()
    case CreateExtension(extension: ExtensionObj) =>
      log.info("POST Extension: {}", extension)
      val isCreated = ExtensionsDAO.create(extension)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        result
      }) to sender()
    case DeleteExtensionById(extensionId: String) =>
      log.info("DELETE Extension By Id: {}", extensionId)
      val isDeleted = ExtensionsDAO.deleteById(extensionId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    // Handle Transformation MSGs
    case GetTransformationById(extensionId: String) =>
      log.info("GET TransformationExtension By Id: {}", extensionId)
      pipe(TransformationsDAO.findById(extensionId).mapTo[Option[TransformationExtensionObj]] map { result: Option[TransformationExtensionObj] =>
        result
      }) to sender()
    case GetTransformations(extensionId: String) =>
      log.info("GET Query TransformationExtensions: {} {}", extensionId)
      pipe(TransformationsDAO.find(extensionId).mapTo[Seq[TransformationExtensionObj]] map { result: Seq[TransformationExtensionObj] =>
        result
      }) to sender()
    case CreateTransformation(transformationExtensionObj: TransformationExtensionObj) =>
      log.info("POST TransformationExtension: {}", transformationExtensionObj)
      val ss = sender()
      /** In order to avoid additional calls on metadata while running the Extensions, it will enhance the performance of the system,
        * if it already store the all metadata of timeseries while creating the extensions */
      val dummyExtension = Extension("", "", "", Trigger("", Array()))
      val a: List[Future[Int]] = transformationExtensionObj.toTransformationExtension(dummyExtension).variables.toList.map(variable => {
        if(variable.timeSeriesId.isDefined) {
          // TODO: Store Timeseries with complete metadata inside variable
          val createVariables: Future[Option[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseriesById(variable.timeSeriesId.get)).mapTo[Option[MetadataIdsObj]]
          createVariables map { isCreated: Option[MetadataIdsObj] =>
            if(isCreated.isDefined) {
              1
            } else {
              -1
            }
          }
        } else if(variable.timeSeries.isDefined) {
          (metadataAdapterRef ? CreateTimeseries(variable.timeSeries.get.toMetadataObj)).mapTo[Int]
        } else if(variable.timeSeriesWithIds.isDefined){
          // TODO: Store Timeseries with complete metadata inside variable
          (metadataAdapterRef ? CreateTimeseriesWithIds(variable.timeSeriesWithIds.get.toMetadataIdsObj)).mapTo[Int]
        } else {
          Future(0)
        }
      })
      val aa: Future[List[Int]] = Future.sequence(a)
      aa map ((bb: List[Int]) => {
        if(bb.exists(_ < 0)) {
          ss ! 0
        } else {
          val isCreated = TransformationsDAO.create(transformationExtensionObj)
          isCreated.mapTo[Int] map { result: Int =>
            ss ! result
          }
        }
      })
    case DeleteTransformationById(transformationExtensionId: String) =>
      log.info("DELETE TransformationExtension By Id: {}", transformationExtensionId)
      val isDeleted = TransformationsDAO.deleteById(transformationExtensionId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ExtensionAdapter): {}", ref.path.name)
      ref.path.name match {
        case "metadataAdapter" => metadataAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ExtensionAdapter: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}
