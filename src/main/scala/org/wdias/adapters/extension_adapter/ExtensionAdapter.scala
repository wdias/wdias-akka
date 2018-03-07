package org.wdias.adapters.extension_adapter
import akka.pattern.pipe
import akka.util.Timeout
import akka.actor.{Actor, ActorLogging}
import org.wdias.adapters.extension_adapter.ExtensionAdapter._
import org.wdias.adapters.extension_adapter.models.{ExtensionsDAO, TransformationsDAO}
import org.wdias.adapters.metadata_adapter.models.{LocationsDAO, ParametersDAO, TimeSeriesMetadataDAO, TimeStepsDAO}
import org.wdias.constant._
import org.wdias.extensions.transformation.TransformationExtensionObj
import org.wdias.extensions.ExtensionObj
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExtensionAdapter {

  case class GetValidationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetTransformationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class GetInterpolationConfig(timeSeriesEnvelop: TimeSeriesEnvelop)

  case class ValidationConfig(name: String, minValue: Double, maxValue: Double)

  case class ValidationConfigResult(option: Option[ValidationConfig], timeSeriesEnvelop: TimeSeriesEnvelop)

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

  var validationConfigs = Set(
    ValidationConfig("Hanwella", 0, 120),
    ValidationConfig("Colombo", 0, 120),
    ValidationConfig("Waga", 0, 120),
  )

  def receive: Receive = {
    // Handle Extension MSGs
    case GetExtensionById(extensionId) =>
      log.info("GET Extension By Id: {}", extensionId)
      pipe(ExtensionsDAO.findById(extensionId).mapTo[Option[ExtensionObj]] map { result: Option[ExtensionObj] =>
        result
      }) to sender()
    case GetExtensions(extensionId, extension, function) =>
      log.info("GET Query Extensions: {} {}", extensionId)
      pipe(ExtensionsDAO.find(extensionId, extension, function).mapTo[Seq[ExtensionObj]] map { result: Seq[ExtensionObj] =>
        result
      }) to sender()
    case CreateExtension(extension) =>
      log.info("POST Extension: {}", extension)
      val isCreated = ExtensionsDAO.create(extension)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        if (result > 0) extension else 0
      }) to sender()
    case DeleteExtensionById(extensionId) =>
      log.info("DELETE Extension By Id: {}", extensionId)
      val isDeleted = ExtensionsDAO.deleteById(extensionId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    // Handle Transformation MSGs
    case GetTransformationById(transformationExtensionId) =>
      log.info("GET TransformationExtension By Id: {}", transformationExtensionId)
      pipe(TransformationsDAO.findById(transformationExtensionId).mapTo[Option[TransformationExtensionObj]] map { result: Option[TransformationExtensionObj] =>
        result
      }) to sender()
    case GetTransformations(extensionId) =>
      log.info("GET Query TransformationExtensions: {} {}", extensionId)
      pipe(TransformationsDAO.find(extensionId).mapTo[Seq[TransformationExtensionObj]] map { result: Seq[TransformationExtensionObj] =>
        result
      }) to sender()
    case CreateTransformation(transformationExtensionObj: TransformationExtensionObj) =>
      log.info("POST TransformationExtension: {}", transformationExtensionObj)
      val isCreated = TransformationsDAO.create(transformationExtensionObj)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        if (result > 0) transformationExtensionObj else 0
      }) to sender()
    case DeleteTransformationById(transformationExtensionId) =>
      log.info("DELETE TransformationExtension By Id: {}", transformationExtensionId)
      val isDeleted = TransformationsDAO.deleteById(transformationExtensionId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()


    case GetValidationConfig(timeSeriesEnvelop) =>
      log.info("GetValidationConfig:: {}, {}", timeSeriesEnvelop, validationConfigs.find(_.name == timeSeriesEnvelop.metaData.location.name))

      sender() ! ValidationConfigResult(validationConfigs.find(_.name == timeSeriesEnvelop.metaData.location.name), timeSeriesEnvelop)
      log.info("<<<<")
    case GetTransformationConfig(timeSeriesEnvelop) =>
      log.info("GetTransformationConfig: {}", timeSeriesEnvelop)
    case GetInterpolationConfig(timeSeriesEnvelop) =>
      log.info("GetInterpolationConfig: {}", timeSeriesEnvelop)
  }
}
