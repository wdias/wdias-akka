package org.wdias.adapters.extension_adapter
import akka.pattern.pipe
import akka.util.Timeout
import akka.actor.{Actor, ActorLogging}
import org.wdias.adapters.extension_adapter.ExtensionAdapter._
import org.wdias.adapters.extension_adapter.models.{ExtensionsDAO, TransformationsDAO}
import org.wdias.constant._
import org.wdias.extensions.transformation.TransformationExtensionObj
import org.wdias.extensions.ExtensionObj
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExtensionAdapter {
  // Extension
  case class GetExtensionById(extensionId: String)

  case class GetExtensions(extensionId: String = "", extension: String = "", function: String = "")

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

  implicit val timeout: Timeout = Timeout(15 seconds)

  def receive: Receive = {
    // Handle Extension MSGs
    case GetExtensionById(extensionId: String) =>
      log.info("GET Extension By Id: {}", extensionId)
      pipe(ExtensionsDAO.findById(extensionId).mapTo[Option[ExtensionObj]] map { result: Option[ExtensionObj] =>
        result
      }) to sender()
    case GetExtensions(extensionId: String, extension: String, function: String) =>
      log.info("GET Query Extensions: {} {}", extensionId)
      pipe(ExtensionsDAO.find(extensionId, extension, function).mapTo[Seq[ExtensionObj]] map { result: Seq[ExtensionObj] =>
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
      val isCreated = TransformationsDAO.create(transformationExtensionObj)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        result
      }) to sender()
    case DeleteTransformationById(transformationExtensionId: String) =>
      log.info("DELETE TransformationExtension By Id: {}", transformationExtensionId)
      val isDeleted = TransformationsDAO.deleteById(transformationExtensionId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()
  }
}
